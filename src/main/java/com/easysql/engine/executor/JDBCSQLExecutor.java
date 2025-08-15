package com.easysql.engine.executor;

import com.easysql.engine.model.Template;

import java.sql.*;
import java.util.*;

public class JDBCSQLExecutor implements SQLExecutor {

    @Override
    public QueryResult executeQuery(Connection connection, String sql, Template template, Map<String, Object> params) {
        long start = System.currentTimeMillis();
        try {
            NamedParameterStatement nps = new NamedParameterStatement(connection, sql);
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
                return new QueryResult(rows, cols, end - start);
            }
        } catch (SQLException e) {
            throw new RuntimeException("SQL query failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int executeUpdate(Connection connection, String sql, Template template, Map<String, Object> params) {
        try {
            NamedParameterStatement nps = new NamedParameterStatement(connection, sql);
            bindParameters(nps, template, params);
            return nps.getStatement().executeUpdate();
        } catch (SQLException e) {
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