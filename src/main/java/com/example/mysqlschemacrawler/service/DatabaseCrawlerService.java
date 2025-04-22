package com.example.mysqlschemacrawler.service;

import com.example.mysqlschemacrawler.dto.DatabaseMetadataResponse;
import com.example.mysqlschemacrawler.model.ColumnMetadata;
import com.example.mysqlschemacrawler.model.ForeignKeyMetadata;
import com.example.mysqlschemacrawler.model.IndexMetadata;
import com.example.mysqlschemacrawler.model.TableMetadata;
import com.example.mysqlschemacrawler.util.NamingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
@Slf4j
public class DatabaseCrawlerService {

    public boolean testConnection(String url, String username, String password) {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            return true;
        } catch (SQLException e) {
            log.error("Connection test failed", e);
            return false;
        }
    }

    public DatabaseMetadataResponse extractDatabaseMetadata(String url, String username, String password, String schema) {
        DatabaseMetadataResponse response = new DatabaseMetadataResponse();
        response.setDatabaseName(schema);
        
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // Get tables
            List<TableMetadata> tables = extractTables(metaData, schema);
            response.setTables(tables);
            
            // Extract relationships
            Map<String, List<String>> relationships = extractRelationships(tables);
            response.setRelationships(relationships);
            
            // Set statistics
            response.setTotalTables(tables.size());
            int totalColumns = tables.stream().mapToInt(t -> t.getColumns().size()).sum();
            response.setTotalColumns(totalColumns);
            response.setTotalRelationships(relationships.size());
            
            return response;
        } catch (SQLException e) {
            log.error("Failed to extract database metadata", e);
            throw new RuntimeException("Failed to extract database metadata", e);
        }
    }

    private List<TableMetadata> extractTables(DatabaseMetaData metaData, String schema) throws SQLException {
        List<TableMetadata> tables = new ArrayList<>();
        
        try (ResultSet rs = metaData.getTables(null, schema, null, new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                
                TableMetadata table = new TableMetadata();
                table.setName(tableName);
                table.setClassName(NamingUtils.toClassName(tableName));
                
                // Extract columns
                table.setColumns(extractColumns(metaData, schema, tableName));
                
                // Extract primary keys
                table.setPrimaryKeys(extractPrimaryKeys(metaData, schema, tableName));
                
                // Extract foreign keys
                table.setForeignKeys(extractForeignKeys(metaData, schema, tableName));
                
                // Extract indexes
                table.setIndexes(extractIndexes(metaData, schema, tableName));
                
                tables.add(table);
            }
        }
        
        return tables;
    }

    private List<ColumnMetadata> extractColumns(DatabaseMetaData metaData, String schema, String tableName) throws SQLException {
        List<ColumnMetadata> columns = new ArrayList<>();
        Set<String> primaryKeys = new HashSet<>(extractPrimaryKeys(metaData, schema, tableName));
        
        try (ResultSet rs = metaData.getColumns(null, schema, tableName, null)) {
            while (rs.next()) {
                ColumnMetadata column = new ColumnMetadata();
                String columnName = rs.getString("COLUMN_NAME");
                
                column.setName(columnName);
                column.setDataType(rs.getString("TYPE_NAME"));
                column.setSize(rs.getInt("COLUMN_SIZE"));
                column.setNullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.setPrimaryKey(primaryKeys.contains(columnName));
                column.setFieldName(NamingUtils.toCamelCase(columnName));
                column.setJavaType(mapSqlTypeToJavaType(rs.getInt("DATA_TYPE"), rs.getInt("COLUMN_SIZE")));
                
                columns.add(column);
            }
        }
        
        // Mark foreign key columns
        try (ResultSet rs = metaData.getImportedKeys(null, schema, tableName)) {
            while (rs.next()) {
                String fkColumnName = rs.getString("FKCOLUMN_NAME");
                String pkTableName = rs.getString("PKTABLE_NAME");
                String pkColumnName = rs.getString("PKCOLUMN_NAME");
                
                for (ColumnMetadata column : columns) {
                    if (column.getName().equals(fkColumnName)) {
                        column.setForeignKey(true);
                        column.setReferencedTable(pkTableName);
                        column.setReferencedColumn(pkColumnName);
                        break;
                    }
                }
            }
        }
        
        return columns;
    }

    private List<String> extractPrimaryKeys(DatabaseMetaData metaData, String schema, String tableName) throws SQLException {
        List<String> primaryKeys = new ArrayList<>();
        
        try (ResultSet rs = metaData.getPrimaryKeys(null, schema, tableName)) {
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"));
            }
        }
        
        return primaryKeys;
    }

    private List<ForeignKeyMetadata> extractForeignKeys(DatabaseMetaData metaData, String schema, String tableName) throws SQLException {
        List<ForeignKeyMetadata> foreignKeys = new ArrayList<>();
        
        try (ResultSet rs = metaData.getImportedKeys(null, schema, tableName)) {
            while (rs.next()) {
                ForeignKeyMetadata fk = new ForeignKeyMetadata();
                fk.setName(rs.getString("FK_NAME"));
                fk.setColumnName(rs.getString("FKCOLUMN_NAME"));
                fk.setReferencedTableName(rs.getString("PKTABLE_NAME"));
                fk.setReferencedColumnName(rs.getString("PKCOLUMN_NAME"));
                
                // Determine relationship type (simplified logic)
                fk.setRelationshipType("MANY_TO_ONE"); // Default assumption
                
                foreignKeys.add(fk);
            }
        }
        
        return foreignKeys;
    }

    private List<IndexMetadata> extractIndexes(DatabaseMetaData metaData, String schema, String tableName) throws SQLException {
        Map<String, IndexMetadata> indexMap = new HashMap<>();
        
        try (ResultSet rs = metaData.getIndexInfo(null, schema, tableName, false, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                if (indexName == null) continue; // Skip unnamed indexes
                
                IndexMetadata index = indexMap.computeIfAbsent(indexName, k -> {
                    IndexMetadata newIndex = new IndexMetadata();
                    try {
                        newIndex.setName(indexName);
                        newIndex.setUnique(!rs.getBoolean("NON_UNIQUE"));
                        newIndex.setColumns(new ArrayList<>());
                    } catch (SQLException e) {
                        e.printStackTrace(); // or log it properly
                    }
                    return newIndex;
                });
                
                index.getColumns().add(rs.getString("COLUMN_NAME"));
            }
        }
        
        return new ArrayList<>(indexMap.values());
    }

    private Map<String, List<String>> extractRelationships(List<TableMetadata> tables) {
        Map<String, List<String>> relationships = new HashMap<>();
        
        for (TableMetadata table : tables) {
            for (ForeignKeyMetadata fk : table.getForeignKeys()) {
                String sourceTable = table.getName();
                String targetTable = fk.getReferencedTableName();
                
                String relationshipKey = sourceTable + "->" + targetTable;
                relationships.computeIfAbsent(relationshipKey, k -> new ArrayList<>())
                        .add(fk.getColumnName() + " references " + fk.getReferencedTableName() + "." + fk.getReferencedColumnName());
            }
        }
        
        return relationships;
    }

    private String mapSqlTypeToJavaType(int sqlType, int size) {
        switch (sqlType) {
            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.TINYINT:
                return "Integer";
            case Types.BIGINT:
                return "Long";
            case Types.FLOAT:
            case Types.REAL:
                return "Float";
            case Types.DOUBLE:
            case Types.DECIMAL:
                return "Double";
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
                return "String";
            case Types.DATE:
                return "java.time.LocalDate";
            case Types.TIME:
                return "java.time.LocalTime";
            case Types.TIMESTAMP:
                return "java.time.LocalDateTime";
            case Types.BOOLEAN:
            case Types.BIT:
                return size > 1 ? "byte[]" : "Boolean";
            case Types.BLOB:
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                return "byte[]";
            default:
                return "Object";
        }
    }
}
