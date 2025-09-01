package com.github.jaycleverly.stock_info.client;

import java.util.List;

import com.github.jaycleverly.stock_info.exception.DynamoClientException;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

/*
 * Client for dynamo db operations
 */
public class DynamoClient {
    private final DynamoDbEnhancedClient client;

    /**
     * Creates a new client for interacting with dynamo db
     * 
     * @param instance the aws sdk dynamo instance to use
     */
    public DynamoClient(DynamoDbEnhancedClient instance) {
        this.client = instance;
    }

    /**
     * Puts an item into a table
     * 
     * @param tableName the name of the table to look in
     * @param item the item to put into the table
     * @param type the type of item to put into the table
     */
    public <T> void putItem(String tableName, T item, Class<T> type) {
        try {
            DynamoDbTable<T> table = client.table(tableName, TableSchema.fromBean(type));
            table.putItem(item);

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
     * @param type the type of object getting from the table
     * @return the found item
     */
    public <T> T getItem(String tableName, Key key, Class<T> type) {
        try {
            DynamoDbTable<T> table = client.table(tableName, TableSchema.fromBean(type));
            return table.getItem(key);

        } catch (Exception e) {
            throw new DynamoClientException(
                String.format("Exception when getting an item from table (%s)", tableName), e);
        }
    }

    /**
     * Queries a table with a custom expression
     *
     * @param tableName the name of the table to look in
     * @param condition the condition that determines items to be read
     * @param type the type of the values to be returned
     * @return the matching items the query has found
     */
    public <T> List<T> query(String tableName, QueryConditional condition, Class<T> type) {
        try {
            DynamoDbTable<T> table = client.table(tableName, TableSchema.fromBean(type));
            return table.query(QueryEnhancedRequest.builder()
                    .queryConditional(condition)
                    .build())
                    .items()
                    .stream()
                    .toList();

        } catch (Exception e) {
            throw new DynamoClientException(
                String.format("Exception when querying table (%s)", tableName), e);
        }
    }
}
