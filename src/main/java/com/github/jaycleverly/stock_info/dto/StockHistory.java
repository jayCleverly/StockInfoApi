package com.github.jaycleverly.stock_info.dto;

import java.util.List;

public class StockHistory {
    private final String symbol;
    private final List<DailyStockRecord> records;

    public StockHistory(String symbol, List<DailyStockRecord> records) {
        this.symbol = symbol;
        this.records = records;
    }

    public String getSymbol() {
        return symbol;
    }

    public List<DailyStockRecord> getRecords() {
        return records;
    }
}
