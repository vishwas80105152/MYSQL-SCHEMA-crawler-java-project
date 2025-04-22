package com.example.mysqlschemacrawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class MySqlSchemaCrawlerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MySqlSchemaCrawlerApplication.class, args);
    }
}
