package com.github.jaycleverly.stock_info.config.beans;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.github.jaycleverly.stock_info.config.properties.AppCalculationsProperties;
import com.github.jaycleverly.stock_info.config.properties.AppLimitsProperties;

@Configuration
@EnableConfigurationProperties({
    AppLimitsProperties.class,
    AppCalculationsProperties.class
})
public class AppConfig {
}
