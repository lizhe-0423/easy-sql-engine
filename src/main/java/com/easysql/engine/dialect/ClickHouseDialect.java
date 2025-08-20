package com.easysql.engine.dialect;

public class ClickHouseDialect implements SQLDialect {

    @Override
    public String getName() {
        return "clickhouse";
    }

    @Override
    public String escapeIdentifier(String identifier) {
        if (identifier == null) return null;
        // ClickHouse建议使用反引号或双引号，反引号兼容性较好
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
        if (limit != null && offset != null) {
            // ClickHouse支持 LIMIT x OFFSET y 或 LIMIT y, x 两种
            sb.append(" LIMIT ").append(limit).append(" OFFSET ").append(offset);
            return sb.toString();
        }
        if (limit != null) {
            sb.append(" LIMIT ").append(limit);
        }
        if (offset != null) {
            // 无limit时，使用 LIMIT 18446744073709551615 OFFSET x 的方法不适用于ClickHouse，保持仅offset不生效的约定
            sb.append(" OFFSET ").append(offset);
        }
        return sb.toString();
    }

    @Override
    public String mapOperator(String operator) {
        if (operator == null) return null;
        switch (operator.toUpperCase()) {
            case "REGEXP": return "match"; // ClickHouse函数式正则：match(subject, pattern)
            default: return operator.toUpperCase();
        }
    }

    @Override
    public String mapType(String logicalType) {
        if (logicalType == null) return null;
        switch (logicalType.toUpperCase()) {
            case "STRING": return "String";
            case "INT": return "Int32";
            case "BIGINT": return "Int64";
            case "DOUBLE": return "Float64";
            case "DECIMAL": return "Decimal(38, 18)";
            case "DATE": return "Date";
            case "TIMESTAMP": return "DateTime";
            case "BOOLEAN": return "UInt8";
            default: return logicalType;
        }
    }
}