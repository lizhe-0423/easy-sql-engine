package com.easysql.engine.dialect;

public class MySQLDialect implements SQLDialect {

    @Override
    public String getName() {
        return "mysql";
    }

    @Override
    public String escapeIdentifier(String identifier) {
        if (identifier == null) return null;
        return "`" + identifier.replace("`", "``") + "`";
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
            if (limit == null) {
                // MySQL允许使用 LIMIT 18446744073709551615 OFFSET x，但我们简单处理为 LIMIT 大数
                sb.append(" LIMIT 18446744073709551615");
            }
            sb.append(" OFFSET ").append(offset);
        }
        return sb.toString();
    }

    @Override
    public String mapOperator(String operator) {
        if (operator == null) return null;
        // MySQL基本操作符直接返回
        return operator.toUpperCase();
    }

    @Override
    public String mapType(String logicalType) {
        if (logicalType == null) return null;
        switch (logicalType.toUpperCase()) {
            case "STRING": return "VARCHAR";
            case "INT": return "INT";
            case "BIGINT": return "BIGINT";
            case "DOUBLE": return "DOUBLE";
            case "DECIMAL": return "DECIMAL";
            case "DATE": return "DATE";
            case "TIMESTAMP": return "TIMESTAMP";
            case "BOOLEAN": return "BOOLEAN";
            default: return logicalType;
        }
    }
}