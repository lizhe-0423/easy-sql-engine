package com.easysql.engine;

import com.easysql.engine.builder.SQLBuilder;
import com.easysql.engine.dialect.MySQLDialect;
import com.easysql.engine.dialect.SQLDialect;
import com.easysql.engine.model.Template;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EasySQLEngine {

    private final Map<String, SQLDialect> dialects = new HashMap<>();

    public EasySQLEngine() {
        dialects.put("mysql", new MySQLDialect());
        // 其他方言可在M2添加
    }

    /**
     * 从JSON构建SQL
     */
    public String buildSQL(String json) throws IOException {
        Template t = TemplateMapper.fromJson(json);
        Validator.validateBasic(t);
        String dialectName = t.dialect == null || t.dialect.isEmpty() ? "mysql" : t.dialect.toLowerCase();
        SQLDialect dialect = dialects.get(dialectName);
        if (dialect == null) {
            throw new IllegalArgumentException("unsupported dialect: " + dialectName);
        }
        SQLBuilder builder = new SQLBuilder(dialect);
        return builder.buildSelect(t);
    }

    /**
     * 直接从Template构建SQL
     */
    public String buildSQL(Template t) {
        Validator.validateBasic(t);
        String dialectName = t.dialect == null || t.dialect.isEmpty() ? "mysql" : t.dialect.toLowerCase();
        SQLDialect dialect = dialects.get(dialectName);
        if (dialect == null) {
            throw new IllegalArgumentException("unsupported dialect: " + dialectName);
        }
        SQLBuilder builder = new SQLBuilder(dialect);
        return builder.buildSelect(t);
    }

    /**
     * 查询结果模型（暂时简化）
     */
    public static class QueryResult {
        public String sql;
        public Template template;
        public long buildTimeMs;

        public QueryResult(String sql, Template template, long buildTimeMs) {
            this.sql = sql;
            this.template = template;
            this.buildTimeMs = buildTimeMs;
        }
    }

    /**
     * 完整解析与构建（包含性能指标）
     */
    public QueryResult parseAndBuild(String json) throws IOException {
        long start = System.currentTimeMillis();
        Template t = TemplateMapper.fromJson(json);
        Validator.validateBasic(t);
        String dialectName = t.dialect == null || t.dialect.isEmpty() ? "mysql" : t.dialect.toLowerCase();
        SQLDialect dialect = dialects.get(dialectName);
        if (dialect == null) {
            throw new IllegalArgumentException("unsupported dialect: " + dialectName);
        }
        SQLBuilder builder = new SQLBuilder(dialect);
        String sql = builder.buildSelect(t);
        long end = System.currentTimeMillis();
        return new QueryResult(sql, t, end - start);
    }

    /**
     * 从Template构建（包含性能指标）
     */
    public QueryResult parseAndBuild(Template t) {
        long start = System.currentTimeMillis();
        Validator.validateBasic(t);
        String dialectName = t.dialect == null || t.dialect.isEmpty() ? "mysql" : t.dialect.toLowerCase();
        SQLDialect dialect = dialects.get(dialectName);
        if (dialect == null) {
            throw new IllegalArgumentException("unsupported dialect: " + dialectName);
        }
        SQLBuilder builder = new SQLBuilder(dialect);
        String sql = builder.buildSelect(t);
        long end = System.currentTimeMillis();
        return new QueryResult(sql, t, end - start);
    }
}