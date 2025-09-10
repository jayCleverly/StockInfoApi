package com.github.jaycleverly.stock_info.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import com.github.jaycleverly.stock_info.client.DynamoClient;
import com.github.jaycleverly.stock_info.client.TimeSeriesApiClient;
import com.github.jaycleverly.stock_info.config.properties.AppLimitsProperties;
import com.github.jaycleverly.stock_info.config.properties.DynamoDbProperties;
import com.github.jaycleverly.stock_info.exception.ClientErrorException;
import com.github.jaycleverly.stock_info.exception.DynamoClientException;
import com.github.jaycleverly.stock_info.exception.InternalServerErrorException;
import com.github.jaycleverly.stock_info.exception.ParserException;
import com.github.jaycleverly.stock_info.exception.MetricBuilderException;
import com.github.jaycleverly.stock_info.exception.SerializerException;
import com.github.jaycleverly.stock_info.exception.TimeSeriesApiException;
import com.github.jaycleverly.stock_info.model.DailyStockMetrics;
import com.github.jaycleverly.stock_info.model.DailyStockRecord;
import com.github.jaycleverly.stock_info.parser.StockRecordsParser;
import com.github.jaycleverly.stock_info.serializer.StockMetricsSerializer;

/**
 * Class to provide a response containing metrics about a stock
 */
@Service
public class StockAnalysisService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StockAnalysisService.class);
    
    private final int defaultDaysToAnalyse;
    private final int maxDaysToAnalyse;
    private final String dynamoTableName;
    private final DynamoClient dynamoClient;
    private final TimeSeriesApiClient timeSeriesApiClient;
    private final MetricBuilderService metricBuilderService;

    /**
     * Creates a new service that can provide an analysis response on a stock
     * 
     * @param limitsProperties the properties set for the application
     * @param dynamoDbProperties the properties set for dynamodb
     * @param dynamoClient the client to handle dynamo db interactions
     * @param timeSeriesApiClient the client to handle external stock api interactions
     * @param metricBuilderService the service to create metrics from stock records
     */
    public StockAnalysisService(AppLimitsProperties limitsProperties,
                                DynamoDbProperties dynamoDbProperties,
                                DynamoClient dynamoClient, 
                                TimeSeriesApiClient timeSeriesApiClient, 
                                MetricBuilderService metricBuilderService) {
        this.defaultDaysToAnalyse = limitsProperties.compactRecords();
        this.maxDaysToAnalyse = limitsProperties.fullRecords();
        this.dynamoTableName = dynamoDbProperties.tableName();
        this.dynamoClient = dynamoClient;
        this.timeSeriesApiClient = timeSeriesApiClient;
        this.metricBuilderService = metricBuilderService;
    }

    /**
     * Produces a response containing metrics for a particular stock symbol
     * 
     * @param symbol the symbol of the stock to provide analysis on
     * @param fullOutputSize if true return a full output size, else compact
     * @return a list of metrics in a json format
     * @throws InternalServerErrorException if an error occurs while processing
     */
    public String produceAnalysis(String symbol, boolean fullOutputSize) throws InternalServerErrorException {        
        final int numRecordsToReturn = fullOutputSize ? maxDaysToAnalyse : defaultDaysToAnalyse;
       
        List<DailyStockMetrics> stockAnalysis = new ArrayList<>();
        try {
            List<DailyStockMetrics> dynamoRecords = findLastNDynamoRecords(
                symbol,
                numRecordsToReturn,
                DailyStockMetrics.class);

            // If no record of stock in dynamo or records need updating
            if (dynamoRecords.isEmpty() || dynamoRecords.getLast().getDate().isBefore(LocalDate.now().minusDays(1))) {
                LOGGER.info(String.format("Dynamo records incomplete for stock %s, updating...", symbol));

                List<DailyStockRecord> stockRecords = fetchAndConvertStockRecords(symbol);
                List<DailyStockMetrics> stockMetrics = calculateAndUploadStockMetrics(stockRecords);

                stockAnalysis.addAll(stockMetrics);

            } else {
                stockAnalysis = dynamoRecords;
            }

            return filterAndSerializeMetrics(stockAnalysis, numRecordsToReturn);

        } catch (DynamoClientException | TimeSeriesApiException | ParserException | MetricBuilderException | SerializerException exception) {
            throw new InternalServerErrorException(
                String.format("Exception when producing %s analysis!", symbol), 
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception);
        } 
    }

    private <T> List<T> findLastNDynamoRecords(String partitionKey, 
                                               int maxRecords,
                                               Class<T> recordType) throws DynamoClientException {
        try {
            return dynamoClient.query(
                dynamoTableName, 
                QueryConditional.keyEqualTo(Key.builder().partitionValue(partitionKey).build()),
                maxRecords,
                recordType);
        } catch (DynamoClientException exception) {
            LOGGER.error(String.format("Exception when finding the last %d dynamo records for key %s", maxRecords, partitionKey));
            throw exception;
        }
    }

    private List<DailyStockRecord> fetchAndConvertStockRecords(String symbol) throws TimeSeriesApiException, ClientErrorException, ParserException {
        try {
            return StockRecordsParser.parse(timeSeriesApiClient.getDailyTimeSeries(symbol));
        } catch (TimeSeriesApiException exception) {
            LOGGER.error(String.format("Exception when retrieving data from API for stock (%s)", symbol));

            // Client specific errors should be seperated from the internal exceptions
            switch (exception.getStatusCode()) {
                case 401:
                case 404:
                case 429:
                    throw new ClientErrorException(exception.getMessage(), HttpStatus.valueOf(exception.getStatusCode()), exception);
                default:
                    throw exception;
            }
            
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
                dynamoClient.putItem(dynamoTableName, metricsToUpload.getLast(), DailyStockMetrics.class);
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

    private String filterAndSerializeMetrics(List<DailyStockMetrics> metrics, int numRecordsToReturn) throws SerializerException {
        // More recent metrics at start of list
        List<DailyStockMetrics> metricsToFormat = metrics.reversed().stream()
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
