package com.github.jaycleverly.stock_info.model;

import java.time.LocalDate;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class DailyStockMetrics {
    private String symbol;
    private LocalDate date;
    private Double close;
    private Double previousCloseChange;
    private Double movingAverage;
    private Double volatility;
    private Double momentum;

    public DailyStockMetrics() {}

    public DailyStockMetrics(String symbol,
                             LocalDate date,
                             Double close,
                             Double previousCloseChange,
                             Double movingAverage,
                             Double volatility,
                             Double momentum) {
        this.symbol = symbol;
        this.date = date;
        this.close = close;
        this.previousCloseChange = previousCloseChange;
        this.movingAverage = movingAverage;
        this.volatility = volatility;
        this.momentum = momentum;
    }
    
    @DynamoDbPartitionKey
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    @DynamoDbSortKey
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Double getClose() {
        return close;
    }

    public void setClose(Double close) {
        this.close = close;
    }

    public Double getPreviousCloseChange() {
        return previousCloseChange;
    }

    public void setPreviousCloseChange(Double previousCloseChange) {
        this.previousCloseChange = previousCloseChange;
    }

    public Double getMovingAverage() {
        return movingAverage;
    }

    public void setMovingAverage(Double movingAverage) {
        this.movingAverage = movingAverage;
    }

    public Double getVolatility() {
        return volatility;
    }

    public void setVolatility(Double volatility) {
        this.volatility = volatility;
    }

    public Double getMomentum() {
        return momentum;
    }

    public void setMomentum(Double momentum) {
        this.momentum = momentum;
    }
}