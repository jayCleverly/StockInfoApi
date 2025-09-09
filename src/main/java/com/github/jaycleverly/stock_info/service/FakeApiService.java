package com.github.jaycleverly.stock_info.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jaycleverly.stock_info.config.properties.AppLimitsProperties;
import com.github.jaycleverly.stock_info.exception.FakeApiException;
import com.github.jaycleverly.stock_info.model.DailyStockRecord;

/**
 * Fake API that is called in place of a traditional endpoint - no daily limits on calls. 
 */
@Service
public class FakeApiService {
    private static final Random RANDOM = new Random();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(FakeApiService.class);

    private final int defaultHistoryPeriod;
    private final Map<String, List<DailyStockRecord>> stockData;

    /**
     * Creates a new service mimicking an external API
     * 
     * @param limitsProperties the properties set for the application
     */
    public FakeApiService(AppLimitsProperties limitsProperties) {
        this.defaultHistoryPeriod = limitsProperties.apiRecords();
        this.stockData = new HashMap<>();
    }

    /**
     * Returns a historical record of stock data in a json format
     * 
     * @param symbol the stock symbol to get records for
     * @param numRecords the number of records to recover (start with most recent, work backwards)
     * @return a json object containing metadata about the stock and historical records
     * @throws FakeApiException if the api cannot produce a valid response
     */
    public String getStockData(String symbol, int numRecords) throws FakeApiException {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Stock symbol must not be null or empty!");
        }
        symbol = symbol.toUpperCase(Locale.ENGLISH);

        List<DailyStockRecord> history = stockData.get(symbol);
        if (history == null) {
            history = generateInitialHistory(symbol, defaultHistoryPeriod);
            stockData.put(symbol, history);
        }

        // Trim to last numRecords
        if (numRecords > 0 && numRecords < history.size()) {
            history = history.subList(history.size() - numRecords, history.size());
        }

        try {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("Meta Data", buildMetadataHeader(symbol, history));
            response.put("Time Series (Daily)", buildTimeSeries(history));
                
            return OBJECT_MAPPER.writeValueAsString(response);

        } catch (JsonProcessingException exception) {
            throw new FakeApiException("Exception when generating response!", exception);
        }
    }

    /**
     * Adds a new record to each stock at midnight every day UTC
     */
    @Scheduled(cron = "0 0 0 * * ?", zone = "UTC")
    private void addNewDailyRecord() {
        for (String symbol : stockData.keySet()) {
            List<DailyStockRecord> history = stockData.get(symbol);
            double lastClose = history.get(history.size() - 1).getClose();
            double newClose = generateNextPrice(lastClose);

            LOGGER.info(String.format("Fake API adding new stock record for symbol %s...", symbol));
            history.add(generateDailyStockRecord(symbol, LocalDate.now().minusDays(1), newClose));
        }
    }

    private List<DailyStockRecord> generateInitialHistory(String symbol, int days) {
        List<DailyStockRecord> history = new ArrayList<>();

        double price = 100 + RANDOM.nextDouble() * 100;
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i + 1); // Stock info is generated at market close the day before
            price = generateNextPrice(price);
            history.add(generateDailyStockRecord(symbol, date, price));
        }
        return history;
    }

    private double generateNextPrice(double previousClose) {
        // -2% to +2% price change, upward drift of +0.1%
        return previousClose * (1 + (RANDOM.nextDouble() - 0.5) * 0.04 + 0.001);
    }

    private DailyStockRecord generateDailyStockRecord(String symbol, LocalDate date, double basePrice) {
        double open = basePrice;
        double high = open * (1 + RANDOM.nextDouble() * 0.02);
        double low = open * (1 - RANDOM.nextDouble() * 0.02);
        double close = low + (high - low) * RANDOM.nextDouble();
        long volume = 1_000_000 + RANDOM.nextInt(5_000_000);

        return new DailyStockRecord(symbol, date, open, high, low, close, close, volume);
    }
    
    private Map<String, String> buildMetadataHeader(String symbol, List<DailyStockRecord> history) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("1. Information", "Daily Time Series with Splits and Dividend Events");
        metadata.put("2. Symbol", symbol);
        metadata.put("3. Last Refreshed", history.get(history.size() - 1).getDate().toString());
        
        return metadata;
    }
    
    private Map<String, Object> buildTimeSeries(List<DailyStockRecord> history) {
        Map<String, Object> timeSeries = new LinkedHashMap<>();
        // Records should be shown in descending order of dates
        for (DailyStockRecord p : history.reversed()) {
            timeSeries.put(p.getDate().toString(), buildNewRecord(p));
        }
        return timeSeries;
    }
    
    private Map<String, Object> buildNewRecord(DailyStockRecord p) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("1. open", convertDoubleToString(p.getOpen(), "%.2f"));
        record.put("2. high", convertDoubleToString(p.getHigh(), "%.2f"));
        record.put("3. low", convertDoubleToString(p.getLow(), "%.2f"));
        record.put("4. close", convertDoubleToString(p.getClose(), "%.2f"));
        record.put("5. adjusted close", convertDoubleToString(p.getAdjustedClose(), "%.2f"));
        record.put("6. volume", String.valueOf(p.getVolume()));

        return record;
    }
    
    private String convertDoubleToString(double value, String dpFormat) {
        return String.format(dpFormat, value);
    }
}
