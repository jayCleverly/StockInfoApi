package com.github.jaycleverly.stock_info.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.jaycleverly.stock_info.exception.ParserException;
import com.github.jaycleverly.stock_info.model.DailyStockRecord;

public class StockRecordParserTest {
  @Test
    void shouldParseSingleRecord() throws IOException {
        String mockJsonData = """
        {
          "Meta Data": {
            "1. Information": "Daily Time Series with Splits and Dividend Events",
            "2. Symbol": "IBM",
            "3. Last Refreshed":"2025-08-27"
          },
          "Time Series (Daily)": {
            "2025-08-27": {
              "1. open": "100.5",
              "2. high": "105.0",
              "3. low": "99.8",
              "4. close": "102.3",
              "5. adjusted close": "102.3",
              "6. volume": "1500000"
            }
          }
        }
        """;

        List<DailyStockRecord> result = StockRecordsParser.parse(mockJsonData);
        DailyStockRecord mockRecord = result.get(0);

        assertEquals(1, result.size());
        assertEquals(100.5, mockRecord.getOpen());
        assertEquals(105.0, mockRecord.getHigh());
        assertEquals(99.8, mockRecord.getLow());
        assertEquals(102.3, mockRecord.getClose());
    }

    @Test
    void shouldParseMultipleRecords() throws IOException {
        String mockJsonData = """
        {
          "Meta Data": {
            "1. Information": "Daily Time Series with Splits and Dividend Events",
            "2. Symbol": "IBM",
            "3. Last Refreshed":"2025-08-27"
          },
          "Time Series (Daily)": {
            "2025-08-27": {
              "1. open": "100.5",
              "2. high": "105.0",
              "3. low": "99.8",
              "4. close": "102.3",
              "5. adjusted close": "102.3",
              "6. volume": "1500000"
            },
            "2025-08-26": {
              "1. open": "98.0",
              "2. high": "101.0",
              "3. low": "97.5",
              "4. close": "99.5",
              "5. adjusted close": "99.5",
              "6. volume": "1200000"
            }
          }
        }
        """;

        List<DailyStockRecord> result = StockRecordsParser.parse(mockJsonData);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(record -> record.getDate().equals(LocalDate.of(2025, 8, 27))));
        assertTrue(result.stream().anyMatch(record -> record.getDate().equals(LocalDate.of(2025, 8, 26))));
        assertTrue(result.get(1).getDate().equals(LocalDate.of(2025, 8, 27)));
    }

    @Test
    void shouldThrowExceptionOnInvalidJsonResponse() {
      String mockJsonData = """
        {
          "Meta Data": {
            "1. Information": "Daily Time Series with Splits and Dividend Events"
          },
          "Time Series (Daily)": {
            "2025-08-27": {
              "1. open": "100.5",
              "2. high": "105.0",
            },
            "2025-08-26": {
              "1. open": "98.0",
              "2. high": "101.0",
              "3. low": "97.5",
              "4. close": "99.5",
              "5. adjusted close": "99.5",
              "6. volume": "1200000"
            }
          }
        }
        """;

        ParserException exception = assertThrows(ParserException.class, () -> StockRecordsParser.parse(mockJsonData));
        assertTrue(exception.getMessage().equals("Exception when parsing stock records!"));
        assertTrue(exception.getCause().getClass().getSimpleName().equals("JsonParseException"));
    }

    @Test
    void shouldThrowExceptionOnIncompleteResponse() {
      String mockApiResponseJson = """
        {
          "Meta Data": {
            "1. Information": "Daily Time Series with Splits and Dividend Events",
            "2. Symbol": "IBM",
            "3. Last Refreshed":"2025-08-27"
          },
          "Time Series (Daily)": {
            "2025-08-27": {
              "1. open": "100.5",
              "2. high": "105.0",
              "4. close": "102.3",
              "6. volume": "1500000"
            }
          }
        }
        """;

        ParserException exception = assertThrows(ParserException.class, () -> StockRecordsParser.parse(mockApiResponseJson));
        assertTrue(exception.getMessage().equals("Exception when parsing stock records!"));
        assertTrue(exception.getCause().getClass().getSimpleName().equals("NullPointerException"));
    }
}
