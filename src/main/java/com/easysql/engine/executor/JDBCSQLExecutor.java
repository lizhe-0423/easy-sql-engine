package com.easysql.engine.executor;

import com.easysql.engine.model.Template;

import java.sql.*;
import java.util.*;

import com.easysql.engine.monitor.MetricsCollector;
import com.easysql.engine.monitor.QueryMetrics;

public class JDBCSQLExecutor implements SQLExecutor {

    private MetricsCollector metrics;

    public JDBCSQLExecutor() {}
    public JDBCSQLExecutor(MetricsCollector metrics) { this.metrics = metrics; }
    public void setMetrics(MetricsCollector metrics) { this.metrics = metrics; }

    private MetricsCollector metrics() {
        if (this.metrics == null) this.metrics = new MetricsCollector();
        return this.metrics;
    }

    @Override
    public QueryResult executeQuery(Connection connection, String sql, Template template, Map<String, Object> params) {
        long start = System.currentTimeMillis();
        try {
            NamedParameterStatement nps = new NamedParameterStatement(connection, sql);
            // 设置执行参数，如超时、fetchSize、maxRows（若模板提供）
            if (template.options != null) {
                if (template.options.fetchSize != null) {
                    nps.getStatement().setFetchSize(template.options.fetchSize);
                }
                if (template.options.readOnly != null) {
                    nps.getStatement().getConnection().setReadOnly(template.options.readOnly);
                }
                if (template.options.timeoutMs != null) {
                    nps.getStatement().setQueryTimeout(Math.max(1, template.options.timeoutMs / 1000));
                }
                if (template.options.maxRows != null) {
                    nps.getStatement().setMaxRows(template.options.maxRows);
                }
            }
            bindParameters(nps, template, params);
            try (ResultSet rs = nps.getStatement().executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                List<String> cols = new ArrayList<>();
                for (int i = 1; i <= colCount; i++) {
                    cols.add(meta.getColumnLabel(i));
                }
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.put(cols.get(i - 1), rs.getObject(i));
                    }
                    rows.add(row);
                }
                long end = System.currentTimeMillis();
                long execTime = end - start;
                metrics().record(QueryMetrics.success(template.id, template.version, template.datasource, 0, execTime, rows.size()));
                return new QueryResult(rows, cols, execTime);
            }
        } catch (SQLException e) {
            long end = System.currentTimeMillis();
            long execTime = end - start;
            metrics().record(QueryMetrics.failure(template.id, template.version, template.datasource, 0, "JDBC_ERROR", e.getMessage()));
            throw new RuntimeException("SQL query failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int executeUpdate(Connection connection, String sql, Template template, Map<String, Object> params) {
        long start = System.currentTimeMillis();
        try {
            NamedParameterStatement nps = new NamedParameterStatement(connection, sql);
            if (template.options != null) {
                if (template.options.timeoutMs != null) {
                    nps.getStatement().setQueryTimeout(Math.max(1, template.options.timeoutMs / 1000));
                }
            }
            bindParameters(nps, template, params);
            int updated = nps.getStatement().executeUpdate();
            long end = System.currentTimeMillis();
            long execTime = end - start;
            metrics().record(QueryMetrics.success(template.id, template.version, template.datasource, 0, execTime, updated));
            return updated;
        } catch (SQLException e) {
            long end = System.currentTimeMillis();
            long execTime = end - start;
            metrics().record(QueryMetrics.failure(template.id, template.version, template.datasource, 0, "JDBC_ERROR", e.getMessage()));
            throw new RuntimeException("SQL update failed: " + e.getMessage(), e);
        }
    }

    private void bindParameters(NamedParameterStatement nps, Template template, Map<String, Object> params) throws SQLException {
        Map<String, String> typeMap = new HashMap<>();
        if (template.params != null) {
            for (Template.Param p : template.params) {
                typeMap.put(p.name, p.type);
            }
        }
        List<String> order = nps.getOrder();
        for (int i = 0; i < order.size(); i++) {
            String name = order.get(i);
            Object val = params == null ? null : params.get(name);
            String type = typeMap.getOrDefault(name, "STRING");
            Object converted = SQLExecutor.ParameterConverter.convertParameter(type, val);
            nps.setObject(i + 1, converted);
        }
    }
}