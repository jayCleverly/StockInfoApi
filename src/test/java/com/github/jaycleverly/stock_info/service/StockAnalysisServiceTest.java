package com.github.jaycleverly.stock_info.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;

import com.github.jaycleverly.stock_info.client.DynamoClient;
import com.github.jaycleverly.stock_info.dto.DailyStockMetrics;
import com.github.jaycleverly.stock_info.dto.DailyStockRecord;
import com.github.jaycleverly.stock_info.exception.DynamoClientException;
import com.github.jaycleverly.stock_info.exception.StockAnalysisException;
import com.github.jaycleverly.stock_info.parser.StockApiResponseParser;

@SpringBootTest
public class StockAnalysisServiceTest {
    private static final String MOCK_SYMBOL = "MOCK";
    private static final int NUM_RECORDS = 50;

    private static DynamoClient dynamoClientMock;
    private static MockedStatic<FakeApiService> fakeApiMock;
    private static MockedStatic<StockApiResponseParser> parserMock;
    private static MockedStatic<MetricBuilderService> metricBuilderMock;
    private static MockedStatic<MetricFormatterService> metricFormatterMock;

    private List<DailyStockRecord> mockRecordHistory = new ArrayList<>();
    private List<DailyStockMetrics> mockMetricHistory = new ArrayList<>();
    private String mockJsonAnalysis = "Mocked JSON response";

    @BeforeEach
    void setup() throws Exception {
        dynamoClientMock = mock(DynamoClient.class);
        
        Field clientField = StockAnalysisService.class.getDeclaredField("dynamoClient");
        clientField.setAccessible(true);
        clientField.set(null, dynamoClientMock);

        fakeApiMock = mockStatic(FakeApiService.class);
        parserMock = mockStatic(StockApiResponseParser.class);
        metricBuilderMock = mockStatic(MetricBuilderService.class);
        metricFormatterMock = mockStatic(MetricFormatterService.class);

        // Generate data for stock history + metrics
        LocalDate startDate = LocalDate.now().minusDays(NUM_RECORDS);
        for (int i = 0; i < NUM_RECORDS; i++) {
            mockRecordHistory.add(new DailyStockRecord(MOCK_SYMBOL, startDate.plusDays(i), 0, 0, 0, 100 + i, 100 + i, 0L));
        }
        for (int i = 0; i < NUM_RECORDS; i++) {
            mockMetricHistory.add(new DailyStockMetrics(MOCK_SYMBOL, startDate.plusDays(i), 100.0, null, null, null, null));
        }
    }

    @AfterEach
    void cleanup() {
        fakeApiMock.close();
        parserMock.close();
        metricBuilderMock.close();
        metricFormatterMock.close();
    }

    @Test
    void shouldAddAllNewRecordsInDynamo() throws Exception {
        when(dynamoClientMock.query(any(), any(), anyInt(), eq(DailyStockMetrics.class))).thenReturn(Collections.emptyList());

        fakeApiMock.when(() -> FakeApiService.getStockData(eq(MOCK_SYMBOL), anyInt()))
            .thenReturn("mock-json");
        parserMock.when(() -> StockApiResponseParser.parse("mock-json"))
            .thenReturn(mockRecordHistory);
        metricBuilderMock.when(() -> MetricBuilderService.caclculateMetrics(any(LocalDate.class), eq(mockRecordHistory)))
            .thenAnswer(invocation -> {
                return mockMetricHistory.stream()
                    .filter(m -> m.getDate().equals(invocation.getArgument(0)))
                    .findFirst()
                    .orElse(null);
        });
        metricFormatterMock.when(() -> MetricFormatterService.convertMetricsToJson(anyList()))
            .thenReturn(mockJsonAnalysis);

        String result = StockAnalysisService.produceAnalysis(MOCK_SYMBOL, null, null);
        assertEquals(mockJsonAnalysis, result);

        verify(dynamoClientMock, times(NUM_RECORDS))
                .putItem(anyString(), any(DailyStockMetrics.class), eq(DailyStockMetrics.class));
    }

    @Test
    void shouldAddSomeNewRecordsInDynamo() throws Exception {
        int recordsPresent = new Random().nextInt(NUM_RECORDS) + 1;

        when(dynamoClientMock.query(any(), any(), anyInt(), eq(DailyStockMetrics.class)))
            .thenAnswer(invocation -> new ArrayList<>(mockMetricHistory.subList(0, recordsPresent)));
        fakeApiMock.when(() -> FakeApiService.getStockData(eq(MOCK_SYMBOL), anyInt()))
            .thenReturn("mock-json");
        parserMock.when(() -> StockApiResponseParser.parse("mock-json"))
            .thenReturn(mockRecordHistory);
        metricBuilderMock.when(() -> MetricBuilderService.caclculateMetrics(any(LocalDate.class), eq(mockRecordHistory)))
            .thenAnswer(invocation -> {
                return mockMetricHistory.stream()
                    .filter(m -> m.getDate().equals(invocation.getArgument(0)))
                    .findFirst()
                    .orElse(null);
        });
        metricFormatterMock.when(() -> MetricFormatterService.convertMetricsToJson(anyList()))
            .thenReturn(mockJsonAnalysis);

        String result = StockAnalysisService.produceAnalysis(MOCK_SYMBOL, null, null);
        assertEquals(mockJsonAnalysis, result);

        verify(dynamoClientMock, times(NUM_RECORDS - recordsPresent))
                .putItem(anyString(), any(DailyStockMetrics.class), eq(DailyStockMetrics.class));
    }

    @Test
    void shouldAddNoNewRecordsInDynamo() throws Exception {
        when(dynamoClientMock.query(any(), any(), anyInt(), eq(DailyStockMetrics.class)))
            .thenAnswer(invocation -> new ArrayList<>(mockMetricHistory));
        metricFormatterMock.when(() -> MetricFormatterService.convertMetricsToJson(anyList()))
            .thenReturn(mockJsonAnalysis);

        String result = StockAnalysisService.produceAnalysis(MOCK_SYMBOL, null, null);
        assertEquals(mockJsonAnalysis, result);

        verify(dynamoClientMock, times(0))
                .putItem(anyString(), any(DailyStockMetrics.class), eq(DailyStockMetrics.class));
    }

    @Test
    void shouldProduceAnalysisWithCustomDate() throws Exception {
        ArrayList<DailyStockMetrics> mockMetricCustomRange = new ArrayList<>(mockMetricHistory.subList(NUM_RECORDS - 15, NUM_RECORDS - 1));
        ArrayList<DailyStockRecord> mockRecordCustomRange = new ArrayList<>(mockRecordHistory.subList(mockRecordHistory.size() - 1, mockRecordHistory.size()));

        when(dynamoClientMock.query(any(), any(), anyInt(), eq(DailyStockMetrics.class)))
            .thenAnswer(invocation -> new ArrayList<>(mockMetricCustomRange));
        
        fakeApiMock.when(() -> FakeApiService.getStockData(eq(MOCK_SYMBOL), anyInt()))
            .thenReturn("mock-json");
        parserMock.when(() -> StockApiResponseParser.parse("mock-json"))
            .thenReturn(mockRecordCustomRange);
        metricBuilderMock.when(() -> MetricBuilderService.caclculateMetrics(any(LocalDate.class), eq(mockRecordCustomRange)))
            .thenAnswer(invocation -> {
                return mockMetricHistory.stream()
                    .filter(m -> m.getDate().equals(invocation.getArgument(0)))
                    .findFirst()
                    .orElse(null);
        });
        metricFormatterMock.when(() -> MetricFormatterService.convertMetricsToJson(anyList()))
            .thenReturn(mockJsonAnalysis);
        
        String result = StockAnalysisService.produceAnalysis(
            MOCK_SYMBOL, 
            mockMetricCustomRange.getFirst().getDate().toString(),
            null);
        assertEquals(mockJsonAnalysis, result);

        verify(dynamoClientMock, times(mockRecordCustomRange.size()))
                .putItem(anyString(), any(DailyStockMetrics.class), eq(DailyStockMetrics.class));
    }

    @Test
    void shouldFailToProduceAnalysis() throws Exception {
        when(dynamoClientMock.query(any(), any(), anyInt(), eq(DailyStockMetrics.class))).thenThrow(new DynamoClientException("Exception!", null));

        StockAnalysisException exception = assertThrows(StockAnalysisException.class, () -> 
            StockAnalysisService.produceAnalysis(MOCK_SYMBOL, null, null));

        assertTrue(exception.getMessage().equals(
            String.format("Exception when producing %s analysis over the period %s - %s!", 
                MOCK_SYMBOL, 
                LocalDate.now().minusDays(NUM_RECORDS + 1), 
                LocalDate.now().minusDays(1))));
        assertTrue(exception.getCause().getClass().getSimpleName().equals("DynamoClientException"));
    }
}
