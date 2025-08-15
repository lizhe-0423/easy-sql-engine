package com.easysql.engine.executor;

import com.easysql.engine.model.Template;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * SQL执行器接口
 */
public interface SQLExecutor {

    /**
     * 执行查询
     * @param connection 数据库连接
     * @param sql 带占位符的SQL
     * @param template 模板对象（用于参数定义）
     * @param params 参数值映射
     * @return 查询结果
     */
    QueryResult executeQuery(Connection connection, String sql, Template template, Map<String, Object> params);

    /**
     * 执行更新操作（INSERT/UPDATE/DELETE）
     * @param connection 数据库连接
     * @param sql 带占位符的SQL
     * @param template 模板对象（用于参数定义）
     * @param params 参数值映射
     * @return 影响行数
     */
    int executeUpdate(Connection connection, String sql, Template template, Map<String, Object> params);

    /**
     * 查询结果封装
     */
    class QueryResult {
        private final List<Map<String, Object>> rows;
        private final List<String> columnNames;
        private final long executionTimeMs;

        public QueryResult(List<Map<String, Object>> rows, List<String> columnNames, long executionTimeMs) {
            this.rows = rows;
            this.columnNames = columnNames;
            this.executionTimeMs = executionTimeMs;
        }

        public List<Map<String, Object>> getRows() {
            return rows;
        }

        public List<String> getColumnNames() {
            return columnNames;
        }

        public long getExecutionTimeMs() {
            return executionTimeMs;
        }

        public int getRowCount() {
            return rows != null ? rows.size() : 0;
        }
    }

    /**
     * 参数类型转换工具
     */
    class ParameterConverter {
        
        public static Object convertParameter(String type, Object value) {
            if (value == null) return null;
            
            switch (type.toUpperCase()) {
                case "STRING":
                    return value.toString();
                case "INT":
                    if (value instanceof Number) {
                        return ((Number) value).intValue();
                    }
                    return Integer.parseInt(value.toString());
                case "BIGINT":
                    if (value instanceof Number) {
                        return ((Number) value).longValue();
                    }
                    return Long.parseLong(value.toString());
                case "DOUBLE":
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue();
                    }
                    return Double.parseDouble(value.toString());
                case "DECIMAL":
                    if (value instanceof BigDecimal) {
                        return value;
                    }
                    return new BigDecimal(value.toString());
                case "DATE":
                    if (value instanceof Date) {
                        return value;
                    }
                    if (value instanceof java.util.Date) {
                        return new Date(((java.util.Date) value).getTime());
                    }
                    return Date.valueOf(value.toString());
                case "TIMESTAMP":
                    if (value instanceof Timestamp) {
                        return value;
                    }
                    if (value instanceof java.util.Date) {
                        return new Timestamp(((java.util.Date) value).getTime());
                    }
                    return Timestamp.valueOf(value.toString());
                case "BOOLEAN":
                    if (value instanceof Boolean) {
                        return value;
                    }
                    return Boolean.parseBoolean(value.toString());
                default:
                    return value;
            }
        }
    }
}