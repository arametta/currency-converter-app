package com.arametta.currencyconverter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.arametta.currencyconverter.client.ExchangeRateUnavailableException;
import com.arametta.currencyconverter.model.CurrencyInfo;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupportedCurrenciesTest {

    @Mock CurrencyService currencyService;
    @InjectMocks SupportedCurrencies supportedCurrencies;

    @Test
    void codeInActiveList_isSupported() {
        when(currencyService.getAvailableCurrencies()).thenReturn(List.of(
                new CurrencyInfo("EUR", "Euro"), new CurrencyInfo("USD", "United States dollar")));

        assertThat(supportedCurrencies.isSupported("USD")).isTrue();
    }

    @Test
    void codeMissingFromList_isNotSupported() {
        when(currencyService.getAvailableCurrencies()).thenReturn(List.of(
                new CurrencyInfo("EUR", "Euro")));

        assertThat(supportedCurrencies.isSupported("XYZ")).isFalse();
    }

    @Test
    void listUnavailable_propagates() {
        when(currencyService.getAvailableCurrencies())
                .thenThrow(new ExchangeRateUnavailableException("Exchange rate provider is unreachable"));

        assertThatThrownBy(() -> supportedCurrencies.isSupported("USD"))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }
}
