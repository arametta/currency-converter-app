package com.arametta.currencyconverter.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arametta.currencyconverter.config.SwopClientConfig;
import com.arametta.currencyconverter.config.SwopProperties;
import com.arametta.currencyconverter.model.SwopCurrencyResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Unit tests for the swop.cx HTTP adapter.
 *
 * The client is built through SwopClientConfig and talks real HTTP to
 * WireMock, so base URL, headers and timeouts are the production ones,
 * not a hand-built copy.
 */
class SwopClientTest {

    // Short read timeout so the timeout scenario stays fast.
    private static final Duration READ_TIMEOUT = Duration.ofMillis(300);

    private static WireMockServer wireMock;

    private SwopClient client;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        SwopProperties properties = new SwopProperties(
                wireMock.baseUrl(), "test-key", Duration.ofSeconds(1), READ_TIMEOUT);
        RestClient restClient = new SwopClientConfig().swopRestClient(RestClient.builder(), properties);
        client = new SwopClient(restClient);
    }

    // ---- EUR short-circuit ------------------------------------------------

    @Test
    void getEurRateForEur_returnsOneWithoutHttpCall() {
        BigDecimal rate = client.getEurRate("EUR");

        assertThat(rate).isEqualByComparingTo(BigDecimal.ONE);
        wireMock.verify(exactly(0), anyRequestedFor(anyUrl()));
    }

    // ---- success ----------------------------------------------------------

    @Test
    void successfulResponse_isDeserialisedAndRateReturned() {
        stubJson("/rest/rates/EUR/USD", """
                { "base_currency": "EUR", "quote_currency": "USD", "quote": 1.08, "date": "2026-05-23" }
                """);

        BigDecimal rate = client.getEurRate("USD");

        assertThat(rate).isEqualByComparingTo("1.08");
        wireMock.verify(exactly(1), getRequestedFor(urlPathEqualTo("/rest/rates/EUR/USD"))
                .withHeader("Authorization", equalTo("ApiKey test-key"))
                .withHeader("Accept", equalTo("application/json")));
    }

    // ---- failure translation ---------------------------------------------

    @Test
    void notFound_throwsUnknownCurrencyException() {
        // 404 specifically means "swop.cx doesn't support this currency".
        // Caller sees 422, not 503 — the upstream is fine, the input isn't.
        stubStatus("/rest/rates/EUR/XTS", 404);

        assertThatThrownBy(() -> client.getEurRate("XTS"))
                .isInstanceOf(UnknownCurrencyException.class)
                .hasMessageContaining("XTS");
    }

    @Test
    void otherClientError_throwsExchangeRateUnavailable() {
        // 401 (bad API key), 429 (rate-limited), etc. all funnel here.
        stubStatus("/rest/rates/EUR/USD", 401);

        assertThatThrownBy(() -> client.getEurRate("USD"))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    @Test
    void serverError_throwsExchangeRateUnavailable() {
        stubStatus("/rest/rates/EUR/USD", 500);

        assertThatThrownBy(() -> client.getEurRate("USD"))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    @Test
    void readTimeout_throwsExchangeRateUnavailable() {
        wireMock.stubFor(get(urlPathEqualTo("/rest/rates/EUR/USD"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay((int) READ_TIMEOUT.toMillis() * 3)));

        assertThatThrownBy(() -> client.getEurRate("USD"))
                .isInstanceOf(ExchangeRateUnavailableException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);
    }

    @Test
    void emptyResponseBody_throwsExchangeRateUnavailable() {
        // Defensive: never assume the body is non-null.
        stubJson("/rest/rates/EUR/USD", "");

        assertThatThrownBy(() -> client.getEurRate("USD"))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    // ---- getCurrencies ---------------------------------------------------

    @Test
    void getCurrencies_returnsDeserialisedListIncludingInactiveEntries() {
        // The client doesn't filter — that's CurrencyService's job. Verify
        // both active and inactive entries pass through unchanged.
        stubJson("/rest/currencies", """
                [
                  { "code": "USD", "name": "United States dollar", "active": true },
                  { "code": "XAF", "name": "CFA franc BEAC", "active": false },
                  { "code": "EUR", "name": "Euro", "active": true }
                ]
                """);

        List<SwopCurrencyResponse> result = client.getCurrencies();

        assertThat(result).extracting(SwopCurrencyResponse::code)
                .containsExactly("USD", "XAF", "EUR");
        wireMock.verify(exactly(1), getRequestedFor(urlPathEqualTo("/rest/currencies"))
                .withHeader("Authorization", equalTo("ApiKey test-key")));
    }

    @Test
    void getCurrencies_notFound_throwsExchangeRateUnavailable() {
        // Unlike a rate lookup, a 404 here is an upstream problem, not bad input.
        stubStatus("/rest/currencies", 404);

        assertThatThrownBy(() -> client.getCurrencies())
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    @Test
    void getCurrencies_serverError_throwsExchangeRateUnavailable() {
        stubStatus("/rest/currencies", 500);

        assertThatThrownBy(() -> client.getCurrencies())
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    @Test
    void getCurrencies_networkFailure_throwsExchangeRateUnavailable() {
        wireMock.stubFor(get(urlPathEqualTo("/rest/currencies"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(() -> client.getCurrencies())
                .isInstanceOf(ExchangeRateUnavailableException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);
    }

    @Test
    void getCurrencies_emptyBody_throwsExchangeRateUnavailable() {
        stubJson("/rest/currencies", "");

        assertThatThrownBy(() -> client.getCurrencies())
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    // ---- helpers ---------------------------------------------------------

    private void stubJson(String path, String body) {
        wireMock.stubFor(get(urlPathEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    private void stubStatus(String path, int status) {
        wireMock.stubFor(get(urlPathEqualTo(path)).willReturn(aResponse().withStatus(status)));
    }
}
