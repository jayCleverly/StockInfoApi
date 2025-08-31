package com.github.jaycleverly.stock_info.client;

import java.util.Map;

import com.github.jaycleverly.stock_info.exception.DynamoClientException;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

/*
 * Client for dynamo db operations
 */
public class DynamoClient {
    private final DynamoDbClient client;

    /**
     * Creates a new client for interacting with dynamo db
     * 
     * @param instance the aws sdk dynamo instance to use
     */
    public DynamoClient(DynamoDbClient instance) {
        this.client = instance;
    }

    /**
     * Puts an item into a table
     * 
     * @param tableName the name of the table to look in
     * @param item the item to put into the table
     */
    public void putItem(String tableName, Map<String, AttributeValue> item) {
        try {
            PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();
            client.putItem(request);

        } catch (Exception e) {
            throw new DynamoClientException(
                String.format("Exception when putting an item into table (%s)", tableName), e);
        }
    }

    /**
     * Gets an item from a table by key (partition + sort)
     *
     * @param tableName the name of the table to look in
     * @param key the partition + sort key pairings to search with
     * @return the found item
     */
    public Map<String, AttributeValue> getItem(String tableName, Map<String, AttributeValue> key) {
        try {
            GetItemRequest request = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .build();
            return client.getItem(request).item();

        } catch (Exception e) {
            throw new DynamoClientException(
                String.format("Exception when getting an item from table (%s)", tableName), e);
        }
    }

    /**
     * Queries a table with a custom expression
     *
     * @param tableName the name of the table to look in
     * @param keyConditionExpression the condition that determines items to be read
     * @param expressionValues the values that are subsituted in the expression
     * @return the matching items the query has found
     */
    public QueryResponse query(String tableName, String keyConditionExpression, Map<String, AttributeValue> expressionValues) {
        try {
            QueryRequest request = QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression(keyConditionExpression)
                    .expressionAttributeValues(expressionValues)
                    .build();
            return client.query(request);

        } catch (Exception e) {
            throw new DynamoClientException(
                String.format("Exception when querying table (%s)", tableName), e);
        }
    }
}
