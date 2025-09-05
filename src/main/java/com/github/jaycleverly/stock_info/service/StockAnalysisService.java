package com.github.jaycleverly.stock_info.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import com.github.jaycleverly.stock_info.client.DynamoClient;
import com.github.jaycleverly.stock_info.dto.DailyStockMetrics;
import com.github.jaycleverly.stock_info.dto.DailyStockRecord;
import com.github.jaycleverly.stock_info.dto.DateRange;
import com.github.jaycleverly.stock_info.exception.DynamoClientException;
import com.github.jaycleverly.stock_info.exception.ExternalApiProcessingException;
import com.github.jaycleverly.stock_info.exception.MetricBuilderException;
import com.github.jaycleverly.stock_info.exception.MetricFormatterException;
import com.github.jaycleverly.stock_info.exception.StockAnalysisException;
import com.github.jaycleverly.stock_info.parser.StockApiResponseParser;
import com.github.jaycleverly.stock_info.util.DateUtils;

/**
 * Class to provide a response containing metrics about a stock
 */
@Service
public class StockAnalysisService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StockAnalysisService.class);
    private static final String DYNAMO_TABLE_NAME = "StockMetrics";

    private static DynamoClient dynamoClient = new DynamoClient(DynamoDbEnhancedClient.create());

    private static int numDaysToAnalyse;
    @Value("${max.records}")
    public void setNumDaysToAnalyse(int numDaysToAnalyse) {StockAnalysisService.numDaysToAnalyse = numDaysToAnalyse;}

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
    public static String produceAnalysis(String symbol, 
                                                          String startDate, 
                                                          String endDate) throws IllegalArgumentException, StockAnalysisException {
        DateRange analysisDateRange = DateUtils.verifyDateRange(DateUtils.convertToDate(startDate), 
                                                                DateUtils.convertToDate(endDate),
                                                                DateUtils.getPastDate(numDaysToAnalyse + 1), 
                                                                DateUtils.getPastDate(1));
        List<DailyStockMetrics> stockAnalysis = new ArrayList<>();

        try {
            List<DailyStockMetrics> dynamoResponse = findDynamoRecordsBetween(
                analysisDateRange.getStartDate().toString(), 
                analysisDateRange.getEndDate().toString(),
                symbol,
                DailyStockMetrics.class);
                
            if (dynamoResponse.isEmpty()) {
                List<DailyStockRecord> stockRecords = fetchAndConvertStockRecords(symbol, numDaysToAnalyse);
                List<DailyStockMetrics> stockMetrics = calculateAndUploadStockMetrics(stockRecords.getFirst().getDate(), numDaysToAnalyse, stockRecords);
                
                stockAnalysis.addAll(stockMetrics);
            } else {
    
                stockAnalysis = dynamoResponse;
                LocalDate lastUpdate = dynamoResponse.getLast().getDate();
    
                // Dynamo records are not up to date
                if (!lastUpdate.equals(DateUtils.getPastDate(1))) {
                    int daysSinceUpdate = DateUtils.calculateNumDaysBetweenDates(lastUpdate, DateUtils.getPastDate(1));
    
                    List<DailyStockRecord> stockRecords = fetchAndConvertStockRecords(symbol, daysSinceUpdate);
                    List<DailyStockMetrics> stockMetrics = calculateAndUploadStockMetrics(lastUpdate.plusDays(1), daysSinceUpdate, stockRecords);
    
                    stockAnalysis.addAll(stockMetrics);
                }
            }
    
            // Filter the returned list by the dates entered
            return filterAndFormatMetrics(stockAnalysis, analysisDateRange.getStartDate(), analysisDateRange.getEndDate());

        } catch (DynamoClientException | ExternalApiProcessingException | MetricBuilderException | MetricFormatterException exception) {
            throw new StockAnalysisException(
                String.format("Exception when producing %s analysis over the period %s - %s!", 
                    symbol, analysisDateRange.getStartDate(), analysisDateRange.getEndDate()), 
                exception);
        }
    }

    private static <T> List<T> findDynamoRecordsBetween(String sortKeyStart, 
                                                        String sortKeyEnd, 
                                                        String partitionKey, 
                                                        Class<T> recordType) throws DynamoClientException {
        try {
            return dynamoClient.query(
                DYNAMO_TABLE_NAME, 
                QueryConditional
                    .sortBetween(
                        Key.builder().partitionValue(partitionKey).sortValue(sortKeyStart).build(), 
                        Key.builder().partitionValue(partitionKey).sortValue(sortKeyEnd).build()),
                numDaysToAnalyse,
                recordType);
        } catch (DynamoClientException exception) {
            LOGGER.error(String.format("Exception when finding dynamo records between (%s) - (%s)", sortKeyStart, sortKeyEnd));
            throw exception;
        }
    }

    private static List<DailyStockRecord> fetchAndConvertStockRecords(String symbol, 
                                                                      int numDaysToFetch) throws ExternalApiProcessingException {
        try {
            return StockApiResponseParser.parse(FakeApiService.getStockData(symbol, numDaysToFetch));
        } catch (ExternalApiProcessingException exception) {
            LOGGER.error(String.format("Exception when parsing API response for stock (%s)", symbol));
            throw exception;
        }
    }

    private static List<DailyStockMetrics> calculateAndUploadStockMetrics(LocalDate startDate, 
                                                                          int daysToStore, 
                                                                          List<DailyStockRecord> recordsToAnalyse) throws MetricBuilderException, DynamoClientException {
        ArrayList<DailyStockMetrics> metricsToUpload = new ArrayList<>();
        
        for (int i = 0; i < daysToStore; i++) {
            try {
                metricsToUpload.add(MetricBuilderService.caclculateMetrics(startDate.plusDays(i), recordsToAnalyse));
                dynamoClient.putItem(DYNAMO_TABLE_NAME, metricsToUpload.getLast(), DailyStockMetrics.class);
            } catch (MetricBuilderException exception) {
                LOGGER.error(String.format("Exception when generating metric record for stock on date (%s)", startDate.plusDays(i)));
                throw exception;
            } catch (DynamoClientException exception) {
                LOGGER.error(String.format("Exception when uplading metric record with date (%s) to dynamo", startDate.plusDays(i)));
                throw exception;
            }
        }
        return metricsToUpload;
    }

    private static String filterAndFormatMetrics(List<DailyStockMetrics> metrics, 
                                                 LocalDate metricStartDate, 
                                                LocalDate metricEndDate) throws MetricFormatterException {
        // More recent metrics at start of list
        List<DailyStockMetrics> metricsToFormat = metrics.stream()
            .filter(r -> !r.getDate().isBefore(metricStartDate) && !r.getDate().isAfter(metricEndDate))
            .collect(Collectors.toList())
            .reversed();

        try {
            return MetricFormatterService.convertMetricsToJson(metricsToFormat);
        } catch (MetricFormatterException exception) {
            LOGGER.error("Exception when converting metrics to JSON response", exception);
            throw exception;
        }
    }
}
