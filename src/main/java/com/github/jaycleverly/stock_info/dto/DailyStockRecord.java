package com.github.jaycleverly.stock_info.dto;

import java.time.LocalDate;

public class DailyStockRecord {
    private final LocalDate date;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final double adjustedClose;
    private final long volume;

    public DailyStockRecord(LocalDate date, double open, double high, double low, double close, double adjustedClose, long volume) {
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.adjustedClose = adjustedClose;
        this.volume = volume;
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

    public double getAdjustedClose() { 
        return adjustedClose; 
    }

    public long getVolume() { 
        return volume; 
    }
}
