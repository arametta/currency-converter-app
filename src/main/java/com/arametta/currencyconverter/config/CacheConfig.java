package com.arametta.currencyconverter.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Caffeine cache for exchange rates:
 *   exchangeRates — 5 min, max 500 entries. FX rates move; 5 min is
 *                   short enough to stay fresh, long enough to absorb bursts.
 *
 * recordStats() feeds the cache.hit.rate gauge.
 */
@Configuration
public class CacheConfig {

    public static final String EXCHANGE_RATES_CACHE = "exchangeRates";

    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        manager.registerCustomCache(EXCHANGE_RATES_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofMinutes(5))
                        .maximumSize(500)
                        .recordStats() // required for the cache.hit.rate gauge
                        .build());

        return manager;
    }
}
