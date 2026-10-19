package com.arametta.currencyconverter.service;

import com.arametta.currencyconverter.model.ConversionRequest;
import com.arametta.currencyconverter.model.ConversionResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Currency;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Handles a single conversion request. Normalises currency codes,
 * short-circuits same-currency pairs, and applies the source→target
 * rate from swop.cx. All caching happens in ExchangeRateProvider.
 */
@Service
public class CurrencyConversionService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyConversionService.class);

    // Calling a separate bean (rather than a method on `this`) is required for
    // @Cacheable to fire — see ExchangeRateProvider for the explanation.
    private final ExchangeRateProvider rateProvider;

    public CurrencyConversionService(ExchangeRateProvider rateProvider) {
        this.rateProvider = rateProvider;
    }

    public ConversionResponse convert(ConversionRequest request) {
        // Lowercase codes are accepted to be lenient with clients: "usd" is
        // unambiguous, so we normalise it instead of rejecting it.
        String source = request.sourceCurrency().toUpperCase(Locale.ROOT);
        String target = request.targetCurrency().toUpperCase(Locale.ROOT);

        // Validate both codes before touching swop.cx — 422 is cheaper than an API call.
        resolveCurrency(source);
        Currency targetIso = resolveCurrency(target);

        BigDecimal amount = request.amount();

        // Same-currency short-circuit. Rate is implicitly 1; no upstream call.
        if (source.equals(target)) {
            log.info("Same-currency conversion {} {} — short-circuit", amount, source);
            BigDecimal converted = scale(amount, targetIso);
            return new ConversionResponse(
                    amount, source, target,
                    converted,
                    format(converted, targetIso),
                    BigDecimal.ONE);
        }

        BigDecimal rate = rateProvider.getExchangeRate(source, target);

        BigDecimal converted = scale(amount.multiply(rate), targetIso);
        String formatted = format(converted, targetIso);

        log.info("Converted {} {} → {} {} (rate {})", amount, source, converted, target, rate);

        return new ConversionResponse(amount, source, target, converted, formatted, rate);
    }

    private Currency resolveCurrency(String code) {
        try {
            return Currency.getInstance(code);
        } catch (IllegalArgumentException e) {
            throw new UnknownCurrencyException("Unknown currency code: " + code, e);
        }
    }

    /**
     * Rounds to the target currency's default fraction digits. Avoids hard-
     * coding "2 decimals" — JPY has 0, KWD has 3, BHD has 3, etc.
     */
    private BigDecimal scale(BigDecimal value, Currency currency) {
        int digits = currency.getDefaultFractionDigits();
        if (digits < 0) {
            digits = 2; // some pseudo-currencies report -1; safe default
        }
        return value.setScale(digits, RoundingMode.HALF_UP);
    }

    /**
     * NumberFormat-based locale-aware formatting.
     *
     * NumberFormat is NOT thread-safe → create a fresh instance per call.
     * Premature optimisation (ThreadLocal cache, etc.) is not worth the risk
     * at this traffic level.
     */
    private String format(BigDecimal value, Currency currency) {
        Locale locale = resolveLocale(currency);
        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
        // The matched locale's default currency may differ (e.g. for a locale
        // that just happens to use this currency); force the right one so the
        // symbol and fraction digits are always correct.
        formatter.setCurrency(currency);
        return formatter.format(value);
    }

    /**
     * Find a Locale whose default Currency matches the target. ISO 4217 codes
     * don't map 1:1 to Locales, so we scan available locales and pick the first
     * match. Fallback to Locale.ROOT (which produces a neutral ISO format like
     * "USD 12.34") if no locale claims that currency — rare but possible for
     * niche codes.
     */
    private Locale resolveLocale(Currency currency) {
        return Arrays.stream(Locale.getAvailableLocales())
                .filter(l -> !l.getCountry().isEmpty())
                .filter(l -> {
                    try {
                        return currency.equals(Currency.getInstance(l));
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                })
                .findFirst()
                .orElse(Locale.ROOT);
    }
}
