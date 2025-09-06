package com.github.jaycleverly.stock_info.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.calculations")
public record AppCalculationsProperties(
    int movingAveragePeriod,
    int volatilityPeriod,
    int momentumPeriod
) {}
