package com.github.jaycleverly.stock_info.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.jaycleverly.stock_info.exception.DynamoClientException;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

public class DynamoClientTest {
    private final String mockTableName = "MockTable";

    private DynamoClient dynamoClient;
    private DynamoDbClient mockClient;

    @BeforeEach
    void setUp() {
        mockClient = mock(DynamoDbClient.class);
        dynamoClient = new DynamoClient(mockClient);
    }
    
    @Test
    void shouldSuccessfullyAddNewItem() {
        Map<String, AttributeValue> mockItem = new HashMap<>();
        mockItem.put("id", AttributeValue.builder().s("1").build());
        when(mockClient.putItem(any(PutItemRequest.class))).thenReturn(PutItemResponse.builder().build());

        assertDoesNotThrow(() -> dynamoClient.putItem(mockTableName, mockItem));
        verify(mockClient).putItem(any(PutItemRequest.class));
    }

    @Test
    void shouldThrowErrorOnPutFailure() {
        Map<String, AttributeValue> mockItem = new HashMap<>();
        mockItem.put("id", AttributeValue.builder().s("2").build());
        when(mockClient.putItem(any(PutItemRequest.class))).thenThrow(RuntimeException.class);

        Exception exception = assertThrows(DynamoClientException.class, () -> dynamoClient.putItem(mockTableName, mockItem));
        assertTrue(exception.getMessage().equals("Exception when putting an item into table (MockTable)"));
        verify(mockClient).putItem(any(PutItemRequest.class));
    }

    @Test
    void shouldSuccessfullyGetItem() {
        Map<String, AttributeValue> mockKey = new HashMap<>();
        mockKey.put("id", AttributeValue.builder().s("3").build());

        Map<String, AttributeValue> mockReturnedItem = new HashMap<>();
        mockKey.put("data", AttributeValue.builder().s("3").build());
        when(mockClient.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().item(mockReturnedItem).build());

        Map<String, AttributeValue> result = dynamoClient.getItem(mockTableName, mockKey);
        assertEquals(mockReturnedItem, result);
        verify(mockClient).getItem(any(GetItemRequest.class));
    }

    @Test
    void shouldThrowErrorOnGetFailure() {
        Map<String, AttributeValue> mockKey = new HashMap<>();
        mockKey.put("id", AttributeValue.builder().s("4").build());
        when(mockClient.getItem(any(GetItemRequest.class))).thenThrow(RuntimeException.class);

        Exception exception = assertThrows(DynamoClientException.class, () -> dynamoClient.getItem(mockTableName, mockKey));
        assertTrue(exception.getMessage().equals("Exception when getting an item from table (MockTable)"));
        verify(mockClient).getItem(any(GetItemRequest.class));
    }

    @Test
    void shouldSuccessfullyQueryTable() {
        Map<String, AttributeValue> mockExpressionValues = new HashMap<>();
        mockExpressionValues.put(":id", AttributeValue.builder().s("5").build());

        QueryResponse response = QueryResponse.builder().build();
        when(mockClient.query(any(QueryRequest.class))).thenReturn(response);

        QueryResponse result = dynamoClient.query(mockTableName, "id = :id", mockExpressionValues);
        assertEquals(response, result);
        verify(mockClient).query(any(QueryRequest.class));
    }

    @Test
    void shouldThrowErrorOnQueryFailure() {
        Map<String, AttributeValue> mockExpressionValues = new HashMap<>();
        mockExpressionValues.put("id", AttributeValue.builder().s("6").build());
        when(mockClient.query(any(QueryRequest.class))).thenThrow(RuntimeException.class);

        Exception exception = assertThrows(DynamoClientException.class, () -> 
            dynamoClient.query(mockTableName, "id = :id", mockExpressionValues));
        assertTrue(exception.getMessage().equals("Exception when querying table (MockTable)"));
        verify(mockClient).query(any(QueryRequest.class));
    }
}
