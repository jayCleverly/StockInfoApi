package com.github.jaycleverly.stock_info.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.limits")
public record AppLimitsProperties(
    int metricRecords,
    int apiRecords
) {}
