package com.github.jaycleverly.stock_info.config.beans;

import java.net.URI;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.jaycleverly.stock_info.config.properties.DynamoDbProperties;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

@Configuration
@EnableConfigurationProperties(DynamoDbProperties.class)
public class DynamoDbConfig {
    @Bean
    public DynamoDbClient dynamoDbClient(DynamoDbProperties properties) {
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
            .region(Region.of(properties.region()))
            .credentialsProvider(DefaultCredentialsProvider.builder().build());

        // For local testing purposes
        properties.endpoint()
            .filter(s -> !s.isBlank())
            .ifPresent(endpoint -> builder.endpointOverride(URI.create(endpoint)));

        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
            .dynamoDbClient(dynamoDbClient)
            .build();
    }
}
