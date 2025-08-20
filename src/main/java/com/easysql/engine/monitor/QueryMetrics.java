package com.easysql.engine.monitor;

/**
 * 查询指标数据
 */
public class QueryMetrics {
    public String templateId;
    public String templateVersion; // 模板版本，用于审计追踪
    public String datasource;
    public long buildTimeMs;
    public long executeTimeMs;
    public int rowCount;
    public boolean success;
    public String errorCode;
    public String errorMessage;

    public static QueryMetrics success(String templateId, String templateVersion, String datasource, long buildTimeMs, long executeTimeMs, int rowCount) {
        QueryMetrics m = new QueryMetrics();
        m.templateId = templateId;
        m.templateVersion = templateVersion;
        m.datasource = datasource;
        m.buildTimeMs = buildTimeMs;
        m.executeTimeMs = executeTimeMs;
        m.rowCount = rowCount;
        m.success = true;
        return m;
    }

    public static QueryMetrics failure(String templateId, String templateVersion, String datasource, long buildTimeMs, String errorCode, String errorMessage) {
        QueryMetrics m = new QueryMetrics();
        m.templateId = templateId;
        m.templateVersion = templateVersion;
        m.datasource = datasource;
        m.buildTimeMs = buildTimeMs;
        m.executeTimeMs = 0;
        m.rowCount = 0;
        m.success = false;
        m.errorCode = errorCode;
        m.errorMessage = errorMessage;
        return m;
    }

    // 向后兼容方法（废弃但保留）
    @Deprecated
    public static QueryMetrics success(String templateId, String datasource, long buildTimeMs, long executeTimeMs, int rowCount) {
        return success(templateId, null, datasource, buildTimeMs, executeTimeMs, rowCount);
    }

    @Deprecated
    public static QueryMetrics failure(String templateId, String datasource, long buildTimeMs, String errorCode, String errorMessage) {
        return failure(templateId, null, datasource, buildTimeMs, errorCode, errorMessage);
    }
}