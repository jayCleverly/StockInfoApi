package com.github.jaycleverly.stock_info.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import com.github.jaycleverly.stock_info.dto.DateRange;

/**
 * Class to provide helper methods when processing dates
 */
public class DateUtils {
    /**
     * Verifys dates are valid and fills in nulls
     * 
     * @param startDate the starting date
     * @param endDate the ending date
     * @param minStartDate the earliest a start date can be
     * @param maxEndDate the latest a start date can be
     * @return a DateRange object containing a valid start/end date
     * @throws IllegalArgumentException if the entered dates do not meet certain criteria
     */
    public static DateRange verifyDateRange(LocalDate startDate, 
                                            LocalDate endDate, 
                                            LocalDate minStartDate, 
                                            LocalDate maxEndDate) throws IllegalArgumentException {
        // Apply default values if no input
        startDate = (startDate != null) ? startDate : minStartDate;
        endDate = (endDate != null) ? endDate : maxEndDate;
        
        if (startDate.isBefore(minStartDate)) {
            throw new IllegalArgumentException(
                String.format("Start date cannot be earlier than %s!", minStartDate)
            );
        } else if (endDate.isAfter(maxEndDate)) {
            throw new IllegalArgumentException(
                String.format("End date cannot be later than %s!", maxEndDate)
            );
        } else if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                String.format("Start date %s must come before the end date %s", startDate, endDate)
            );
        } else {
            return new DateRange(startDate, endDate);
        }
    }

    /**
     * Gets the number of days between days
     * 
     * @param startDate the starting date
     * @param endDate the ending date
     * @return number of days
     */
    public static int calculateNumDaysBetweenDates(LocalDate startDate, LocalDate endDate) {
        return (int) ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * Gets a date in the past
     * 
     * @param numDaysAgo the number of days in the past to get a date for
     * @return a date
     */
    public static LocalDate getPastDate(int numDaysAgo) {
        return LocalDate.now().minusDays(numDaysAgo);
    }

    /**
     * Converts a string to date object
     * 
     * @param date the string to convert
     * @return a date object
     * @throws IllegalArgumentException if the date cannot be parsed
     */
    public static LocalDate convertToDate(String date) throws IllegalArgumentException {
        try {
            return (date != null) ? LocalDate.parse(date) : null;
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Date must be in the format (YYYY-MM-DD)!");
        }
    }
}
