package com.github.jaycleverly.stock_info.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.jaycleverly.stock_info.config.properties.AppCalculationsProperties;
import com.github.jaycleverly.stock_info.exception.MetricBuilderException;
import com.github.jaycleverly.stock_info.model.DailyStockMetrics;
import com.github.jaycleverly.stock_info.model.DailyStockRecord;

public class MetricBuilderServiceTest {
    private static List<DailyStockRecord> mockStockHistory = new ArrayList<>();

    private final AppCalculationsProperties calculationsProperties = new AppCalculationsProperties(30, 7, 14);

    @BeforeAll
    static void setUp() {
        // Generate 50 days of mock data.
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        for (int i = 0; i < 50; i++) {
            mockStockHistory.add(new DailyStockRecord("TEST", startDate.plusDays(i), 0, 0, 0, 100 + i));
        }
    }
    
    @Test
    void shouldReturnCorrectMetrics() {
        LocalDate dateToTest = LocalDate.of(2025, 2, 19);
        MetricBuilderService metricBuilderService = new MetricBuilderService(calculationsProperties);
        DailyStockMetrics analysis = metricBuilderService.caclculateMetrics(dateToTest, mockStockHistory);

        assertEquals(149, analysis.getClose());
        assertEquals(1.0, analysis.getPreviousCloseChange());
        assertEquals(134.5, analysis.getMovingAverage());
        assertEquals(0.01, analysis.getVolatility());
        assertEquals(10.37, analysis.getMomentum());
    }

    @Test
    void shouldReturnNullsForUnsuitableRecords() {
        LocalDate dateToTest = LocalDate.of(2025, 1, 1);
        MetricBuilderService metricBuilderService = new MetricBuilderService(calculationsProperties);
        DailyStockMetrics analysis = metricBuilderService.caclculateMetrics(dateToTest, mockStockHistory);

        assertEquals(100, analysis.getClose());
        assertEquals(null, analysis.getPreviousCloseChange());
        assertEquals(null, analysis.getMovingAverage());
        assertEquals(null, analysis.getVolatility());
        assertEquals(null, analysis.getMomentum());
    }

    @Test
    void shouldThrowErrorForInvalidDate() {
        LocalDate dateToTest = LocalDate.of(2025, 2, 20);
        MetricBuilderService metricBuilderService = new MetricBuilderService(calculationsProperties);

        MetricBuilderException exception = assertThrows(MetricBuilderException.class, () -> metricBuilderService.caclculateMetrics(dateToTest, mockStockHistory));
        assertTrue(exception.getMessage().equals("Exception when building metrics for stock!"));
        assertTrue(exception.getCause().getClass().getSimpleName().equals("IllegalArgumentException"));
    }
}
