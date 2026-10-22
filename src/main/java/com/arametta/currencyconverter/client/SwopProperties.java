package com.arametta.currencyconverter.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * swop.cx connection settings. Validated at startup so a missing API key
 * fails fast instead of on the first upstream call.
 */
@Validated
@ConfigurationProperties(prefix = "swop")
public record SwopProperties(
        @NotBlank String baseUrl,
        @NotBlank(message = "must be set (SWOP_API_KEY)") String apiKey,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout
) {
}
