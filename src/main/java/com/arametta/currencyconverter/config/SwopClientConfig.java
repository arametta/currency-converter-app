package com.arametta.currencyconverter.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * RestClient for swop.cx with base URL, auth header and timeouts applied once,
 * so SwopClient only describes the requests.
 *
 * Explicit timeouts are critical: without them a hung upstream would stall
 * request threads and eventually exhaust Tomcat's worker pool.
 * Tuneable via swop.connect-timeout / swop.read-timeout (defaults 5s / 10s).
 */
@Configuration
@EnableConfigurationProperties(SwopProperties.class)
public class SwopClientConfig {

    @Bean
    public RestClient swopRestClient(RestClient.Builder builder, SwopProperties swop) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(swop.connectTimeout())
                .withReadTimeout(swop.readTimeout());

        return builder
                .baseUrl(swop.baseUrl())
                // swop.cx auth scheme is literal "ApiKey <token>", not "Bearer".
                .defaultHeader(HttpHeaders.AUTHORIZATION, "ApiKey " + swop.apiKey())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                // JDK HttpClient: no extra dependency, and the choice is explicit
                // rather than depending on what happens to be on the classpath.
                .requestFactory(ClientHttpRequestFactoryBuilder.jdk().build(settings))
                .build();
    }
}
