package com.github.jaycleverly.stock_info.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.jaycleverly.stock_info.dto.DailyStockMetrics;

public class MetricFormatterServiceTest {
    private static List<DailyStockMetrics> inputListMock = new ArrayList<>();

    @BeforeEach
    void setup() throws Exception {
        inputListMock.add(new DailyStockMetrics("symbol", LocalDate.now(), 100.0, 10.0, null, null, null));
        inputListMock.add(new DailyStockMetrics("symbol", LocalDate.now().minusDays(1), 90.0, null, null, null, null));
    }

    @Test
    void shouldReturnValidJson() {
        String expectedResult = """
        {
          "Meta Data" : {
            "1. Information" : "Daily Time Series with custom metrics",
            "2. Symbol" : "symbol",
            "3. Last Refreshed" : "2025-09-05",
            "4. Time Zone" : "Europe/London"
          },
          "Time Series (Daily)" : {
            "2025-09-05" : {
              "1. close" : "100.00",
              "2. previousCloseChange" : "10.00",
              "3. movingAverage(30d)" : null,
              "4. volatility(7d)" : null,
              "5. momentum(14d)" : null
            },
            "2025-09-04" : {
              "1. close" : "90.00",
              "2. previousCloseChange" : null,
              "3. movingAverage(30d)" : null,
              "4. volatility(7d)" : null,
              "5. momentum(14d)" : null
            }
          }
        }""";

        String result = MetricFormatterService.convertMetricsToJson(inputListMock);
        assertEquals(expectedResult, result);
    }
}
