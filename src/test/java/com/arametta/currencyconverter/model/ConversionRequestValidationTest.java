package com.arametta.currencyconverter.model;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the Jakarta constraints on ConversionRequest without a Spring
 * context, so a broken annotation fails fast and close to the DTO.
 */
class ConversionRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void negativeAmount_failsValidation() {
        Set<ConstraintViolation<ConversionRequest>> violations = validator.validate(
                new ConversionRequest(new BigDecimal("-1"), "USD", "EUR"));

        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .contains("amount");
    }

    @Test
    void nullSourceCurrency_failsValidation() {
        Set<ConstraintViolation<ConversionRequest>> violations = validator.validate(
                new ConversionRequest(new BigDecimal("1"), null, "EUR"));

        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .contains("sourceCurrency");
    }

    @Test
    void nullTargetCurrency_failsValidation() {
        Set<ConstraintViolation<ConversionRequest>> violations = validator.validate(
                new ConversionRequest(new BigDecimal("1"), "USD", null));

        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .contains("targetCurrency");
    }
}
