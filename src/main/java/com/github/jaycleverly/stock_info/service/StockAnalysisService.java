package com.github.jaycleverly.stock_info.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import com.github.jaycleverly.stock_info.client.DynamoClient;
import com.github.jaycleverly.stock_info.dto.DailyStockMetrics;
import com.github.jaycleverly.stock_info.dto.DailyStockRecord;
import com.github.jaycleverly.stock_info.dto.DateRange;
import com.github.jaycleverly.stock_info.parser.StockApiResponseParser;
import com.github.jaycleverly.stock_info.util.DateUtils;

/**
 * Class to provide a response containing metrics about a stock
 */
public class StockAnalysisService {
    private static final String DYNAMO_TABLE_NAME = "StockMetrics";
    private static final int NUM_DAYS_TO_ANALYSE = 50;

    private static DynamoClient dynamoClient = new DynamoClient(DynamoDbEnhancedClient.create());
    
    /**
     * Produces a response containing metrics for a particular stock symbol
     * 
     * @param symbol the symbol of the stock to provide analysis on
     * @return a list of metrics
     * @throws RuntimeException if an exception occurs during processing
     */
    public static List<DailyStockMetrics> produceAnalysis(String symbol) throws RuntimeException {
        // Default is to keep data period within last 50 days
        return produceAnalysis(symbol, LocalDate.now().minusDays(NUM_DAYS_TO_ANALYSE + 1), LocalDate.now().minusDays(1));
    }

    /**
     * Produces a response containing metrics for a particular stock symbol and date range
     * 
     * @param symbol the symbol of the stock to provide analysis on
     * @param startDate the date to provide results from
     * @param endDate the date to provide results to
     * @return a list of metrics 
     * @throws RuntimeException if an exception occurs during processing
     */
    public static List<DailyStockMetrics> produceAnalysis(String symbol, LocalDate startDate, LocalDate endDate) throws RuntimeException {
        DateRange analysisDateRange = DateUtils.verifyDateRange(startDate, endDate, DateUtils.getPastDate(NUM_DAYS_TO_ANALYSE + 1), DateUtils.getPastDate(1));
        List<DailyStockMetrics> stockAnalysis = new ArrayList<>();
        
        try {
            List<DailyStockMetrics> dynamoResponse = findDynamoRecordsBetween(
                analysisDateRange.getStartDate().toString(), 
                analysisDateRange.getEndDate().toString(),
                symbol,
                DailyStockMetrics.class);
            
            if (dynamoResponse.isEmpty()) {
                List<DailyStockRecord> stockRecords = fetchAndConvertStockRecords(symbol, NUM_DAYS_TO_ANALYSE);
                List<DailyStockMetrics> stockMetrics = calculateAndUploadStockMetrics(stockRecords.getFirst().getDate(), NUM_DAYS_TO_ANALYSE, stockRecords);
            
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
            return stockAnalysis.stream()
                .filter(r -> !r.getDate().isBefore(analysisDateRange.getStartDate()) && !r.getDate().isAfter(analysisDateRange.getEndDate()))
                .collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException(
                String.format("Exception when retrieving %s analysis over the period %s - %s!", symbol, analysisDateRange.getStartDate(), analysisDateRange.getEndDate()), 
                e);
        }
    }

    private static <T> List<T> findDynamoRecordsBetween(String sortKeyStart, String sortKeyEnd, String partitionKey, Class<T> recordType) {
        return dynamoClient.query(
            DYNAMO_TABLE_NAME, 
            QueryConditional
                .sortBetween(
                    Key.builder().partitionValue(partitionKey).sortValue(sortKeyStart).build(), 
                    Key.builder().partitionValue(partitionKey).sortValue(sortKeyEnd).build()),
            NUM_DAYS_TO_ANALYSE,
            recordType);
    }

    private static List<DailyStockRecord> fetchAndConvertStockRecords(String symbol, int numDaysToFetch) throws Exception {
        return StockApiResponseParser.parse(FakeApiService.getStockData(symbol, numDaysToFetch));
    }

    private static List<DailyStockMetrics> calculateAndUploadStockMetrics(LocalDate startDate, int daysToStore, List<DailyStockRecord> recordsToAnalyse) {
        ArrayList<DailyStockMetrics> metricsToUpload = new ArrayList<>();
        
        for (int i = 0; i < daysToStore; i++) {
            metricsToUpload.add(MetricBuilderService.caclculateMetrics(startDate.plusDays(i), recordsToAnalyse));
            dynamoClient.putItem(DYNAMO_TABLE_NAME, metricsToUpload.getLast(), DailyStockMetrics.class);
        }
        return metricsToUpload;
    }
}
