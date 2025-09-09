package com.github.jaycleverly.stock_info.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.jaycleverly.stock_info.config.properties.AppApiProperties;
import com.github.jaycleverly.stock_info.exception.TimeSeriesApiException;
import com.github.tomakehurst.wiremock.WireMockServer;

public class TimeSeriesApiClientTest {
    private static final String MOCK_API_URL = "http://localhost:%d/mock_url&symbol=%s&apikey=%s";
    private static final String MOCK_API_TOKEN = "mockToken";
    private static final String MOCK_STOCK = "MOCK";

    private WireMockServer wireMockServer;
    private AppApiProperties apiProperties;
    private TimeSeriesApiClient timeSeriesApiClient;

    @BeforeEach
    void setup() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());

        apiProperties = new AppApiProperties(
            String.format(MOCK_API_URL, wireMockServer.port(), "%s", "%s"),
            MOCK_API_TOKEN
        );
        timeSeriesApiClient = new TimeSeriesApiClient(apiProperties);
    }

    @AfterEach
    void cleanup() {
        wireMockServer.stop();
    }
    
    @Test
    void shouldReturnValidJson() {
        String expected = String.format("{\"symbol\":\"%s\"}", MOCK_STOCK);
        stubFor(get(urlPathEqualTo(String.format("/mock_url&symbol=%s&apikey=%s", MOCK_STOCK, MOCK_API_TOKEN)))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(expected)));

        assertEquals(expected, timeSeriesApiClient.getDailyTimeSeries(MOCK_STOCK));
    }
    
    @Test
    void shouldThrowExceptionOnUnauthorised() {
        stubFor(get(urlPathEqualTo(String.format("/mock_url&symbol=%s&apikey=%s", MOCK_STOCK, MOCK_API_TOKEN)))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"Error Message\": \"Invalid API call.\"}")));

        apiProperties = new AppApiProperties(
            String.format(MOCK_API_URL, wireMockServer.port(), "%s", "%s"),
            ""
        );
        timeSeriesApiClient = new TimeSeriesApiClient(apiProperties);

        TimeSeriesApiException exception = assertThrows(TimeSeriesApiException.class, () -> timeSeriesApiClient.getDailyTimeSeries(MOCK_STOCK));
        assertEquals(String.format("Encountered a 4xx error when getting records for MOCK!", MOCK_STOCK), exception.getMessage());
        assertEquals(401, exception.getStatusCode());
    }

    @Test
    void shouldThrowExceptionOnNotFound() {
        stubFor(get(urlPathEqualTo(String.format("/mock_url&symbol=%s&apikey=%s", MOCK_STOCK, MOCK_API_TOKEN)))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"Error Message\": \"Invalid API call.\"}")));

        TimeSeriesApiException exception = assertThrows(TimeSeriesApiException.class, () -> timeSeriesApiClient.getDailyTimeSeries(MOCK_STOCK));
        assertEquals(String.format("Encountered a 4xx error when getting records for MOCK!", MOCK_STOCK), exception.getMessage());
        assertEquals(404, exception.getStatusCode());
    }

    @Test
    void shouldThrowExceptionOnRateLimit() {
        stubFor(get(urlPathEqualTo(String.format("/mock_url&symbol=%s&apikey=%s", MOCK_STOCK, MOCK_API_TOKEN)))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"Information\": \"Too many requests.\"}")));

        TimeSeriesApiException exception = assertThrows(TimeSeriesApiException.class, () -> timeSeriesApiClient.getDailyTimeSeries(MOCK_STOCK));
        assertEquals(String.format("Encountered a 4xx error when getting records for MOCK!", MOCK_STOCK), exception.getMessage());
        assertEquals(429, exception.getStatusCode());
    }

    @Test
    void shouldThrowExceptionOnConnectionError() {
        wireMockServer.stop(); // Simulate connectionn issue

        TimeSeriesApiException exception = assertThrows(TimeSeriesApiException.class, () -> timeSeriesApiClient.getDailyTimeSeries(MOCK_STOCK));
        assertEquals(String.format("Error when getting API response for symbol %s!", MOCK_STOCK), exception.getMessage());
    }
}
