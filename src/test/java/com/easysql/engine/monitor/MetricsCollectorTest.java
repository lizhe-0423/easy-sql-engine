package com.easysql.engine.monitor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 监控指标系统测试
 */
public class MetricsCollectorTest {

    @Test
    public void testRecordSuccessMetrics() {
        MetricsCollector collector = new MetricsCollector();
        
        // 初始状态
        assertEquals(0, collector.getTotalQueries());
        assertEquals(0, collector.getSuccessQueries());
        assertEquals(0, collector.getFailedQueries());
        
        // 记录成功指标
        QueryMetrics success = QueryMetrics.success("test_template", "test_db", 50, 100, 5);
        collector.record(success);
        
        assertEquals(1, collector.getTotalQueries());
        assertEquals(1, collector.getSuccessQueries());
        assertEquals(0, collector.getFailedQueries());
    }

    @Test
    public void testRecordFailureMetrics() {
        MetricsCollector collector = new MetricsCollector();
        
        // 记录失败指标
        QueryMetrics failure = QueryMetrics.failure("test_template", "test_db", 25, "SQL_ERROR", "Invalid syntax");
        collector.record(failure);
        
        assertEquals(1, collector.getTotalQueries());
        assertEquals(0, collector.getSuccessQueries());
        assertEquals(1, collector.getFailedQueries());
    }

    @Test
    public void testMixedMetrics() {
        MetricsCollector collector = new MetricsCollector();
        
        // 记录多个成功和失败
        collector.record(QueryMetrics.success("t1", "db1", 10, 20, 3));
        collector.record(QueryMetrics.success("t2", "db1", 15, 25, 7));
        collector.record(QueryMetrics.failure("t3", "db2", 5, "TIMEOUT", "Query timeout"));
        collector.record(QueryMetrics.success("t4", "db2", 12, 18, 2));
        
        assertEquals(4, collector.getTotalQueries());
        assertEquals(3, collector.getSuccessQueries());
        assertEquals(1, collector.getFailedQueries());
    }

    @Test
    public void testQueryMetricsFactory() {
        // 测试成功指标构造
        QueryMetrics success = QueryMetrics.success("template1", "datasource1", 100, 200, 10);
        assertEquals("template1", success.templateId);
        assertEquals("datasource1", success.datasource);
        assertEquals(100, success.buildTimeMs);
        assertEquals(200, success.executeTimeMs);
        assertEquals(10, success.rowCount);
        assertTrue(success.success);
        assertNull(success.errorCode);
        assertNull(success.errorMessage);
        
        // 测试失败指标构造
        QueryMetrics failure = QueryMetrics.failure("template2", "datasource2", 50, "PARSE_ERROR", "Invalid JSON");
        assertEquals("template2", failure.templateId);
        assertEquals("datasource2", failure.datasource);
        assertEquals(50, failure.buildTimeMs);
        assertEquals(0, failure.executeTimeMs);
        assertEquals(0, failure.rowCount);
        assertFalse(failure.success);
        assertEquals("PARSE_ERROR", failure.errorCode);
        assertEquals("Invalid JSON", failure.errorMessage);
    }
}