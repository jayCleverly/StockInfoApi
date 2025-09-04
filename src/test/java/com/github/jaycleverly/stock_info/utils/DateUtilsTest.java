package com.github.jaycleverly.stock_info.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.github.jaycleverly.stock_info.dto.DateRange;
import com.github.jaycleverly.stock_info.util.DateUtils;

public class DateUtilsTest {
    
    private static final LocalDate mockMinStart = LocalDate.of(2025, 8, 1);
    private static final LocalDate mockMaxStart = LocalDate.of(2025, 9, 1);

    @Test
    void shouldProduceValidDateRange() {
        LocalDate startDate = LocalDate.of(2025, 8, 2);
        LocalDate endDate = LocalDate.of(2025, 8, 5);

        DateRange result = DateUtils.verifyDateRange(startDate, endDate, mockMinStart, mockMaxStart);
        assertEquals(startDate, result.getStartDate());
        assertEquals(endDate, result.getEndDate());
    }

    @Test
    void shouldProduceValidDateRangeWithNullInput() {
        DateRange result = DateUtils.verifyDateRange(null, null, mockMinStart, mockMaxStart);
        assertEquals(mockMinStart, result.getStartDate());
        assertEquals(mockMaxStart, result.getEndDate());
    }

    @Test
    void shouldThrowExceptionOnInvalidDateRangeInput_1() {
        LocalDate startDate = LocalDate.of(2025, 7, 2);
        LocalDate endDate = LocalDate.of(2025, 8, 5);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> DateUtils.verifyDateRange(startDate, endDate, mockMinStart, mockMaxStart));
        assertTrue(exception.getMessage().equals(String.format("Start date cannot be earlier than %s!", mockMinStart)));
    }

    @Test
    void shouldThrowExceptionOnInvalidDateRangeInput_2() {
        LocalDate startDate = LocalDate.of(2025, 8, 2);
        LocalDate endDate = LocalDate.of(2025, 9, 5);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> DateUtils.verifyDateRange(startDate, endDate, mockMinStart, mockMaxStart));
        assertTrue(exception.getMessage().equals(String.format("End date cannot be later than %s!", mockMaxStart)));
    }

    @Test
    void shouldThrowExceptionOnInvalidDateRangeInput_3() {
        LocalDate startDate = LocalDate.of(2025, 8, 2);
        LocalDate endDate = LocalDate.of(2025, 8, 1);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> DateUtils.verifyDateRange(startDate, endDate, mockMinStart, mockMaxStart));
        assertTrue(exception.getMessage().equals(String.format("Start date %s must come before the end date %s", startDate, endDate)));
    }

    @Test
    void shouldCalculateDaysBetweenDates() {
        assertEquals(DateUtils.calculateNumDaysBetweenDates(mockMinStart, mockMaxStart), 31);
    }

    @Test
    void shouldGetAPastDate() {
        assertEquals(DateUtils.getPastDate(5), LocalDate.now().minusDays(5));
    }

    @Test
    void shouldConvertStringToDate() {
        assertEquals(LocalDate.of(2025, 8, 4), DateUtils.convertToDate("2025-08-04"));
    }

    @Test
    void shouldConvertNullToNullDate() {
        assertEquals(null, DateUtils.convertToDate(null));
    }

    @Test
    void shouldFailToConvertStringToDate() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> DateUtils.convertToDate("2025-045-3"));
        assertTrue(exception.getMessage().equals("Date must be in the format (YYYY-MM-DD)!"));
    }
}
