package com.github.jaycleverly.stock_info.dto;

import java.time.LocalDate;

public class DailyStockAnalysis {
    private final LocalDate date;
    private final double close;
    private final Double previousCloseChange;
    private final Double movingAverage;
    private final Double volatility;
    private final Double momentum;
    
    public DailyStockAnalysis(LocalDate date, double close, Double previousCloseChange, Double movingAverage, Double volatility, Double momentum) {
        this.date = date;
        this.close = close;
        this.previousCloseChange = previousCloseChange;
        this.movingAverage = movingAverage;
        this.volatility = volatility;
        this.momentum = momentum;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getClose() { 
        return close; 
    }

    public Double getPreviousCloseChange() { 
        return previousCloseChange; 
    }

    public Double getMovingAverage() { 
        return movingAverage; 
    }

    public Double getVolatility() { 
        return volatility; 
    }

    public Double getMomentum() { 
        return momentum; 
    }
}
