package com.easysql.engine.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 指标收集器（基础实现：日志埋点 + 内存计数），后续可替换为Micrometer
 */
public class MetricsCollector {
    private static final Logger log = LoggerFactory.getLogger(MetricsCollector.class);

    private final AtomicLong totalQueries = new AtomicLong();
    private final AtomicLong successQueries = new AtomicLong();
    private final AtomicLong failedQueries = new AtomicLong();

    public void record(QueryMetrics m) {
        totalQueries.incrementAndGet();
        if (m.success) successQueries.incrementAndGet(); else failedQueries.incrementAndGet();
        // 简单日志输出，避免泄漏SQL，仅输出模板ID与耗时
        if (m.success) {
            log.info("[metrics] template={} version={} datasource={} buildTimeMs={} executeTimeMs={} rows={} success=true",
                    m.templateId, m.templateVersion, m.datasource, m.buildTimeMs, m.executeTimeMs, m.rowCount);
        } else {
            log.warn("[metrics] template={} version={} datasource={} buildTimeMs={} success=false errorCode={} errorMsg={}",
                    m.templateId, m.templateVersion, m.datasource, m.buildTimeMs, m.errorCode, m.errorMessage);
        }
    }

    public long getTotalQueries() { return totalQueries.get(); }
    public long getSuccessQueries() { return successQueries.get(); }
    public long getFailedQueries() { return failedQueries.get(); }
}