package com.example.mysqlschemacrawler.dto;

import lombok.Data;

@Data
public class ConnectionConfigDto {
    private String url;
    private String username;
    private String password;
    private String schema;
    private String modelPackage = "com.example.model";
}
