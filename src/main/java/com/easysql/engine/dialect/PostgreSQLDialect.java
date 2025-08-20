package com.easysql.engine.dialect;

public class PostgreSQLDialect implements SQLDialect {

    @Override
    public String getName() {
        return "postgresql";
    }

    @Override
    public String escapeIdentifier(String identifier) {
        if (identifier == null) return null;
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String escapeString(String value) {
        if (value == null) return "NULL";
        return "'" + value.replace("'", "''") + "'";
    }

    @Override
    public String limitSQL(String baseSql, Integer limit, Integer offset) {
        StringBuilder sb = new StringBuilder(baseSql);
        if (limit != null) {
            sb.append(" LIMIT ").append(limit);
        }
        if (offset != null) {
            sb.append(" OFFSET ").append(offset);
        }
        return sb.toString();
    }

    @Override
    public String mapOperator(String operator) {
        if (operator == null) return null;
        // PostgreSQL特有操作符映射
        switch (operator.toUpperCase()) {
            case "ILIKE": return "ILIKE";  // 大小写不敏感的LIKE
            case "REGEXP": return "~";     // 正则表达式匹配
            case "NOT_REGEXP": return "!~"; // 正则表达式不匹配
            case "IREGEXP": return "~*";   // 大小写不敏感正则匹配
            case "NOT_IREGEXP": return "!~*"; // 大小写不敏感正则不匹配
            default: return operator.toUpperCase();
        }
    }

    @Override
    public String mapType(String logicalType) {
        if (logicalType == null) return null;
        switch (logicalType.toUpperCase()) {
            case "STRING": return "VARCHAR";
            case "TEXT": return "TEXT";
            case "INT": return "INTEGER";
            case "BIGINT": return "BIGINT";
            case "DOUBLE": return "DOUBLE PRECISION";
            case "DECIMAL": return "NUMERIC";
            case "DATE": return "DATE";
            case "TIMESTAMP": return "TIMESTAMP";
            case "BOOLEAN": return "BOOLEAN";
            case "JSON": return "JSON";
            case "JSONB": return "JSONB";
            case "UUID": return "UUID";
            case "ARRAY": return "ARRAY";
            default: return logicalType;
        }
    }
}