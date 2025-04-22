package com.example.mysqlschemacrawler.dto;

import com.example.mysqlschemacrawler.model.TableMetadata;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DatabaseMetadataResponse {
    private String databaseName;
    private List<TableMetadata> tables;
    private Map<String, List<String>> relationships;
    private int totalTables;
    private int totalColumns;
    private int totalRelationships;
}
