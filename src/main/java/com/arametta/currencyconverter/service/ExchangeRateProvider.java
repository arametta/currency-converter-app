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
 * e.g. CurrencyConversionService.convert() calling this.getExchangeRate() —
 * goes through the actual object reference, not the proxy, so the cache
 * advice never fires. Putting the @Cacheable method on a separate Spring bean
 * means CurrencyConversionService always reaches it through the proxy, and
 * the cache works as intended.
 */
@Component
public class ExchangeRateProvider {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateProvider.class);

    private final SwopClient swopClient;

    public ExchangeRateProvider(SwopClient swopClient) {
        this.swopClient = swopClient;
    }

    @Cacheable(value = "exchangeRates", key = "#baseCurrency + '_' + #quoteCurrency")
    public BigDecimal getExchangeRate(String baseCurrency, String quoteCurrency) {
        log.debug("Cache miss — fetching {}/{} from swop.cx", baseCurrency, quoteCurrency);
        return swopClient.getExchangeRate(baseCurrency, quoteCurrency);
    }
}
