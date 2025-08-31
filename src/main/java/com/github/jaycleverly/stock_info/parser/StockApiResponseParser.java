package com.github.jaycleverly.stock_info.parser;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jaycleverly.stock_info.dto.DailyStockRecord;
import com.github.jaycleverly.stock_info.dto.StockHistory;

/**
 * Parser to convert a json response from a stock API into a custom object. 
 */
public class StockApiResponseParser {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parses a json response to an object
     * 
     * @param stockHistoricalData the response from a stock api
     * @return a new object containing a list of daily stock records
     * @throws JsonMappingException if the json structure does not match expected format
     * @throws JsonProcessingException if the json cannot be parsed
     * @throws RuntimeException if a record cannot be formed from the json
     */
    public static StockHistory parse(String stockHistoricalData) throws JsonMappingException, JsonProcessingException, RuntimeException {
        JsonNode root = objectMapper.readTree(stockHistoricalData);
        JsonNode symbol = root.path("Meta Data").path("2. Symbol");
        JsonNode timeSeries = root.path("Time Series (Daily)");

        List<DailyStockRecord> records = new ArrayList<>();
        timeSeries.fieldNames().forEachRemaining(dateStr -> {
            try {
                records.add(toRecord(timeSeries.get(dateStr), LocalDate.parse(dateStr)));
            } catch (Exception e) {
                throw new RuntimeException(String.format("Error when creating records: %s", e.getMessage()));
            }
        });

        // More recent the record, higher the index
        return new StockHistory(symbol.toString(), records.reversed());
    }

    private static DailyStockRecord toRecord(JsonNode stockInfo, LocalDate infoOriginDate) throws RuntimeException {
        return new DailyStockRecord(
                infoOriginDate,
                stockInfo.get("1. open").asDouble(),
                stockInfo.get("2. high").asDouble(),
                stockInfo.get("3. low").asDouble(),
                stockInfo.get("4. close").asDouble(),
                stockInfo.get("5. adjusted close").asDouble(),
                stockInfo.get("6. volume").asLong()
            );
    }
}
