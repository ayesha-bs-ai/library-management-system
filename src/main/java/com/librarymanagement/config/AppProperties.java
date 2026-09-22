package com.librarymanagement.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String libraryName = "Simple Library";
    private String baseUrl = "http://localhost:8080";
    private String currency = "PKR";
    private boolean mailEnabled = false;
    private boolean demoMode = false;
}
