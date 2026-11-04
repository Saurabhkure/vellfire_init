package com.website.vellfire_init.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    @Value("${app.http.connect-timeout-seconds:5}")
    private int connectTimeoutSeconds;

    @Value("${app.http.read-timeout-seconds:10}")
    private int readTimeoutSeconds;

    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutSeconds * 1000);
        factory.setReadTimeout(readTimeoutSeconds * 1000);
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
