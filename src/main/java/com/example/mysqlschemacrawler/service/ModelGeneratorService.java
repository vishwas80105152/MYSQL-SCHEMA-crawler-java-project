package com.example.mysqlschemacrawler.service;

import com.example.mysqlschemacrawler.dto.DatabaseMetadataResponse;
import com.example.mysqlschemacrawler.model.ColumnMetadata;
import com.example.mysqlschemacrawler.model.ForeignKeyMetadata;
import com.example.mysqlschemacrawler.model.TableMetadata;
import com.example.mysqlschemacrawler.util.NamingUtils;
import com.squareup.javapoet.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.lang.model.element.Modifier;
import java.util.*;

@Service
@Slf4j
public class ModelGeneratorService {

    public Map<String, String> generateModelClasses(DatabaseMetadataResponse metadata, String packageName) {
        Map<String, String> generatedClasses = new HashMap<>();
        Map<String, String> tableToClassNameMap = createTableToClassNameMap(metadata.getTables());
        
        for (TableMetadata table : metadata.getTables()) {
            String className = table.getClassName();
            TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(AnnotationSpec.builder(ClassName.get("lombok", "Data")).build())
                    .addAnnotation(AnnotationSpec.builder(ClassName.get("jakarta.persistence", "Entity")).build())
                    .addAnnotation(AnnotationSpec.builder(ClassName.get("jakarta.persistence", "Table"))
                            .addMember("name", "$S", table.getName())
                            .build());
            
            // Add fields
            for (ColumnMetadata column : table.getColumns()) {
                addField(classBuilder, column, tableToClassNameMap);
            }
            
            // Add relationships
            addRelationships(classBuilder, table, tableToClassNameMap);
            
            // Generate class
            TypeSpec typeSpec = classBuilder.build();
            JavaFile javaFile = JavaFile.builder(packageName, typeSpec).build();
            
            try {
                String sourceCode = javaFile.toString();
                generatedClasses.put(className, sourceCode);
            } catch (Exception e) {
                log.error("Failed to generate class: " + className, e);
            }
        }
        
        return generatedClasses;
    }

    private Map<String, String> createTableToClassNameMap(List<TableMetadata> tables) {
        Map<String, String> map = new HashMap<>();
        for (TableMetadata table : tables) {
            map.put(table.getName(), table.getClassName());
        }
        return map;
    }

    private void addField(TypeSpec.Builder classBuilder, ColumnMetadata column, Map<String, String> tableToClassNameMap) {
        // Skip if this is a foreign key that will be represented as a relationship
        if (column.isForeignKey()) {
            return;
        }
        
        FieldSpec.Builder fieldBuilder = FieldSpec.builder(
                getTypeFromString(column.getJavaType()),
                column.getFieldName(),
                Modifier.PRIVATE
        );
        
        // Add JPA annotations
        if (column.isPrimaryKey()) {
            fieldBuilder.addAnnotation(AnnotationSpec.builder(ClassName.get("jakarta.persistence", "Id")).build());
            
            // Add GeneratedValue if it's an auto-increment field (assuming Integer/Long type for simplicity)
            if (column.getJavaType().equals("Integer") || column.getJavaType().equals("Long")) {
                fieldBuilder.addAnnotation(
                        AnnotationSpec.builder(ClassName.get("jakarta.persistence", "GeneratedValue"))
                                .addMember("strategy", "$T.IDENTITY", ClassName.get("jakarta.persistence", "GenerationType"))
                                .build()
                );
            }
        }
        
        // Add Column annotation
        AnnotationSpec.Builder columnAnnotation = AnnotationSpec.builder(ClassName.get("jakarta.persistence", "Column"))
                .addMember("name", "$S", column.getName());
        
        if (!column.isNullable()) {
            columnAnnotation.addMember("nullable", "$L", false);
        }
        
        if (column.getSize() > 0 && column.getJavaType().equals("String")) {
            columnAnnotation.addMember("length", "$L", column.getSize());
        }
        
        fieldBuilder.addAnnotation(columnAnnotation.build());
        classBuilder.addField(fieldBuilder.build());
    }

    private void addRelationships(TypeSpec.Builder classBuilder, TableMetadata table, Map<String, String> tableToClassNameMap) {
        // Process foreign keys to create relationships
        for (ForeignKeyMetadata fk : table.getForeignKeys()) {
            String referencedTableName = fk.getReferencedTableName();
            String referencedClassName = tableToClassNameMap.get(referencedTableName);
            
            if (referencedClassName == null) {
                log.warn("Referenced table not found: " + referencedTableName);
                continue;
            }
            
            // Create field for the relationship
            String fieldName = NamingUtils.toCamelCase(referencedTableName);
            
            // Determine relationship type and add appropriate annotations
            ClassName referencedClass = ClassName.bestGuess(referencedClassName);
            
            // Default to ManyToOne relationship
            FieldSpec.Builder fieldBuilder = FieldSpec.builder(referencedClass, fieldName, Modifier.PRIVATE);
            
            fieldBuilder.addAnnotation(
                    AnnotationSpec.builder(ClassName.get("jakarta.persistence", "ManyToOne"))
                            .addMember("fetch", "$T.LAZY", ClassName.get("jakarta.persistence", "FetchType"))
                            .build()
            );
            
            fieldBuilder.addAnnotation(
                    AnnotationSpec.builder(ClassName.get("jakarta.persistence", "JoinColumn"))
                            .addMember("name", "$S", fk.getColumnName())
                            .addMember("referencedColumnName", "$S", fk.getReferencedColumnName())
                            .build()
            );
            
            classBuilder.addField(fieldBuilder.build());
            
            // Check for bidirectional relationships (simplified approach)
            // In a real implementation, you would need to analyze the database more thoroughly
            // to determine the correct relationship type (OneToMany, ManyToMany, etc.)
        }
    }

    private TypeName getTypeFromString(String typeName) {
        switch (typeName) {
            case "Integer":
                return TypeName.INT.box();
            case "Long":
                return TypeName.LONG.box();
            case "Float":
                return TypeName.FLOAT.box();
            case "Double":
                return TypeName.DOUBLE.box();
            case "Boolean":
                return TypeName.BOOLEAN.box();
            case "String":
                return ClassName.get(String.class);
            case "java.time.LocalDate":
                return ClassName.get("java.time", "LocalDate");
            case "java.time.LocalTime":
                return ClassName.get("java.time", "LocalTime");
            case "java.time.LocalDateTime":
                return ClassName.get("java.time", "LocalDateTime");
            case "byte[]":
                return ArrayTypeName.of(TypeName.BYTE);
            default:
                return ClassName.bestGuess(typeName);
        }
    }
}
