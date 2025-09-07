package com.github.jaycleverly.stock_info.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import com.github.jaycleverly.stock_info.client.DynamoClient;
import com.github.jaycleverly.stock_info.config.properties.AppLimitsProperties;
import com.github.jaycleverly.stock_info.exception.DynamoClientException;
import com.github.jaycleverly.stock_info.exception.StockAnalysisException;
import com.github.jaycleverly.stock_info.model.DailyStockMetrics;
import com.github.jaycleverly.stock_info.model.DailyStockRecord;
import com.github.jaycleverly.stock_info.parser.StockRecordsParser;
import com.github.jaycleverly.stock_info.serializer.StockMetricsSerializer;

public class StockAnalysisServiceTest {
    private static final String MOCK_SYMBOL = "MOCK";
    private static final String MOCK_JSON_RECORDS = "Mocked JSON records";
    private static final String MOCK_JSON_METRICS = "Mocked JSON metrics";

    private static MockedStatic<StockRecordsParser> parserMock;
    private static MockedStatic<StockMetricsSerializer> serializerMock;

    private final List<DailyStockRecord> mockRecordHistory = new ArrayList<>();
    private final List<DailyStockMetrics> mockMetricHistory = new ArrayList<>();
    private final AppLimitsProperties appLimitsProperties = new AppLimitsProperties(50, 100);
    private final int numRecords = appLimitsProperties.metricRecords();

    @Mock
    private DynamoClient dynamoClientMock;
    @Mock
    private FakeApiService fakeApiServiceMock;
    @Mock
    private MetricBuilderService metricBuilderServiceMock;
    private StockAnalysisService stockAnalysisService;

    @BeforeEach
    void setup() {
        parserMock = mockStatic(StockRecordsParser.class);
        serializerMock = mockStatic(StockMetricsSerializer.class);

        MockitoAnnotations.openMocks(this);
        stockAnalysisService = new StockAnalysisService(appLimitsProperties, dynamoClientMock, fakeApiServiceMock, metricBuilderServiceMock);

        // Generate data for stock history + metrics
        LocalDate startDate = LocalDate.now().minusDays(numRecords);
        for (int i = 0; i < numRecords; i++) {
            mockRecordHistory.add(new DailyStockRecord(MOCK_SYMBOL, startDate.plusDays(i), 0, 0, 0, 100 + i, 100 + i, 0L));
        }
        for (int i = 0; i < numRecords; i++) {
            mockMetricHistory.add(new DailyStockMetrics(MOCK_SYMBOL, startDate.plusDays(i), 100.0, null, null, null, null));
        }
    }

    @AfterEach
    void cleanup() {
        parserMock.close();
        serializerMock.close();
    }

    @Test
    void shouldAddAllNewRecordsInDynamo() {
        when(dynamoClientMock.query(any(), any(), anyInt(), eq(DailyStockMetrics.class))).thenReturn(Collections.emptyList());
        when(fakeApiServiceMock.getStockData(eq(MOCK_SYMBOL), anyInt())).thenReturn(MOCK_JSON_RECORDS);
        parserMock.when(() -> StockRecordsParser.parse(eq(MOCK_JSON_RECORDS))).thenReturn(mockRecordHistory);
        when(metricBuilderServiceMock.caclculateMetrics(any(LocalDate.class), eq(mockRecordHistory)))
            .thenAnswer(invocation -> {
                return mockMetricHistory.stream()
                    .filter(m -> m.getDate().equals(invocation.getArgument(0)))
                    .findFirst()
                    .orElse(null);
            });
        serializerMock.when(() -> StockMetricsSerializer.serialize(anyList())).thenReturn(MOCK_JSON_METRICS);

        String result = stockAnalysisService.produceAnalysis(MOCK_SYMBOL, null, null);
        assertEquals(MOCK_JSON_METRICS, result);

        verify(dynamoClientMock, times(numRecords))
                .putItem(anyString(), any(DailyStockMetrics.class), eq(DailyStockMetrics.class));
    }

    @Test
    void shouldAddSomeNewRecordsInDynamo() {
        int recordsPresent = new Random().nextInt(numRecords) + 1;
        List<DailyStockRecord> mockApiRecords = mockRecordHistory.subList(recordsPresent, mockRecordHistory.size());

        when(dynamoClientMock.query(any(), any(), anyInt(), eq(DailyStockMetrics.class)))
            .thenAnswer(invocation -> new ArrayList<>(mockMetricHistory.subList(0, recordsPresent)));
        when(fakeApiServiceMock.getStockData(eq(MOCK_SYMBOL), anyInt())).thenReturn(MOCK_JSON_RECORDS);
        parserMock.when(() -> StockRecordsParser.parse(eq(MOCK_JSON_RECORDS))).thenReturn(mockApiRecords);
        when(metricBuilderServiceMock.caclculateMetrics(any(LocalDate.class), eq(mockApiRecords)))
            .thenAnswer(invocation -> {
                return mockMetricHistory.stream()
                    .filter(m -> m.getDate().equals(invocation.getArgument(0)))
                    .findFirst()
                    .orElse(null);
            });
        serializerMock.when(() -> StockMetricsSerializer.serialize(anyList())).thenReturn(MOCK_JSON_METRICS);

        String result = stockAnalysisService.produceAnalysis(MOCK_SYMBOL, null, null);
        assertEquals(MOCK_JSON_METRICS, result);

        verify(dynamoClientMock, times(numRecords - recordsPresent))
                .putItem(anyString(), any(DailyStockMetrics.class), eq(DailyStockMetrics.class));
    }

    @Test
    void shouldAddNoNewRecordsInDynamo() {
        when(dynamoClientMock.query(any(), any(), anyInt(), eq(DailyStockMetrics.class)))
            .thenAnswer(invocation -> new ArrayList<>(mockMetricHistory));
        serializerMock.when(() -> StockMetricsSerializer.serialize(anyList())).thenReturn(MOCK_JSON_METRICS);

        String result = stockAnalysisService.produceAnalysis(MOCK_SYMBOL, null, null);
        assertEquals(MOCK_JSON_METRICS, result);

        verify(dynamoClientMock, times(0))
                .putItem(anyString(), any(DailyStockMetrics.class), eq(DailyStockMetrics.class));
    }

    @Test
    void shouldProduceAnalysisWithCustomDate() {
        ArrayList<DailyStockMetrics> mockMetricCustomRange = new ArrayList<>(mockMetricHistory.subList(numRecords - 15, numRecords - 1));
        ArrayList<DailyStockRecord> mockRecordCustomRange = new ArrayList<>(mockRecordHistory.subList(mockRecordHistory.size() - 1, mockRecordHistory.size()));

        when(dynamoClientMock.query(any(), any(), anyInt(), eq(DailyStockMetrics.class)))
            .thenAnswer(invocation -> new ArrayList<>(mockMetricCustomRange));
        when(fakeApiServiceMock.getStockData(eq(MOCK_SYMBOL), anyInt())).thenReturn(MOCK_JSON_RECORDS);
        parserMock.when(() -> StockRecordsParser.parse(eq(MOCK_JSON_RECORDS))).thenReturn(mockRecordCustomRange);
        when(metricBuilderServiceMock.caclculateMetrics(any(LocalDate.class), eq(mockRecordCustomRange)))
            .thenAnswer(invocation -> {
                return mockMetricHistory.stream()
                    .filter(m -> m.getDate().equals(invocation.getArgument(0)))
                    .findFirst()
                    .orElse(null);
            });
        serializerMock.when(() -> StockMetricsSerializer.serialize(anyList())).thenReturn(MOCK_JSON_METRICS);
        
        String result = stockAnalysisService.produceAnalysis(
            MOCK_SYMBOL, 
            mockMetricCustomRange.getFirst().getDate().toString(),
            null);
        assertEquals(MOCK_JSON_METRICS, result);

        verify(dynamoClientMock, times(mockRecordCustomRange.size()))
                .putItem(anyString(), any(DailyStockMetrics.class), eq(DailyStockMetrics.class));
    }

    @Test
    void shouldFailToProduceAnalysis() {
        when(dynamoClientMock.query(any(), any(), anyInt(), eq(DailyStockMetrics.class))).thenThrow(new DynamoClientException("Exception!", null));

        StockAnalysisException exception = assertThrows(StockAnalysisException.class, () -> 
            stockAnalysisService.produceAnalysis(MOCK_SYMBOL, null, null));

        assertTrue(exception.getMessage().equals(
            String.format("Exception when producing %s analysis over the period %s - %s!", 
                MOCK_SYMBOL, 
                LocalDate.now().minusDays(numRecords), 
                LocalDate.now().minusDays(1))));
        assertTrue(exception.getCause().getClass().getSimpleName().equals("DynamoClientException"));
    }
}
