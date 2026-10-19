package com.arametta.currencyconverter.client;

import com.arametta.currencyconverter.model.SwopCurrencyResponse;
import com.arametta.currencyconverter.model.SwopRateResponse;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * HTTP adapter for swop.cx. Always fetches EUR-based rates —
 * the free tier doesn't support other base currencies.
 * Translates HTTP failures into domain exceptions so callers
 * don't need to know about RestClient.
 */
@Component
public class SwopClient {

    private static final Logger log = LoggerFactory.getLogger(SwopClient.class);
    private static final String EUR = "EUR";

    // Base URL, auth header and timeouts are configured in SwopClientConfig.
    private final RestClient restClient;

    public SwopClient(RestClient swopRestClient) {
        this.restClient = swopRestClient;
    }

    /**
     * Fetches the EUR → currencyCode exchange rate from swop.cx.
     *
     * Returns BigDecimal.ONE for "EUR" without making any HTTP call —
     * the EUR/EUR rate is always 1 by definition and skipping the call also
     * saves an API-quota credit.
     */
    public BigDecimal getEurRate(String currencyCode) {
        if (EUR.equals(currencyCode)) {
            return BigDecimal.ONE;
        }

        String target = "EUR/" + currencyCode;
        // URI variables are encoded, so the code can't alter the upstream path.
        SwopRateResponse body = call(target, () -> restClient.get()
                .uri("/rest/rates/{base}/{quote}", EUR, currencyCode)
                .retrieve()
                // 404 means swop.cx has no rate for this code: bad input (422),
                // not an upstream failure (503).
                .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (request, response) -> {
                    log.warn("swop.cx has no rate for {}: {}", target, response.getStatusCode());
                    throw new UnknownCurrencyException(
                            "Currency not supported by exchange rate provider: " + currencyCode);
                })
                .body(SwopRateResponse.class));

        if (body == null || body.quote() == null) {
            log.error("swop.cx returned empty body for {}", target);
            throw new ExchangeRateUnavailableException(
                    "Exchange rate provider returned an empty response");
        }
        return body.quote();
    }

    public List<SwopCurrencyResponse> getCurrencies() {
        String target = "/rest/currencies";
        // Array deserialisation is simpler than ParameterizedTypeReference
        // for a JSON top-level array. Arrays.asList wraps without copying.
        SwopCurrencyResponse[] body = call(target, () -> restClient.get()
                .uri(target)
                .retrieve()
                .body(SwopCurrencyResponse[].class));

        if (body == null) {
            log.error("swop.cx returned empty body for {}", target);
            throw new ExchangeRateUnavailableException(
                    "Exchange rate provider returned an empty currency list");
        }
        return Arrays.asList(body);
    }

    /**
     * Runs one swop.cx request and maps transport and HTTP failures to
     * ExchangeRateUnavailableException, so both endpoints share one mapping.
     */
    private <T> T call(String target, Supplier<T> request) {
        try {
            return request.get();

        } catch (HttpClientErrorException e) {
            // Bad API key, rate-limited, malformed request. From the caller's
            // perspective the upstream is "not currently usable".
            log.error("swop.cx returned client error {} for {}", e.getStatusCode(), target);
            throw new ExchangeRateUnavailableException(
                    "Exchange rate provider rejected the request: " + e.getStatusCode(), e);

        } catch (HttpServerErrorException e) {
            log.error("swop.cx returned server error {} for {}", e.getStatusCode(), target);
            throw new ExchangeRateUnavailableException(
                    "Exchange rate provider is currently unavailable", e);

        } catch (ResourceAccessException e) {
            // Connect failure, read timeout, DNS, etc.
            log.error("swop.cx unreachable for {}: {}", target, e.getMessage());
            throw new ExchangeRateUnavailableException(
                    "Exchange rate provider is unreachable", e);
        }
    }
}
