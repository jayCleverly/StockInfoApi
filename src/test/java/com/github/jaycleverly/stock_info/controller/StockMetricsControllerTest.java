package com.github.jaycleverly.stock_info.controller;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import com.github.jaycleverly.stock_info.exception.StockAnalysisException;
import com.github.jaycleverly.stock_info.service.StockAnalysisService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StockMetricsController.class)
public class StockMetricsControllerTest {
    private static final String MOCK_INPUT_SYMBOL = "MOCK_SYMBOL"; 
    private static final String MOCK_JSON_RESPONSE = "MOCK_JSON_RESPONSE";

    private static MockedStatic<StockAnalysisService> stockAnalysisMock;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        stockAnalysisMock = mockStatic(StockAnalysisService.class);
    }

    @AfterEach
    void cleanup() {
        stockAnalysisMock.close();
    }

    @Test
    void shouldReturn200StatusCode() throws Exception {
        stockAnalysisMock.when(() -> StockAnalysisService.produceAnalysis(MOCK_INPUT_SYMBOL, null, null))
            .thenReturn(MOCK_JSON_RESPONSE);

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
