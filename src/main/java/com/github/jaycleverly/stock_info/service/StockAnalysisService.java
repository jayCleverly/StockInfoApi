package com.github.jaycleverly.stock_info.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.github.jaycleverly.stock_info.dto.DailyStockAnalysis;
import com.github.jaycleverly.stock_info.dto.DailyStockRecord;
import com.github.jaycleverly.stock_info.dto.StockHistory;

/**
 * Class to calculate different metrics about a particular stock record
 */
public class StockAnalysisService {
    private static final int MOVING_AVERAGE_PERIOD = 30;
    private static final int VOLATILITY_PERIOD = 7;
    private static final int MOMENTUM_PERIOD = 14;
    
    /**
     * Calculates metrics for a specific date in a stock's records
     * @param date the date of a record to calculate metrics for
     * @param history all the records associated with the stock 
     * @return an object containing all of the metrics
     */
    public static DailyStockAnalysis caclculateMetrics(LocalDate date, StockHistory history) {
        List<DailyStockRecord> records = history.getRecords();
        DailyStockRecord recordToAnalyse = records.stream()
            .filter(r -> r.getDate().equals(date))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(String.format("Date (%s) not found in records", date.toString())));
        
        double close = round2dp(recordToAnalyse.getClose());
        Double previousCloseChange = round2dp(calculateChangeFromPreviousClose(records, recordToAnalyse));
        Double movingAverage = round2dp(calculateMovingAverage(records, recordToAnalyse));
        Double volatility = round2dp(calculateVolatility(records, recordToAnalyse));
        Double momentum = round2dp(calculateMomentum(records, recordToAnalyse));

        return new DailyStockAnalysis(date, close, previousCloseChange, movingAverage, volatility, momentum);
    }

    private static Double calculateChangeFromPreviousClose(List<DailyStockRecord> records, DailyStockRecord recordToAnalyse) {
        int index = records.indexOf(recordToAnalyse);

        if (index >= 1) {
            return records.get(index).getClose() - records.get(index - 1).getClose();
        }
        return null;
    }

    private static Double calculateMovingAverage(List<DailyStockRecord> records, DailyStockRecord recordToAnalyse) {
        int index = records.indexOf(recordToAnalyse);

        if (index >= MOVING_AVERAGE_PERIOD - 1) {
            double sum = 0;
            for (int j = (index - MOVING_AVERAGE_PERIOD) + 1; j <= index; j++) {
                sum += records.get(j).getClose();
            }
            return sum / MOVING_AVERAGE_PERIOD;
        }
        return null;
    }

    private static Double calculateVolatility(List<DailyStockRecord> records, DailyStockRecord recordToAnalyse) {
        int index = records.indexOf(recordToAnalyse);

        if (index >= VOLATILITY_PERIOD - 1) {
            List<Double> returns = new ArrayList<>();
            for (int j = (index - VOLATILITY_PERIOD) + 1; j <= index; j++) {
                double prevClose = records.get(j - 1).getClose();
                returns.add((records.get(j).getClose() - prevClose) / prevClose);
            }
            double mean = returns.stream().mapToDouble(d -> d).average().orElse(0);
            double variance = returns.stream().mapToDouble(r -> Math.pow(r - mean, 2)).sum() / returns.size();
            return Math.sqrt(variance);
        }
        return null;
    }

    private static Double calculateMomentum(List<DailyStockRecord> records, DailyStockRecord recordToAnalyse) {
        int index = records.indexOf(recordToAnalyse);

        if (index >= MOMENTUM_PERIOD - 1) {
            double historicalClose = records.get(index - MOMENTUM_PERIOD).getClose();
            return (recordToAnalyse.getClose() - historicalClose) / historicalClose;
        }
        return null;
    }

    private static Double round2dp(Double value) {
        return value == null 
            ? null 
            : Math.round(value * 100.0) / 100.0;
    }
}
