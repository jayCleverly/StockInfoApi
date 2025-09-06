package com.github.jaycleverly.stock_info.config.properties;

import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.dynamodb")
public record DynamoDbProperties(
        String region,
        Optional<String> endpoint
) {}
