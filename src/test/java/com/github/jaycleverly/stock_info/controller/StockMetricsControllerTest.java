package com.github.jaycleverly.stock_info.controller;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.github.jaycleverly.stock_info.exception.StockAnalysisException;
import com.github.jaycleverly.stock_info.service.StockAnalysisService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StockMetricsController.class)
public class StockMetricsControllerTest {
    private static final String MOCK_INPUT_SYMBOL = "MOCK_SYMBOL"; 
    private static final String MOCK_JSON_RESPONSE = "MOCK_JSON_RESPONSE";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    StockAnalysisService stockAnalysisMock;

    @Test
    void shouldReturn2xxStatusCode() throws Exception {
        when(stockAnalysisMock.produceAnalysis(MOCK_INPUT_SYMBOL, null, null, false))
            .thenReturn(MOCK_JSON_RESPONSE);

        mockMvc.perform(get("/stocks/" + MOCK_INPUT_SYMBOL))
            .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400StatusCode() throws Exception {
        when(stockAnalysisMock.produceAnalysis(MOCK_INPUT_SYMBOL, "INVALID_DATE", null, false))
            .thenThrow(new IllegalArgumentException());
        
        mockMvc.perform(get("/stocks/" + MOCK_INPUT_SYMBOL)
            .param("analysisStartDate", "INVALID_DATE"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn500StatusCode() throws Exception {
        when(stockAnalysisMock.produceAnalysis(MOCK_INPUT_SYMBOL, null, null, false))
            .thenThrow(new StockAnalysisException(null, null));
        
        mockMvc.perform(get("/stocks/" + MOCK_INPUT_SYMBOL))
            .andExpect(status().isInternalServerError());
    }
}
