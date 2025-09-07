package com.github.jaycleverly.stock_info.serializer;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jaycleverly.stock_info.exception.SerializerException;
import com.github.jaycleverly.stock_info.model.DailyStockMetrics;

/**
 * Class to provide methods to serialize metric records to a json response
 */
public class StockMetricsSerializer {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;
    
    /**
     * Converts a list of metrics to a JSON response string
     * 
     * @param metrics the metrics to serialize
     * @return a JSON string
     * @throws MetricFormatterException if the metrics cannot be written as JSON
     */
    public static String serialize(List<DailyStockMetrics> metrics) throws SerializerException {
        ObjectNode root = FACTORY.objectNode();

        // Meta Data
        DailyStockMetrics latest = metrics.getFirst();
        ObjectNode metaData = FACTORY.objectNode();
        metaData.put("1. Information", "Daily Time Series with custom metrics");
        metaData.put("2. Symbol", latest.getSymbol());
        metaData.put("3. Last Refreshed", latest.getDate().toString());
        metaData.put("4. Time Zone", "Europe/London");
        root.set("Meta Data", metaData);

        // Time Series (Daily)
        ObjectNode timeSeries = FACTORY.objectNode();
        for (DailyStockMetrics metric : metrics) {
            ObjectNode daily = FACTORY.objectNode();
            daily.put("1. close", doubleToString(metric.getClose()));
            daily.put("2. previousCloseChange", doubleToString(metric.getPreviousCloseChange()));
            daily.put("3. movingAverage(30d)", doubleToString(metric.getMovingAverage()));
            daily.put("4. volatility(7d%)", doubleToString(metric.getVolatility()));
            daily.put("5. momentum(14d%)", doubleToString(metric.getMomentum()));

            timeSeries.set(metric.getDate().toString(), daily);
        }
        root.set("Time Series (Daily)", timeSeries);

        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch(JsonProcessingException exception) {
            throw new SerializerException("Exception when converting metrics to JSON!", exception);
        }
    }

    private static String doubleToString(Double value) {
        return (value != null) ? String.format("%.2f", value) : null;
    }
}
