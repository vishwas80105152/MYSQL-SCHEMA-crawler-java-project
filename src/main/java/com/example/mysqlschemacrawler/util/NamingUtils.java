package com.example.mysqlschemacrawler.util;

import java.util.Arrays;
import java.util.stream.Collectors;

public class NamingUtils {

    /**
     * Converts a database table name to a Java class name (PascalCase)
     * Example: "user_profiles" -> "UserProfile"
     */
    public static String toClassName(String tableName) {
        // Handle special cases
        if (tableName == null || tableName.isEmpty()) {
            return "Unknown";
        }
        
        // Remove trailing 's' if it exists (simple pluralization)
        String singular = tableName.endsWith("s") && !tableName.endsWith("ss") 
                ? tableName.substring(0, tableName.length() - 1) 
                : tableName;
        
        // Convert snake_case to PascalCase
        return Arrays.stream(singular.split("_"))
                .map(NamingUtils::capitalize)
                .collect(Collectors.joining());
    }
    
    /**
     * Converts a database column name to a Java field name (camelCase)
     * Example: "user_id" -> "userId"
     */
    public static String toCamelCase(String columnName) {
        if (columnName == null || columnName.isEmpty()) {
            return "unknown";
        }
        
        String[] parts = columnName.split("_");
        StringBuilder result = new StringBuilder(parts[0].toLowerCase());
        
        for (int i = 1; i < parts.length; i++) {
            result.append(capitalize(parts[i]));
        }
        
        return result.toString();
    }
    
    /**
     * Capitalizes the first letter of a string
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
