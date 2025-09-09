package com.github.jaycleverly.stock_info.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.external-api")
public record AppApiProperties(
    String url,
    String token
) {}
