package com.github.jaycleverly.stock_info.client;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jaycleverly.stock_info.config.properties.AppApiProperties;
import com.github.jaycleverly.stock_info.exception.TimeSeriesApiException;

import software.amazon.awssdk.http.HttpStatusCode;

/**
 * Class to get records from an api returning daily time series stock data
 */
@Component
public class TimeSeriesApiClient {
    private static final RestTemplate REST_TEMPLATE = new RestTemplate();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String apiUrl;
    private final String apiToken;

    /**
     * Creates a new client that calls an api to get stock time series data
     * 
     * @param apiProperties the properties of the external api
     */
    public TimeSeriesApiClient(AppApiProperties apiProperties) {
        this.apiUrl = apiProperties.url();
        this.apiToken = apiProperties.token();
    }

    /**
     * Returns a json response containing daily time series data for a particular stock
     * 
     * @param symbol the stock to get a response for
     * @return json stock records
     * @throws TimeSeriesApiException if an error occurs while processing the api request
     */
    public String getDailyTimeSeries(String symbol) throws TimeSeriesApiException {
        try {
            // User must have entered a token into application properties
            if (apiToken.isBlank()) {
                throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, String.format("Invalid token!", symbol));
            }

            String response = REST_TEMPLATE.getForObject(String.format(apiUrl, symbol, apiToken), String.class);
            JsonNode jsonResponse = OBJECT_MAPPER.readTree(response);

            if (jsonResponse.has("Error Message")) {
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND, String.format("Symbol %s not found!", symbol));
            }
            if (jsonResponse.has("Information")) {
                throw new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, String.format("API rate limit hit! (Refreshes every day)", symbol));
            }
            return response;

        } catch (HttpClientErrorException exception) {
            throw new TimeSeriesApiException(String.format("Encountered a 4xx error when getting records for %s!", symbol), exception.getStatusCode().value(), exception);
        } catch (HttpServerErrorException exception) {
            throw new TimeSeriesApiException(String.format("Encountered a 5xx error when getting records for %s!", symbol), exception.getStatusCode().value(), exception);
        } catch (RestClientException | JsonProcessingException exception) {
            throw new TimeSeriesApiException(String.format("Error when getting API response for symbol %s!", symbol), HttpStatusCode.INTERNAL_SERVER_ERROR, exception);
        }
    }
}
