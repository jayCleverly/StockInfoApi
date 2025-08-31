package com.github.jaycleverly.stock_info.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.jaycleverly.stock_info.dto.DailyStockAnalysis;
import com.github.jaycleverly.stock_info.dto.DailyStockRecord;
import com.github.jaycleverly.stock_info.dto.StockHistory;

public class StockAnalysisServiceTest {
    private static StockHistory mockStockHistory;

    @BeforeAll
    static void setUp() {
        List<DailyStockRecord> mockRecords = new ArrayList<>();

        // Generate 50 days of mock data.
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        for (int i = 0; i < 49; i++) {
            mockRecords.add(new DailyStockRecord(startDate.plusDays(i), 0, 0, 0, 100 + i, 100 + i, 0L));
        }

        mockStockHistory = new StockHistory("AAPL", mockRecords);
    }
    
    @Test
    void shouldReturnCorrectMetrics() {
        LocalDate dateToTest = LocalDate.of(2025, 2, 18);
        DailyStockAnalysis analysis = StockAnalysisService.caclculateMetrics(dateToTest, mockStockHistory);

        assertEquals(148, analysis.getClose());
        assertEquals(1.0, analysis.getPreviousCloseChange());
        assertEquals(133.5, analysis.getMovingAverage());
        assertEquals(0.0, analysis.getVolatility());
        assertEquals(0.10, analysis.getMomentum());
    }

    @Test
    void shouldReturnNullsForUnsuitableRecords() {
        LocalDate dateToTest = LocalDate.of(2025, 1, 1);
        DailyStockAnalysis analysis = StockAnalysisService.caclculateMetrics(dateToTest, mockStockHistory);

        assertEquals(100, analysis.getClose());
        assertEquals(null, analysis.getPreviousCloseChange());
        assertEquals(null, analysis.getMovingAverage());
        assertEquals(null, analysis.getVolatility());
        assertEquals(null, analysis.getMomentum());
    }

    @Test
    void shouldThrowErrorForInvalidDate() {
        LocalDate dateToTest = LocalDate.of(2025, 2, 19);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> StockAnalysisService.caclculateMetrics(dateToTest, mockStockHistory));
        assertTrue(exception.getMessage().equals("Date (2025-02-19) not found in records"));
    }
}
