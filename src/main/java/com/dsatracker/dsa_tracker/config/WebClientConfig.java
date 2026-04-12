package com.dsatracker.dsa_tracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient codeforcesWebClient() {
        return WebClient.builder()
                .baseUrl("https://codeforces.com/api")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer
                                .defaultCodecs()
                                .maxInMemorySize(5 * 1024 * 1024)) // 5 MB
                        .build())
                .build();
    }

    @Bean
    public WebClient leetcodeWebClient() {
        return WebClient.builder()
                .baseUrl("https://leetcode.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                /*
                 * Why these extra headers?
                 *
                 * LeetCode's GraphQL endpoint is unofficial. Without a proper
                 * Content-Type header, LC may reject the POST or return HTML.
                 * The CF bean didn't need Content-Type because CF uses GET requests
                 * with query params (no request body). LC uses POST with a JSON body,
                 * so Content-Type: application/json is mandatory.
                 *
                 * Some LC API calls return large responses too (full problem catalog
                 * is ~3300 problems), so the 5 MB buffer applies here as well.
                 */
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer
                                .defaultCodecs()
                                .maxInMemorySize(5 * 1024 * 1024))
                        .build())
                .build();
    }
}
