package com.arametta.currencyconverter.client;

import com.arametta.currencyconverter.model.SwopRateResponse;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * HTTP adapter for the swop.cx single-pair rate endpoint.
 * Translates HTTP failures into domain exceptions so callers
 * don't need to know about RestTemplate.
 */
@Component
public class SwopClient {

    private static final Logger log = LoggerFactory.getLogger(SwopClient.class);

    // All injected dependencies are final + constructor-injected.
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;

    public SwopClient(
            RestTemplate restTemplate,
            @Value("${swop.base-url}") String baseUrl,
            @Value("${swop.api-key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    /**
     * Fetches the baseCurrency → quoteCurrency exchange rate from swop.cx.
     */
    public BigDecimal getExchangeRate(String baseCurrency, String quoteCurrency) {
        // URL-encodes path segments and prevents injection into the upstream URL.
        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .pathSegment("rest", "rates", baseCurrency, quoteCurrency)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        // swop.cx auth scheme is literal "ApiKey <token>", not "Bearer".
        headers.set(HttpHeaders.AUTHORIZATION, "ApiKey " + apiKey);
        headers.set(HttpHeaders.ACCEPT, "application/json");

        try {
            ResponseEntity<SwopRateResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    SwopRateResponse.class);

            SwopRateResponse body = response.getBody();
            if (body == null || body.quote() == null) {
                log.error("swop.cx returned empty body for {}/{}", baseCurrency, quoteCurrency);
                throw new ExchangeRateUnavailableException(
                        "Exchange rate provider returned an empty response");
            }
            return body.quote();

        } catch (HttpClientErrorException e) {
            // 4xx — bad API key, rate-limited, malformed request. From the
            // caller's perspective the upstream is "not currently usable".
            log.error("swop.cx returned client error {} for {}/{}",
                    e.getStatusCode(), baseCurrency, quoteCurrency);
            throw new ExchangeRateUnavailableException(
                    "Exchange rate provider rejected the request: " + e.getStatusCode(), e);

        } catch (HttpServerErrorException e) {
            log.error("swop.cx returned server error {} for {}/{}",
                    e.getStatusCode(), baseCurrency, quoteCurrency);
            throw new ExchangeRateUnavailableException(
                    "Exchange rate provider is currently unavailable", e);

        } catch (ResourceAccessException e) {
            // Connect failure, read timeout, DNS, etc.
            log.error("swop.cx unreachable for {}/{}: {}",
                    baseCurrency, quoteCurrency, e.getMessage());
            throw new ExchangeRateUnavailableException(
                    "Exchange rate provider is unreachable", e);
        }
    }
}
