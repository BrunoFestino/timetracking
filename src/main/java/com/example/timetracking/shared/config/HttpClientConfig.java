package com.example.timetracking.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient jiraHttpClient(JiraProperties jiraProps) {
        // SimpleClientHttpRequestFactory (HttpURLConnection) is created lazily: it does not
        // open the java.net.http loopback selector at startup, which fails in some restricted
        // environments. It also lets us honor the configured connect/read timeouts.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(jiraProps.getConnectTimeoutSeconds() * 1000);
        requestFactory.setReadTimeout(jiraProps.getReadTimeoutSeconds() * 1000);
        return RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(jiraProps.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + jiraProps.getToken())
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}

