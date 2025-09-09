package com.github.jaycleverly.stock_info.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.springframework.http.HttpStatus;

import com.github.jaycleverly.stock_info.client.DynamoClient;
import com.github.jaycleverly.stock_info.client.TimeSeriesApiClient;
import com.github.jaycleverly.stock_info.config.properties.AppLimitsProperties;
import com.github.jaycleverly.stock_info.config.properties.DynamoDbProperties;
import com.github.jaycleverly.stock_info.exception.DynamoClientException;
import com.github.jaycleverly.stock_info.exception.InternalServerErrorException;
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
    private final DynamoDbProperties dynamoDbProperties = new DynamoDbProperties(null, null, "StockMetrics");
    private final int numRecords = appLimitsProperties.compactRecords();

    @Mock
    private DynamoClient dynamoClientMock;
    @Mock
    private TimeSeriesApiClient timeSeriesApiClient;
    @Mock
    private MetricBuilderService metricBuilderServiceMock;
    private StockAnalysisService stockAnalysisService;

    @BeforeEach
    void setup() {
        parserMock = mockStatic(StockRecordsParser.class);
        serializerMock = mockStatic(StockMetricsSerializer.class);

        MockitoAnnotations.openMocks(this);
        stockAnalysisService = new StockAnalysisService(appLimitsProperties, dynamoDbProperties, dynamoClientMock, timeSeriesApiClient, metricBuilderServiceMock);

        // Generate data for stock history + metrics
        LocalDate startDate = LocalDate.now().minusDays(numRecords);
        for (int i = 0; i < numRecords; i++) {
            mockRecordHistory.add(new DailyStockRecord(MOCK_SYMBOL, startDate.plusDays(i), 0, 0, 0, 100 + i));
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
        when(timeSeriesApiClient.getDailyTimeSeries(eq(MOCK_SYMBOL))).thenReturn(MOCK_JSON_RECORDS);
        parserMock.when(() -> StockRecordsParser.parse(eq(MOCK_JSON_RECORDS))).thenReturn(mockRecordHistory);
        when(metricBuilderServiceMock.caclculateMetrics(any(LocalDate.class), eq(mockRecordHistory)))
            .thenAnswer(invocation -> {
                return mockMetricHistory.stream()
                    .filter(m -> m.getDate().equals(invocation.getArgument(0)))
                    .findFirst()
                    .orElse(null);
            });
        serializerMock.when(() -> StockMetricsSerializer.serialize(anyList())).thenReturn(MOCK_JSON_METRICS);

        String result = stockAnalysisService.produceAnalysis(MOCK_SYMBOL,false);
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
        when(timeSeriesApiClient.getDailyTimeSeries(eq(MOCK_SYMBOL))).thenReturn(MOCK_JSON_RECORDS);
        parserMock.when(() -> StockRecordsParser.parse(eq(MOCK_JSON_RECORDS))).thenReturn(mockApiRecords);
        when(metricBuilderServiceMock.caclculateMetrics(any(LocalDate.class), eq(mockApiRecords)))
            .thenAnswer(invocation -> {
                return mockMetricHistory.stream()
                    .filter(m -> m.getDate().equals(invocation.getArgument(0)))
                    .findFirst()
                    .orElse(null);
            });
        serializerMock.when(() -> StockMetricsSerializer.serialize(anyList())).thenReturn(MOCK_JSON_METRICS);

        String result = stockAnalysisService.produceAnalysis(MOCK_SYMBOL, false);
        assertEquals(MOCK_JSON_METRICS, result);

        verify(dynamoClientMock, times(numRecords - recordsPresent))
                .putItem(anyString(), any(DailyStockMetrics.class), eq(DailyStockMetrics.class));
    }

    @Test
    void shouldAddNoNewRecordsInDynamo() {
        when(dynamoClientMock.query(any(), any(), anyInt(), eq(DailyStockMetrics.class)))
            .thenAnswer(invocation -> new ArrayList<>(mockMetricHistory));
        serializerMock.when(() -> StockMetricsSerializer.serialize(anyList())).thenReturn(MOCK_JSON_METRICS);

        String result = stockAnalysisService.produceAnalysis(MOCK_SYMBOL, false);
        assertEquals(MOCK_JSON_METRICS, result);

        verify(dynamoClientMock, times(0))
                .putItem(anyString(), any(DailyStockMetrics.class), eq(DailyStockMetrics.class));
    }

    @Test
    void shouldProduceAnalysisWithFullOutputSize() {
        List<DailyStockMetrics> mockMetricCustomRange = mockMetricHistory;
        mockMetricCustomRange.addAll(mockMetricCustomRange);

        List<DailyStockRecord> mockRecordCustomRange = mockRecordHistory;
        mockRecordCustomRange.addAll(mockRecordCustomRange);

        when(dynamoClientMock.query(any(), any(), anyInt(), eq(DailyStockMetrics.class))).thenReturn(Collections.emptyList());
        when(timeSeriesApiClient.getDailyTimeSeries(eq(MOCK_SYMBOL))).thenReturn(MOCK_JSON_RECORDS);
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
            true);
        assertEquals(MOCK_JSON_METRICS, result);

        verify(dynamoClientMock, times(appLimitsProperties.fullRecords()))
                .putItem(anyString(), any(DailyStockMetrics.class), eq(DailyStockMetrics.class));
    }

    @Test
    void shouldFailToProduceAnalysis() {
        when(dynamoClientMock.query(any(), any(), anyInt(), eq(DailyStockMetrics.class))).thenThrow(new DynamoClientException("Exception!", null));

        InternalServerErrorException exception = assertThrows(InternalServerErrorException.class, () -> 
            stockAnalysisService.produceAnalysis(MOCK_SYMBOL, false));

        assertEquals(String.format("Exception when producing %s analysis!", MOCK_SYMBOL), exception.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        assertEquals("DynamoClientException", exception.getCause().getClass().getSimpleName());
    }
}
