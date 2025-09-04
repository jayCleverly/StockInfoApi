package com.github.jaycleverly.stock_info.parser;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jaycleverly.stock_info.dto.DailyStockRecord;
import com.github.jaycleverly.stock_info.exception.ExternalApiProcessingException;

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
     * @throws ExternalApiProcessingException if a record cannot be parsed from the json
     */
    public static List<DailyStockRecord> parse(String stockHistoricalData) throws ExternalApiProcessingException {
        try {
            JsonNode root = objectMapper.readTree(stockHistoricalData);
            JsonNode symbol = root.path("Meta Data").path("2. Symbol");
            JsonNode timeSeries = root.path("Time Series (Daily)");
    
            List<DailyStockRecord> records = new ArrayList<>();
            timeSeries.fieldNames().forEachRemaining(dateStr -> {
                records.add(toRecord(symbol.asText(), LocalDate.parse(dateStr), timeSeries.get(dateStr)));
    
            });
    
            // More recent the record, higher the index
            return records.reversed();

        } catch (JsonProcessingException | NullPointerException exception) {
            throw new ExternalApiProcessingException("Exception when parsing api results!", exception);
        }
    }

    private static DailyStockRecord toRecord(String symbol, LocalDate infoOriginDate, JsonNode stockInfo) throws RuntimeException {
        return new DailyStockRecord(
                symbol,
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
