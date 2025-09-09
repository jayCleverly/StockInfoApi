package com.github.jaycleverly.stock_info.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.jaycleverly.stock_info.exception.DynamoClientException;
import com.github.jaycleverly.stock_info.model.DailyStockMetrics;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

public class DynamoClientTest {
    private final String MOCK_TABLE_NAME = "MockTable";

    private DynamoClient dynamoClient;
    private DynamoDbEnhancedClient mockClient;
    private DynamoDbTable<DailyStockMetrics> mockTable;

    @BeforeEach
    void setUp() {
        mockClient = mock(DynamoDbEnhancedClient.class);
        mockTable = mock(DynamoDbTable.class);
        dynamoClient = new DynamoClient(mockClient);

        when(mockClient.table(anyString(), any(TableSchema.class))).thenReturn(mockTable);
    }
    
    @Test
    void shouldSuccessfullyAddNewItem() {
        DailyStockMetrics mockItem = new DailyStockMetrics("1", LocalDate.now(), 150.0, null, null, null, null);

        assertDoesNotThrow(() -> dynamoClient.putItem(MOCK_TABLE_NAME, mockItem, DailyStockMetrics.class));
        verify(mockTable).putItem(mockItem);
    }

    @Test
    void shouldThrowErrorOnPutFailure() {
        DailyStockMetrics mockItem = new DailyStockMetrics("2", LocalDate.now(), 150.0, null, null, null, null);
        doThrow(RuntimeException.class).when(mockTable).putItem(mockItem);

        Exception exception = assertThrows(DynamoClientException.class, () -> dynamoClient.putItem(MOCK_TABLE_NAME, mockItem, DailyStockMetrics.class));
        assertTrue(exception.getMessage().equals("Exception when putting an item into table (MockTable)"));
        verify(mockTable).putItem(mockItem);
    }

    @Test
    void shouldSuccessfullyQueryTable() {
        QueryConditional mockCondition = QueryConditional.keyEqualTo(
            Key.builder().partitionValue("1").build()
        );
        DailyStockMetrics mockMetrics = new DailyStockMetrics();
        List<DailyStockMetrics> mockMetricList = List.of(mockMetrics);

        PageIterable<DailyStockMetrics> mockPageIterable = mock(PageIterable.class);
        when(mockPageIterable.items()).thenReturn(() -> mockMetricList.iterator());
        when(mockTable.query(any(QueryEnhancedRequest.class))).thenReturn(mockPageIterable);

        List<DailyStockMetrics> result = dynamoClient.query(MOCK_TABLE_NAME, mockCondition, 10, DailyStockMetrics.class);
        assertEquals(mockMetricList, result);
        verify(mockTable).query(any(QueryEnhancedRequest.class));
    }

    @Test
    void shouldThrowErrorOnQueryFailure() {
        QueryConditional mockCondition = QueryConditional.keyEqualTo(
            Key.builder().partitionValue("1").build()
        );
        doThrow(RuntimeException.class).when(mockTable).query(any(QueryEnhancedRequest.class));

        Exception exception = assertThrows(DynamoClientException.class, () -> 
            dynamoClient.query(MOCK_TABLE_NAME, mockCondition, 10, DailyStockMetrics.class));
        assertTrue(exception.getMessage().equals("Exception when querying table (MockTable)"));
        verify(mockTable).query(any(QueryEnhancedRequest.class));
    }
}
