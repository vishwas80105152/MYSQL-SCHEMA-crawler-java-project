package com.example.mysqlschemacrawler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "crawler.database")
@Data
public class DatabaseConfig {
    private String url;
    private String username;
    private String password;
    private String schema;
    private String modelPackage = "com.example.model";
}
