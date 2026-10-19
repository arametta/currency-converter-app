package com.arametta.currencyconverter.service;

/**
 * Thrown when a currency code passes the @Pattern check (3 letters) but is
 * not a known ISO 4217 code. Handled in GlobalExceptionHandler → HTTP 422:
 * the request was syntactically valid but semantically wrong.
 */
public class UnknownCurrencyException extends RuntimeException {

    public UnknownCurrencyException(String message) {
        super(message);
    }

    public UnknownCurrencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
