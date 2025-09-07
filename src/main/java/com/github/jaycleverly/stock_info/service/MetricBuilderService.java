package com.github.jaycleverly.stock_info.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.github.jaycleverly.stock_info.config.properties.AppCalculationsProperties;
import com.github.jaycleverly.stock_info.exception.MetricBuilderException;
import com.github.jaycleverly.stock_info.model.DailyStockMetrics;
import com.github.jaycleverly.stock_info.model.DailyStockRecord;

/**
 * Class to calculate different metrics about a particular stock record
 */
@Service
public class MetricBuilderService {
    private final int movingAveragePeriod;
    private final int volatilityPeriod;
    private final int momentumPeriod;

    /**
     * Creates a new service that can build metric objects from stock records
     * 
     * @param calculationsProperties the properties set for the application
     */
    public MetricBuilderService(AppCalculationsProperties calculationsProperties) {
        this.movingAveragePeriod = calculationsProperties.movingAveragePeriod();
        this.volatilityPeriod = calculationsProperties.volatilityPeriod();
        this.momentumPeriod = calculationsProperties.momentumPeriod();
    }   
    
    /**
     * Calculates metrics for a specific date in a stock's records
     * 
     * @param date the date of a record to calculate metrics for
     * @param history all the records associated with the stock 
     * @return an object containing all of the metrics
     * @throws MetricBuilderException if an error occurs during the metric calculation process
     */
    public DailyStockMetrics caclculateMetrics(LocalDate date, List<DailyStockRecord> history) throws MetricBuilderException {
        try {
            DailyStockRecord recordToAnalyse = history.stream()
                .filter(r -> r.getDate().equals(date))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Date (%s) not found in records", date.toString())));
        
            String symbol = recordToAnalyse.getSymbol();
            double close = round2dp(recordToAnalyse.getClose());
            Double previousCloseChange = round2dp(calculateChangeFromPreviousClose(history, recordToAnalyse));
            Double movingAverage = round2dp(calculateMovingAverage(history, recordToAnalyse));
            Double volatility = round2dp(calculateVolatility(history, recordToAnalyse));
            Double momentum = round2dp(calculateMomentum(history, recordToAnalyse));

            return new DailyStockMetrics(symbol, date, close, previousCloseChange, movingAverage, volatility, momentum);
        
        } catch (Exception exception) {
            throw new MetricBuilderException("Exception when building metrics for stock!", exception);
        }
    }

    private Double calculateChangeFromPreviousClose(List<DailyStockRecord> records, DailyStockRecord recordToAnalyse) {
        int index = records.indexOf(recordToAnalyse);

        if (index >= 1) {
            return records.get(index).getClose() - records.get(index - 1).getClose();
        }
        return null;
    }

    private Double calculateMovingAverage(List<DailyStockRecord> records, DailyStockRecord recordToAnalyse) {
        int index = records.indexOf(recordToAnalyse);

        if (index >= movingAveragePeriod - 1) {
            double sum = 0;
            for (int j = (index - movingAveragePeriod) + 1; j <= index; j++) {
                sum += records.get(j).getClose();
            }
            return sum / movingAveragePeriod;
        }
        return null;
    }

    private Double calculateVolatility(List<DailyStockRecord> records, DailyStockRecord recordToAnalyse) {
        int index = records.indexOf(recordToAnalyse);

        if (index > volatilityPeriod - 1) {
            List<Double> returns = new ArrayList<>();
            for (int j = (index - volatilityPeriod) + 1; j <= index; j++) {
                double prevClose = records.get(j - 1).getClose();
                returns.add((records.get(j).getClose() - prevClose) / prevClose);
            }
            double mean = returns.stream().mapToDouble(d -> d).average().orElse(0);
            double variance = returns.stream().mapToDouble(r -> Math.pow(r - mean, 2)).sum() / returns.size();
            return Math.sqrt(variance);
        }
        return null;
    }

    private Double calculateMomentum(List<DailyStockRecord> records, DailyStockRecord recordToAnalyse) {
        int index = records.indexOf(recordToAnalyse);

        if (index > momentumPeriod - 1) {
            double historicalClose = records.get(index - momentumPeriod).getClose();
            return (recordToAnalyse.getClose() - historicalClose) / historicalClose;
        }
        return null;
    }

    private Double round2dp(Double value) {
        return value == null 
            ? null 
            : Math.round(value * 100.0) / 100.0;
    }
}
