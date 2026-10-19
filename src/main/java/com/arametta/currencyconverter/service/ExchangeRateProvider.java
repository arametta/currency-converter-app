package com.arametta.currencyconverter.service;

import com.arametta.currencyconverter.client.SwopClient;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Caching boundary in front of SwopClient.
 *
 * Why is this a separate bean instead of a method on CurrencyConversionService?
 * Spring's @Cacheable is implemented via AOP proxies. Self-invocation —
 * e.g. CurrencyConversionService.convert() calling this.getEurRate() —
 * goes through the actual object reference, not the proxy, so the cache
 * advice never fires. Putting the @Cacheable method on a separate Spring bean
 * means CurrencyConversionService always reaches it through the proxy, and
 * the cache works as intended.
 *
 * Cache key is a single currency code (e.g. "USD"), not a pair. Since every
 * lookup is anchored on EUR, the same EUR-based rate is reusable across any
 * pair involving that currency. After USD→GBP populates "USD" and "GBP",
 * a subsequent USD→EUR or GBP→USD is satisfied entirely from cache.
 */
@Component
public class ExchangeRateProvider {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateProvider.class);

    private final SwopClient swopClient;

    public ExchangeRateProvider(SwopClient swopClient) {
        this.swopClient = swopClient;
    }

    @Cacheable(value = "exchangeRates", key = "#currencyCode")
    public BigDecimal getEurRate(String currencyCode) {
        log.debug("Cache miss — fetching EUR/{} from swop.cx", currencyCode);
        return swopClient.getEurRate(currencyCode);
    }
}
