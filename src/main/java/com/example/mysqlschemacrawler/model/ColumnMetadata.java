package com.example.mysqlschemacrawler.model;

import lombok.Data;

@Data
public class ColumnMetadata {
    private String name;
    private String dataType;
    private int size;
    private boolean nullable;
    private boolean primaryKey;
    private boolean foreignKey;
    private String referencedTable;
    private String referencedColumn;
    private String javaType;
    private String fieldName;
}
