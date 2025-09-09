package com.github.jaycleverly.stock_info.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import com.github.jaycleverly.stock_info.client.DynamoClient;
import com.github.jaycleverly.stock_info.config.properties.AppLimitsProperties;
import com.github.jaycleverly.stock_info.exception.DynamoClientException;
import com.github.jaycleverly.stock_info.exception.FakeApiException;
import com.github.jaycleverly.stock_info.exception.ParserException;
import com.github.jaycleverly.stock_info.exception.MetricBuilderException;
import com.github.jaycleverly.stock_info.exception.SerializerException;
import com.github.jaycleverly.stock_info.exception.StockAnalysisException;
import com.github.jaycleverly.stock_info.model.DailyStockMetrics;
import com.github.jaycleverly.stock_info.model.DailyStockRecord;
import com.github.jaycleverly.stock_info.model.DateRange;
import com.github.jaycleverly.stock_info.parser.StockRecordsParser;
import com.github.jaycleverly.stock_info.serializer.StockMetricsSerializer;
import com.github.jaycleverly.stock_info.util.DateUtils;

/**
 * Class to provide a response containing metrics about a stock
 */
@Service
public class StockAnalysisService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StockAnalysisService.class);
    private static final String DYNAMO_TABLE_NAME = "StockMetrics";

    private final int defaultDaysToAnalyse;
    private final int maxDaysToAnalyse;
    private final DynamoClient dynamoClient;
    private final FakeApiService fakeApiService;
    private final MetricBuilderService metricBuilderService;

    /**
     * Creates a new service that can provide an analysis response on a stock
     * 
     * @param limitsProperties the properties set for the application
     * @param dynamoClient the client to handle dynamo db interactions
     * @param fakeApiService the service to handle fake data generation
     * @param metricBuilderService the service to create metrics from stock records
     */
    public StockAnalysisService(AppLimitsProperties limitsProperties,
                                DynamoClient dynamoClient, 
                                FakeApiService fakeApiService, 
                                MetricBuilderService metricBuilderService) {
        this.defaultDaysToAnalyse = limitsProperties.metricRecords();
        this.maxDaysToAnalyse = limitsProperties.apiRecords();
        this.dynamoClient = dynamoClient;
        this.fakeApiService = fakeApiService;
        this.metricBuilderService = metricBuilderService;
    }

    /**
     * Produces a response containing metrics for a particular stock symbol and date range
     * 
     * @param symbol the symbol of the stock to provide analysis on
     * @param startDate the date to provide results from
     * @param endDate the date to provide results to
     * @return a list of metrics 
     * @throws IllegalArgumentException if there is invalid input
     * @throws StockAnalysisException if an error occurs while processing
     */
    public String produceAnalysis(String symbol, 
                                  String startDate, 
                                  String endDate,
                                  boolean fullOutputSize) throws IllegalArgumentException, StockAnalysisException {
        final int numRecordsToReturn = fullOutputSize ? maxDaysToAnalyse : defaultDaysToAnalyse;
        final DateRange analysisDateRange = DateUtils.verifyDateRange(DateUtils.convertToDate(startDate), 
                                                                      DateUtils.convertToDate(endDate),
                                                                      DateUtils.getPastDate(numRecordsToReturn), 
                                                                      DateUtils.getPastDate(1));
        
        List<DailyStockMetrics> stockAnalysis = new ArrayList<>();
        try {
            // Disregard analysis dates when retrieving dynamo records
            List<DailyStockMetrics> dynamoResponse = findDynamoRecordsBetween(
                DateUtils.getPastDate(numRecordsToReturn).toString(), 
                DateUtils.getPastDate(1).toString(),
                symbol,
                numRecordsToReturn,
                DailyStockMetrics.class);

            LocalDate lastUpdateDate = dynamoResponse.isEmpty() 
                ? null
                : dynamoResponse.getLast().getDate();

            if (lastUpdateDate == null || lastUpdateDate.isBefore(DateUtils.getPastDate(1))) {
                LOGGER.info(String.format("Dynamo records incomplete for stock %s, updating...", symbol));

                // Set last update to maximum records if none present in dynamo
                lastUpdateDate = lastUpdateDate == null 
                    ? DateUtils.getPastDate(maxDaysToAnalyse)
                    : lastUpdateDate;
                int numRecordsToAdd = DateUtils.calculateNumDaysBetweenDates(lastUpdateDate, DateUtils.getPastDate(1)) + 1;

                List<DailyStockRecord> stockRecords = fetchAndConvertStockRecords(symbol, numRecordsToAdd);
                List<DailyStockMetrics> stockMetrics = calculateAndUploadStockMetrics(stockRecords);

                stockAnalysis.addAll(stockMetrics);

            } else {
                stockAnalysis = dynamoResponse;
            }

            return filterAndSerializeMetrics(stockAnalysis, analysisDateRange.getStartDate(), analysisDateRange.getEndDate(), numRecordsToReturn);

        } catch (DynamoClientException | FakeApiException | ParserException | MetricBuilderException | SerializerException exception) {
            throw new StockAnalysisException(
                String.format("Exception when producing %s analysis over the period %s - %s!", 
                    symbol, analysisDateRange.getStartDate(), analysisDateRange.getEndDate()), 
                exception);
        }
    }

    private <T> List<T> findDynamoRecordsBetween(String sortKeyStart, 
                                                 String sortKeyEnd, 
                                                 String partitionKey, 
                                                 int maxRecords,
                                                 Class<T> recordType) throws DynamoClientException {
        try {
            return dynamoClient.query(
                DYNAMO_TABLE_NAME, 
                QueryConditional
                    .sortBetween(
                        Key.builder().partitionValue(partitionKey).sortValue(sortKeyStart).build(), 
                        Key.builder().partitionValue(partitionKey).sortValue(sortKeyEnd).build()
                    ),
                maxRecords,
                recordType);
        } catch (DynamoClientException exception) {
            LOGGER.error(String.format("Exception when finding dynamo records between (%s) - (%s)", sortKeyStart, sortKeyEnd));
            throw exception;
        }
    }

    private List<DailyStockRecord> fetchAndConvertStockRecords(String symbol, 
                                                               int numDaysToFetch) throws ParserException {
        try {
            return StockRecordsParser.parse(fakeApiService.getStockData(symbol, numDaysToFetch));
        } catch (ParserException exception) {
            LOGGER.error(String.format("Exception when parsing API response for stock (%s)", symbol));
            throw exception;
        }
    }

    private List<DailyStockMetrics> calculateAndUploadStockMetrics(List<DailyStockRecord> recordsToAnalyse) throws MetricBuilderException, DynamoClientException {
        ArrayList<DailyStockMetrics> metricsToUpload = new ArrayList<>();
        
        for (DailyStockRecord record: recordsToAnalyse) {
            try {
                metricsToUpload.add(metricBuilderService.caclculateMetrics(record.getDate(), recordsToAnalyse));
                dynamoClient.putItem(DYNAMO_TABLE_NAME, metricsToUpload.getLast(), DailyStockMetrics.class);
            } catch (MetricBuilderException exception) {
                LOGGER.error(String.format("Exception when generating metric record for stock on date (%s)", record.getDate()));
                throw exception;
            } catch (DynamoClientException exception) {
                LOGGER.error(String.format("Exception when uplading metric record with date (%s) to dynamo", record.getDate()));
                throw exception;
            }
        }
        return metricsToUpload;
    }

    private String filterAndSerializeMetrics(List<DailyStockMetrics> metrics, 
                                             LocalDate metricStartDate, 
                                             LocalDate metricEndDate,
                                             int numRecordsToReturn) throws SerializerException {
        // More recent metrics at start of list
        List<DailyStockMetrics> metricsToFormat = metrics.stream()
            .filter(r -> !r.getDate().isBefore(metricStartDate) && !r.getDate().isAfter(metricEndDate))
            .sorted(Comparator.comparing(DailyStockMetrics::getDate).reversed())
            .limit(numRecordsToReturn)
            .toList();

        try {
            return StockMetricsSerializer.serialize(metricsToFormat);
        } catch (SerializerException exception) {
            LOGGER.error("Exception when converting metrics to JSON response", exception);
            throw exception;
        }
    }
}
