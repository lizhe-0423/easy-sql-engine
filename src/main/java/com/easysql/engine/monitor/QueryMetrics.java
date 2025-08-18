package com.easysql.engine.monitor;

/**
 * 查询指标数据
 */
public class QueryMetrics {
    public String templateId;
    public String datasource;
    public long buildTimeMs;
    public long executeTimeMs;
    public int rowCount;
    public boolean success;
    public String errorCode;
    public String errorMessage;

    public static QueryMetrics success(String templateId, String datasource, long buildTimeMs, long executeTimeMs, int rowCount) {
        QueryMetrics m = new QueryMetrics();
        m.templateId = templateId;
        m.datasource = datasource;
        m.buildTimeMs = buildTimeMs;
        m.executeTimeMs = executeTimeMs;
        m.rowCount = rowCount;
        m.success = true;
        return m;
    }

    public static QueryMetrics failure(String templateId, String datasource, long buildTimeMs, String errorCode, String errorMessage) {
        QueryMetrics m = new QueryMetrics();
        m.templateId = templateId;
        m.datasource = datasource;
        m.buildTimeMs = buildTimeMs;
        m.executeTimeMs = 0;
        m.rowCount = 0;
        m.success = false;
        m.errorCode = errorCode;
        m.errorMessage = errorMessage;
        return m;
    }
}