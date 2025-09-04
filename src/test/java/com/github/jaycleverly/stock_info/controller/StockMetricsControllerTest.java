package com.github.jaycleverly.stock_info.controller;

import static org.mockito.Mockito.mockStatic;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import com.github.jaycleverly.stock_info.dto.DailyStockMetrics;
import com.github.jaycleverly.stock_info.exception.StockAnalysisException;
import com.github.jaycleverly.stock_info.service.StockAnalysisService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StockMetricsController.class)
public class StockMetricsControllerTest {
    private static final String MOCK_INPUT_SYMBOL = "MOCK"; 
    private static final int NUM_RECORDS_TO_RETURN = 10;

    private static MockedStatic<StockAnalysisService> stockAnalysisMock;
    private static List<DailyStockMetrics> mockMetricResponse = new ArrayList<>();

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        stockAnalysisMock = mockStatic(StockAnalysisService.class);

        // Generate data for stock metrics
        LocalDate startDate = LocalDate.now().minusDays(NUM_RECORDS_TO_RETURN);
        for (int i = 0; i < NUM_RECORDS_TO_RETURN; i++) {
            mockMetricResponse.add(new DailyStockMetrics(MOCK_INPUT_SYMBOL, startDate.plusDays(i), 100.0, null, null, null, null));
        }
    }

    @AfterEach
    void cleanup() {
        stockAnalysisMock.close();
    }

    @Test
    void shouldReturn200StatusCode() throws Exception {
        stockAnalysisMock.when(() -> StockAnalysisService.produceAnalysis(MOCK_INPUT_SYMBOL, null, null))
            .thenReturn(mockMetricResponse);

        mockMvc.perform(get("/stocks/" + MOCK_INPUT_SYMBOL))
            .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400StatusCode() throws Exception {
        stockAnalysisMock.when(() -> StockAnalysisService.produceAnalysis(MOCK_INPUT_SYMBOL, "INVALID_DATE", null))
            .thenThrow(new IllegalArgumentException());
        
        mockMvc.perform(get("/stocks/" + MOCK_INPUT_SYMBOL)
            .param("analysisStartDate", "INVALID_DATE"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn500StatusCode() throws Exception {
        stockAnalysisMock.when(() -> StockAnalysisService.produceAnalysis(MOCK_INPUT_SYMBOL, null, null))
            .thenThrow(new StockAnalysisException("MESSAGE", new Exception()));
        
        mockMvc.perform(get("/stocks/" + MOCK_INPUT_SYMBOL))
            .andExpect(status().isInternalServerError());
    }
}
