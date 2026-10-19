package com.arametta.currencyconverter.client;

/**
 * Thrown when a currency code is not usable:
 *   - CurrencyConversionService finds it missing from swop.cx's active
 *     currency list, OR
 *   - swop.cx responds 404 to a rate lookup (safety net for a stale list).
 *
 * Both cases collapse to HTTP 422 (Unprocessable Entity) in
 * GlobalExceptionHandler: the request was syntactically valid (passed
 * @Pattern/@Size) but semantically wrong.
 *
 * Lives in the client package because SwopClient (the lower layer) needs to
 * throw it; service-layer code is allowed to depend on client-layer types.
 */
public class UnknownCurrencyException extends RuntimeException {

    public UnknownCurrencyException(String message) {
        super(message);
    }

    public UnknownCurrencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
