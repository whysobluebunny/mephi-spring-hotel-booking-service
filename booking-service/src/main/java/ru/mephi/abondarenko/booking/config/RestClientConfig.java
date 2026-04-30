package ru.mephi.abondarenko.booking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    @Bean
    RestTemplate restTemplate(
            RestTemplateBuilder builder,
            @Value("${app.integration.connect-timeout-ms:1000}") long connectTimeoutMs,
            @Value("${app.integration.read-timeout-ms:1500}") long readTimeoutMs
    ) {
        return builder
                .setConnectTimeout(java.time.Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(java.time.Duration.ofMillis(readTimeoutMs))
                .build();
    }
}
