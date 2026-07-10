package com.example.timetracking.shared.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jira")
public class JiraProperties {

    private String baseUrl;

    private String token;

    private String prjtaskField;

    private int connectTimeoutSeconds;


    private int readTimeoutSeconds;
}
