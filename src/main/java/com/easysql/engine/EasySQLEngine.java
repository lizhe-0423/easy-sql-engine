package com.easysql.engine;

import com.easysql.engine.builder.SQLBuilder;
import com.easysql.engine.dialect.MySQLDialect;
import com.easysql.engine.dialect.SQLDialect;
import com.easysql.engine.model.Template;
import com.easysql.engine.monitor.MetricsCollector;
import com.easysql.engine.monitor.QueryMetrics;
import com.easysql.engine.optimizer.BasicOptimizer;
import com.easysql.engine.dsl.Query;
import com.easysql.engine.metadata.MetadataCache;
import com.easysql.engine.executor.JDBCSQLExecutor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EasySQLEngine {

    private final Map<String, SQLDialect> dialects = new HashMap<>();
    private final MetricsCollector metrics = new MetricsCollector();
    private final MetadataCache metadataCache = new MetadataCache(1000, 300000); // 1000条，5分钟TTL

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
        // M2：调用简单优化器
        BasicOptimizer optimizer = new BasicOptimizer();
        t = optimizer.optimize(t);
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
        try {
            Template t = TemplateMapper.fromJson(json);
            Validator.validateBasic(t);
            // M2：优化
            BasicOptimizer optimizer = new BasicOptimizer();
            t = optimizer.optimize(t);
            String dialectName = t.dialect == null || t.dialect.isEmpty() ? "mysql" : t.dialect.toLowerCase();
            SQLDialect dialect = dialects.get(dialectName);
            if (dialect == null) {
                throw new IllegalArgumentException("unsupported dialect: " + dialectName);
            }
            SQLBuilder builder = new SQLBuilder(dialect);
            String sql = builder.buildSelect(t);
            long end = System.currentTimeMillis();
            long buildTime = end - start;
            metrics.record(QueryMetrics.success(t.id, t.datasource, buildTime, 0, 0));
            return new QueryResult(sql, t, buildTime);
        } catch (RuntimeException e) {
            long end = System.currentTimeMillis();
            metrics.record(QueryMetrics.failure("unknown", "unknown", end - start, "BUILD_ERROR", e.getMessage()));
            throw e;
        }
    }

    /**
     * 从Template构建（包含性能指标）
     */
    public QueryResult parseAndBuild(Template t) {
        long start = System.currentTimeMillis();
        try {
            Validator.validateBasic(t);
            // M2：优化
            BasicOptimizer optimizer = new BasicOptimizer();
            t = optimizer.optimize(t);
            String dialectName = t.dialect == null || t.dialect.isEmpty() ? "mysql" : t.dialect.toLowerCase();
            SQLDialect dialect = dialects.get(dialectName);
            if (dialect == null) {
                throw new IllegalArgumentException("unsupported dialect: " + dialectName);
            }
            SQLBuilder builder = new SQLBuilder(dialect);
            String sql = builder.buildSelect(t);
            long end = System.currentTimeMillis();
            long buildTime = end - start;
            metrics.record(QueryMetrics.success(t.id, t.datasource, buildTime, 0, 0));
            return new QueryResult(sql, t, buildTime);
        } catch (RuntimeException e) {
            long end = System.currentTimeMillis();
            metrics.record(QueryMetrics.failure(t != null ? t.id : "unknown", t != null ? t.datasource : "unknown", end - start, "BUILD_ERROR", e.getMessage()));
            throw e;
        }
    }

    /**
     * 从DSL对象构建SQL
     */
    public QueryResult buildSQL(Query query) {
        return parseAndBuild(query.build());
    }

    /**
     * 获取指标收集器（便于外部监控集成）
     */
    public MetricsCollector getMetrics() {
        return metrics;
    }

    /**
     * 获取元数据缓存（便于数据源注册）
     */
    public MetadataCache getMetadataCache() {
        return metadataCache;
    }

    /**
     * 创建绑定了引擎MetricsCollector的JDBC执行器
     */
    public JDBCSQLExecutor createExecutor() {
        return new JDBCSQLExecutor(this.metrics);
    }
}