package com.github.jaycleverly.stock_info.parser;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jaycleverly.stock_info.exception.ParserException;
import com.github.jaycleverly.stock_info.model.DailyStockRecord;

/**
 * Parser to convert a json response into a custom object. 
 */
public class StockRecordsParser {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Parses a json response in a specific format to a list of stock records
     * 
     * @param stockData stock data in a json format
     * @return a new object containing a list of daily stock records
     * @throws ParserException if a record cannot be parsed from the json
     */
    public static List<DailyStockRecord> parse(String stockData) throws ParserException {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(stockData);
            JsonNode symbol = root.path("Meta Data").path("2. Symbol");
            JsonNode timeSeries = root.path("Time Series (Daily)");
    
            List<DailyStockRecord> records = new ArrayList<>();
            timeSeries.fieldNames().forEachRemaining(dateStr -> {
                records.add(toRecord(symbol.asText(), LocalDate.parse(dateStr), timeSeries.get(dateStr)));
            });

            // More recent the record, higher the index
            return records.reversed();

        } catch (JsonProcessingException | NullPointerException exception) {
            throw new ParserException("Exception when parsing stock records!", exception);
        }
    }

    private static DailyStockRecord toRecord(String symbol, LocalDate infoOriginDate, JsonNode stockInfo) {
        return new DailyStockRecord(
            symbol,
            infoOriginDate,
            stockInfo.get("1. open").asDouble(),
            stockInfo.get("2. high").asDouble(),
            stockInfo.get("3. low").asDouble(),
            stockInfo.get("4. close").asDouble()
        );
    }
}
