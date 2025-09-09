package com.github.jaycleverly.stock_info.model;

import java.time.LocalDate;

public class DailyStockRecord {
    private final String symbol;
    private final LocalDate date;
    private final double open;
    private final double high;
    private final double low;
    private final double close;

    public DailyStockRecord(String symbol, 
                            LocalDate date, 
                            double open, 
                            double high, 
                            double low, 
                            double close) {
        this.symbol = symbol;
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
    }

    public String getSymbol() {
        return symbol;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getOpen() { 
        return open; 
    }

    public double getHigh() { 
        return high; 
    }

    public double getLow() { 
        return low; 
    }

    public double getClose() { 
        return close; 
    }
}
