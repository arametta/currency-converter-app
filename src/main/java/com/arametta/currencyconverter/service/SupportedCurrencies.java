package com.arametta.currencyconverter.service;

import org.springframework.stereotype.Component;

/**
 * Answers "does swop.cx support this code?" from the active currency list.
 * Backed by CurrencyService's 24h cache, so in normal use it costs no extra
 * upstream calls. If the list can't be loaded, the ExchangeRateUnavailableException
 * propagates (503) rather than the check being skipped.
 */
@Component
public class SupportedCurrencies {

    private final CurrencyService currencyService;

    public SupportedCurrencies(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    public boolean isSupported(String code) {
        return currencyService.getAvailableCurrencies().stream()
                .anyMatch(currency -> currency.code().equals(code));
    }
}
