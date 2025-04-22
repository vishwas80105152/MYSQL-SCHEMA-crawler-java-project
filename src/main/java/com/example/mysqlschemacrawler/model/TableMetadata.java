package com.example.mysqlschemacrawler.model;

import lombok.Data;

import java.util.List;

@Data
public class TableMetadata {
    private String name;
    private String className;
    private List<ColumnMetadata> columns;
    private List<String> primaryKeys;
    private List<ForeignKeyMetadata> foreignKeys;
    private List<IndexMetadata> indexes;
}
