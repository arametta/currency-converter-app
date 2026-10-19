package com.arametta.currencyconverter.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SwopPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfig.class)
            .withPropertyValues(
                    "swop.base-url=https://swop.cx",
                    "swop.connect-timeout=3s",
                    "swop.read-timeout=750ms");

    @Test
    void bindsAllSettings() {
        runner.withPropertyValues("swop.api-key=abc").run(context -> {
            assertThat(context).hasNotFailed();
            SwopProperties properties = context.getBean(SwopProperties.class);
            assertThat(properties.baseUrl()).isEqualTo("https://swop.cx");
            assertThat(properties.apiKey()).isEqualTo("abc");
            assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
            assertThat(properties.readTimeout()).isEqualTo(Duration.ofMillis(750));
        });
    }

    @Test
    void blankApiKey_failsStartup() {
        runner.withPropertyValues("swop.api-key=").run(context ->
                assertThat(context).getFailure()
                        .rootCause()
                        .hasMessageContaining("apiKey"));
    }

    @Test
    void missingApiKey_failsStartup() {
        runner.run(context -> assertThat(context).hasFailed());
    }

    @EnableConfigurationProperties(SwopProperties.class)
    static class PropertiesConfig {
    }
}
