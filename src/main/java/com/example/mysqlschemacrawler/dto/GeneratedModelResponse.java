package com.example.mysqlschemacrawler.dto;

import lombok.Data;

import java.util.Map;

@Data
public class GeneratedModelResponse {
    private Map<String, String> generatedClasses;
    private int totalClasses;
}
