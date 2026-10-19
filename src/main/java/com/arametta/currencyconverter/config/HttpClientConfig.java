package com.arametta.currencyconverter.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate bean for SwopClient.
 *
 * Explicit, conservative timeouts are critical: the default RestTemplate has
 * NO timeouts, which would let a hung upstream stall request threads and
 * eventually exhaust Tomcat's worker pool.
 *
 *   connect — how long to wait to establish the TCP/TLS handshake.
 *   read    — how long to wait for the response body once connected.
 *
 * Tuneable via swop.connect-timeout / swop.read-timeout (defaults 5s / 10s).
 */
@Configuration
@EnableConfigurationProperties(SwopProperties.class)
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, SwopProperties swop) {
        return builder
                .connectTimeout(swop.connectTimeout())
                .readTimeout(swop.readTimeout())
                .build();
    }
}
