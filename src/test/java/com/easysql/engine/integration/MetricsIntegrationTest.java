package com.easysql.engine.integration;

import com.easysql.engine.EasySQLEngine;
import com.easysql.engine.dsl.Query;
import com.easysql.engine.executor.JDBCSQLExecutor;
import com.easysql.engine.model.Template;
import com.easysql.engine.monitor.MetricsCollector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static com.easysql.engine.dsl.WhereClause.*;

/**
 * 监控指标系统集成测试
 * 测试Engine与Executor协同埋点、并发场景、异常处理等
 */
public class MetricsIntegrationTest {

    private EasySQLEngine engine;
    private JDBCSQLExecutor executor;
    private MetricsCollector metrics;
    private Connection connection;

    @BeforeEach
    public void setup() throws Exception {
        engine = new EasySQLEngine();
        metrics = engine.getMetrics();
        executor = engine.createExecutor(); // 使用共享的MetricsCollector
        
        // 建立内存数据库连接
        connection = DriverManager.getConnection("jdbc:h2:mem:metricsdb;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(100), status VARCHAR(20))");
            stmt.execute("INSERT INTO users VALUES (1, 'Alice', 'active'), (2, 'Bob', 'inactive')");
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (connection != null) connection.close();
    }

    @Test
    public void testEngineAndExecutorCooperativeMetrics() throws Exception {
        long totalBefore = metrics.getTotalQueries();
        long successBefore = metrics.getSuccessQueries();

        // 1. Engine构建SQL（会记录构建指标）
        Query q = Query.create("integration_test_1")
                .dialect("mysql")
                .from("users")
                .select("id", "name")
                .where(leaf("status", "=", val("active")));

        EasySQLEngine.QueryResult buildResult = engine.buildSQL(q);
        
        // 验证构建指标记录
        assertEquals(totalBefore + 1, metrics.getTotalQueries());
        assertEquals(successBefore + 1, metrics.getSuccessQueries());

        // 2. Executor执行SQL（会记录执行指标）
        Map<String, Object> params = new HashMap<>();
        executor.executeQuery(connection, buildResult.sql, buildResult.template, params);
        
        // 验证执行指标也被记录（总共2次：构建+执行）
        assertEquals(totalBefore + 2, metrics.getTotalQueries());
        assertEquals(successBefore + 2, metrics.getSuccessQueries());
        assertEquals(0, metrics.getFailedQueries()); // 应该没有失败
    }

    @Test
    public void testEngineFailureMetrics() {
        long totalBefore = metrics.getTotalQueries();
        long failedBefore = metrics.getFailedQueries();

        // 构造一个无效的Template导致Engine构建失败
        Template invalidTemplate = new Template();
        invalidTemplate.id = null; // 缺少必需的id字段

        try {
            engine.parseAndBuild(invalidTemplate);
            fail("Expected validation exception");
        } catch (IllegalArgumentException e) {
            // 预期的异常
        }

        // 验证失败指标被记录
        assertEquals(totalBefore + 1, metrics.getTotalQueries());
        assertEquals(failedBefore + 1, metrics.getFailedQueries());
    }

    @Test
    public void testExecutorFailureMetrics() throws Exception {
        long totalBefore = metrics.getTotalQueries();
        long failedBefore = metrics.getFailedQueries();

        // 构造一个语法错误的SQL
        Template template = new Template();
        template.id = "executor_fail_test";
        template.datasource = "test_db";

        try {
            executor.executeQuery(connection, "INVALID SQL SYNTAX", template, new HashMap<>());
            fail("Expected SQL exception");
        } catch (RuntimeException e) {
            // 预期的SQL异常
        }

        // 验证执行失败指标被记录
        assertEquals(totalBefore + 1, metrics.getTotalQueries());
        assertEquals(failedBefore + 1, metrics.getFailedQueries());
    }

    @Test
    public void testConcurrentMetricsAccuracy() throws Exception {
        int threadCount = 10;
        int operationsPerThread = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long totalBefore = metrics.getTotalQueries();
        long successBefore = metrics.getSuccessQueries();

        // 并发执行多个构建操作
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        Query q = Query.create("concurrent_test_" + threadId + "_" + j)
                                .dialect("mysql")
                                .from("users")
                                .select("id", "name");
                        engine.buildSQL(q);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executorService.shutdown();

        // 验证并发操作的指标准确性
        long expectedTotal = totalBefore + threadCount * operationsPerThread;
        assertEquals(expectedTotal, metrics.getTotalQueries());
        assertEquals(expectedTotal, metrics.getSuccessQueries()); // 所有操作应该成功
        assertEquals(0, metrics.getFailedQueries());
    }

    @Test
    public void testMixedSuccessAndFailureConcurrency() throws Exception {
        int threadCount = 8;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long totalBefore = metrics.getTotalQueries();
        long successBefore = metrics.getSuccessQueries();
        long failedBefore = metrics.getFailedQueries();

        // 一半线程执行成功操作，一半执行失败操作
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    if (threadId % 2 == 0) {
                        // 偶数线程：执行成功操作
                        Query q = Query.create("success_test_" + threadId)
                                .dialect("mysql")
                                .from("users")
                                .select("id", "name");
                        engine.buildSQL(q);
                    } else {
                        // 奇数线程：执行失败操作
                        Template invalidTemplate = new Template();
                        invalidTemplate.id = ""; // 空的id会导致校验失败
                        try {
                            engine.parseAndBuild(invalidTemplate);
                        } catch (Exception e) {
                            // 预期的异常，忽略
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executorService.shutdown();

        // 验证成功和失败的指标都被正确记录
        assertEquals(totalBefore + threadCount, metrics.getTotalQueries());
        assertEquals(successBefore + threadCount / 2, metrics.getSuccessQueries());
        assertEquals(failedBefore + threadCount / 2, metrics.getFailedQueries());
    }

    @Test
    public void testCompleteWorkflowMetrics() throws Exception {
        long totalBefore = metrics.getTotalQueries();

        // 完整工作流：DSL构建 -> Engine解析 -> Executor执行
        Query q = Query.create("workflow_test")
                .dialect("mysql")
                .from("users")
                .select("id", "name", "status")
                .where(leaf("id", ">", val("0")));

        // 1. Engine构建（记录构建指标）
        EasySQLEngine.QueryResult buildResult = engine.buildSQL(q);
        assertNotNull(buildResult.sql);
        assertTrue(buildResult.buildTimeMs >= 0);

        // 2. Executor执行（记录执行指标）
        Map<String, Object> params = new HashMap<>();
        executor.executeQuery(connection, buildResult.sql, buildResult.template, params);

        // 验证完整流程的指标记录：构建+执行=2次
        assertEquals(totalBefore + 2, metrics.getTotalQueries());
        assertEquals(totalBefore + 2, metrics.getSuccessQueries());
        assertEquals(0, metrics.getFailedQueries());
    }
}