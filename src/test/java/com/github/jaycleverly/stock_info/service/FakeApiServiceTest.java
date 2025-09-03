package com.github.jaycleverly.stock_info.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FakeApiServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final int NUM_RECORDS = 50;

    @Test
    void shouldReturnValidJsonObject() throws JsonProcessingException {
        String symbol = "AAPL";
        String json = FakeApiService.getStockData(symbol,NUM_RECORDS);
        JsonNode root = objectMapper.readTree(json);

        assertNotNull(json);
        assertTrue(root.has("Meta Data"));
        assertTrue(root.has("Time Series (Daily)"));
    }

    @Test
    void shouldReturnConsistentData() throws JsonProcessingException {
        String symbol = "FB";
        String firstCall = FakeApiService.getStockData(symbol, NUM_RECORDS);
        String secondCall = FakeApiService.getStockData(symbol, NUM_RECORDS);

        assertEquals(firstCall, secondCall);
    }

    @Test
    void shouldContainCorrectMetadataDate() throws JsonProcessingException {
        String symbol = "GOOG";
        String json = FakeApiService.getStockData(symbol, NUM_RECORDS);
        JsonNode root = objectMapper.readTree(json);

        assertEquals(LocalDate.now().minusDays(1).toString(), 
                     root.get("Meta Data").get("3. Last Refreshed").asText());
    }

    @Test
    void shouldContainDefaultHistoryLength() throws JsonProcessingException {
        String symbol = "MSFT";
        String json = FakeApiService.getStockData(symbol, NUM_RECORDS);

        JsonNode root = objectMapper.readTree(json);
        JsonNode timeSeries = root.get("Time Series (Daily)");

        assertEquals(50, timeSeries.size());
    }

    @Test
    void shouldThrowExceptionOnInvalidInput() {
        String symbol = "";

        Exception exception = assertThrows(Exception.class, () -> FakeApiService.getStockData(symbol, NUM_RECORDS));
        assertTrue(exception.getMessage().contains("Stock symbol must not be null or empty!"));
    }
}
