package com.example.mysqlschemacrawler.model;

import lombok.Data;

@Data
public class ForeignKeyMetadata {
    private String name;
    private String columnName;
    private String referencedTableName;
    private String referencedColumnName;
    private String relationshipType; // ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY
}
