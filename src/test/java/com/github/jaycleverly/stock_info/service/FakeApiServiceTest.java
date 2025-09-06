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
import com.github.jaycleverly.stock_info.config.properties.AppLimitsProperties;

public class FakeApiServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private final AppLimitsProperties limitsProperties = new AppLimitsProperties(50, 100);
    private final int numRecords = limitsProperties.metricRecords();

    @Test
    void shouldReturnValidJsonObject() throws JsonProcessingException {
        String symbol = "AAPL";
        FakeApiService fakeApiService = new FakeApiService(limitsProperties);
        String json = fakeApiService.getStockData(symbol, numRecords);
        JsonNode root = objectMapper.readTree(json);

        assertNotNull(json);
        assertTrue(root.has("Meta Data"));
        assertTrue(root.has("Time Series (Daily)"));
    }

    @Test
    void shouldReturnConsistentData() throws JsonProcessingException {
        String symbol = "FB";
        FakeApiService fakeApiService = new FakeApiService(limitsProperties);
        String firstCall = fakeApiService.getStockData(symbol, numRecords);
        String secondCall = fakeApiService.getStockData(symbol, numRecords);

        assertEquals(firstCall, secondCall);
    }

    @Test
    void shouldContainCorrectMetadataDate() throws JsonProcessingException {
        String symbol = "GOOG";
        FakeApiService fakeApiService = new FakeApiService(limitsProperties);
        String json = fakeApiService.getStockData(symbol, numRecords);
        JsonNode root = objectMapper.readTree(json);

        assertEquals(LocalDate.now().minusDays(1).toString(), 
                     root.get("Meta Data").get("3. Last Refreshed").asText());
    }

    @Test
    void shouldContainDefaultHistoryLength() throws JsonProcessingException {
        String symbol = "MSFT";
        FakeApiService fakeApiService = new FakeApiService(limitsProperties);
        String json = fakeApiService.getStockData(symbol, numRecords);

        JsonNode root = objectMapper.readTree(json);
        JsonNode timeSeries = root.get("Time Series (Daily)");

        assertEquals(numRecords, timeSeries.size());
    }

    @Test
    void shouldThrowExceptionOnInvalidInput() {
        String symbol = "";
        FakeApiService fakeApiService = new FakeApiService(limitsProperties);

        Exception exception = assertThrows(Exception.class, () -> fakeApiService.getStockData(symbol, numRecords));
        assertTrue(exception.getMessage().contains("Stock symbol must not be null or empty!"));
    }
}
