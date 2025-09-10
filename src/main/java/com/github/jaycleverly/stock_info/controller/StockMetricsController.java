package com.github.jaycleverly.stock_info.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.jaycleverly.stock_info.service.StockAnalysisService;

/**
 * Class to control the endpoints of the application
 */
@RestController
public class StockMetricsController {
    private StockAnalysisService stockAnalysisService;

    /**
     * Creates a new rest controller for the application
     * 
     * @param stockAnalysisService the service to provide an analysis on an inputted stock
     */
    public StockMetricsController(StockAnalysisService stockAnalysisService) {
        this.stockAnalysisService = stockAnalysisService;
    }

    /**
     * Returns metrics for the supplied stock
     * 
     * @param symbol the stock to look at
     * @param outputSize the output size of the response (compact / full)
     * @return a json formatted response containing stock metrics
     */
    @GetMapping("stocks/{symbol}")
    public ResponseEntity<String> getStockMetrics(@PathVariable String symbol,
                                                  @RequestParam(required = false, defaultValue = "compact") String outputSize) {
        return ResponseEntity.ok(
            stockAnalysisService.produceAnalysis(symbol.toUpperCase(), outputSize.equalsIgnoreCase("full")));
    }
}
