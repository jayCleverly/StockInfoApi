package com.github.jaycleverly.stock_info.service;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jaycleverly.stock_info.dto.DailyStockMetrics;
import com.github.jaycleverly.stock_info.exception.MetricFormatterException;

/**
 * Class to produce JSON response for stock analysis service
 */
public class MetricFormatterService {
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static JsonNodeFactory factory = JsonNodeFactory.instance;
    
    /**
     * Formats a list of metrics to a JSON string
     * 
     * @param metrics the metrics to format
     * @return a JSON string
     * @throws MetricFormatterException if the metrics cannot be written as JSON
     */
    public static String convertMetricsToJson(List<DailyStockMetrics> metrics) throws MetricFormatterException {
        ObjectNode root = factory.objectNode();

        // Meta Data
        DailyStockMetrics latest = metrics.getFirst();
        ObjectNode metaData = factory.objectNode();
        metaData.put("1. Information", "Daily Time Series with custom metrics");
        metaData.put("2. Symbol", latest.getSymbol());
        metaData.put("3. Last Refreshed", latest.getDate().toString());
        metaData.put("4. Time Zone", "Europe/London");
        root.set("Meta Data", metaData);

        // Time Series (Daily)
        ObjectNode timeSeries = factory.objectNode();
        for (DailyStockMetrics metric : metrics) {
            ObjectNode daily = factory.objectNode();
            daily.put("1. close", doubleToString(metric.getClose()));
            daily.put("2. previousCloseChange", doubleToString(metric.getPreviousCloseChange()));
            daily.put("3. movingAverage(30d)", doubleToString(metric.getMovingAverage()));
            daily.put("4. volatility(7d)", doubleToString(metric.getVolatility()));
            daily.put("5. momentum(14d)", doubleToString(metric.getMomentum()));

            timeSeries.set(metric.getDate().toString(), daily);
        }
        root.set("Time Series (Daily)", timeSeries);

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch(JsonProcessingException exception) {
            throw new MetricFormatterException("Exception when converting metrics to JSON!", exception);
        }

    }

    private static String doubleToString(Double value) {
        return (value != null) ? String.format("%.2f", value) : null;
    }
}
