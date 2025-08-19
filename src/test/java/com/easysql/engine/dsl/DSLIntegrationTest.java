package com.easysql.engine.dsl;

import com.easysql.engine.EasySQLEngine;
import com.easysql.engine.model.Template;
import com.easysql.engine.monitor.MetricsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static com.easysql.engine.dsl.WhereClause.*;
import static com.easysql.engine.dsl.SelectClause.*;

public class DSLIntegrationTest {

    private EasySQLEngine engine;
    private MetricsCollector metrics;

    @BeforeEach
    public void setUp() {
        engine = new EasySQLEngine();
        metrics = engine.getMetrics();
    }

    @Test
    public void testBasicDSLIntegration() {
        Template t = Query.create("dsl_integration_1")
                .dialect("mysql")
                .from("test", "user")
                .select("u.id", "u.name")
                .where(and(
                        leaf("u.status", "=", val("active")),
                        leaf("u.created_at", ">=", param("start_date"))
                ))
                .orderBy(OrderByClause.by("u.created_at").desc())
                .limit(50)
                .build();

        String sql = engine.buildSQL(t);

        assertNotNull(sql);
        assertTrue(sql.toLowerCase().contains("select"));
        assertTrue(sql.toLowerCase().contains("from"));
        assertTrue(sql.toLowerCase().contains("order by"));
        assertTrue(sql.toLowerCase().contains("limit 50"));
        assertTrue(sql.contains(":start_date"));
    }

    @Test
    public void testComplexQueryWithJoins() {
        Template t = Query.create("complex_join_query")
                .dialect("mysql")
                .from("test", "user", "u")
                .select(
                        expr("u.id").as("user_id"),
                        expr("u.name").as("user_name"),
                        expr("p.name").as("profile_name"),
                        expr("COUNT(o.id)").as("order_count")
                )
                .leftJoin("test.profile p", leaf("p.user_id", "=", val("u.id")))
                .innerJoin("test.orders o", and(
                        leaf("o.user_id", "=", val("u.id")),
                        leaf("o.status", "IN", val("('completed', 'shipped')"))
                ))
                .where(and(
                        leaf("u.status", "=", val("active")),
                        or(
                                leaf("u.email", "LIKE", param("email_pattern")),
                                leaf("u.name", "LIKE", param("name_pattern"))
                        )
                ))
                .groupBy("u.id", "u.name", "p.name")
                .having(leaf("COUNT(o.id)", ">", val("0")))
                .orderBy(
                        OrderByClause.by("order_count").desc(),
                        OrderByClause.by("u.name").asc()
                )
                .limit(20, 10)
                .param("email_pattern", "STRING")
                .param("name_pattern", "STRING")
                .build();

        String sql = engine.buildSQL(t);

        assertNotNull(sql);
        assertTrue(sql.toLowerCase().contains("left join"));
        assertTrue(sql.toLowerCase().contains("inner join"));
        assertTrue(sql.toLowerCase().contains("group by"));
        assertTrue(sql.toLowerCase().contains("having"));
        assertTrue(sql.toLowerCase().contains("count(o.id)"));
        assertTrue(sql.contains(":email_pattern"));
        assertTrue(sql.contains(":name_pattern"));
    }

    @Test
    public void testDSLWithComplexWhereConditions() {
        Template t = Query.create("complex_where_test")
                .from("user")
                .select("*")
                .where(and(
                        leaf("status", "=", val("active")),
                        or(
                                and(
                                        leaf("age", ">=", param("min_age")),
                                        leaf("age", "<=", param("max_age"))
                                ),
                                leaf("vip_level", "IN", val("('gold', 'platinum')"))
                        ),
                        not(leaf("email", "IS", val("NULL")))
                ))
                .param("min_age", "INTEGER")
                .param("max_age", "INTEGER")
                .build();

        String sql = engine.buildSQL(t);

        assertNotNull(sql);
        // 验证复杂条件组合
        assertTrue(sql.contains("AND"));
        assertTrue(sql.contains("OR"));
        assertTrue(sql.contains("NOT"));
        assertTrue(sql.contains(":min_age"));
        assertTrue(sql.contains(":max_age"));
    }

    @Test
    public void testDSLWithOptionsAndTimeouts() {
        Template t = Query.create("options_test")
                .from("large_table")
                .select("id", "name")
                .where(leaf("status", "=", val("pending")))
                .limit(1000)
                .options(OptionsClause.create()
                        .timeoutMs(30000)
                        .maxRows(5000)
                        .readOnly(true))
                .build();

        String sql = engine.buildSQL(t);

        assertNotNull(sql);
        assertNotNull(t.options);
        assertEquals(30000, t.options.timeoutMs);
        assertEquals(5000, t.options.maxRows);
        assertTrue(t.options.readOnly);
    }

    @Test
    public void testDSLErrorHandling_MissingRequiredFields() {
        // 测试缺少必填字段的错误处理
        assertThrows(IllegalArgumentException.class, () -> {
            Template t = Query.create()  // 没有设置ID
                    .select("id", "name")  // 没有设置FROM
                    .build();
            engine.buildSQL(t);
        });
    }

    @Test
    public void testDSLErrorHandling_InvalidLimit() {
        assertThrows(IllegalArgumentException.class, () -> {
            Template t = Query.create("invalid_limit_test")
                    .from("user")
                    .select("*")
                    .limit(-1)  // 非法的limit值
                    .build();
            engine.buildSQL(t);
        });
    }

    @Test
    public void testDSLErrorHandling_InvalidOffset() {
        assertThrows(IllegalArgumentException.class, () -> {
            Template t = Query.create("invalid_offset_test")
                    .from("user")
                    .select("*")
                    .offset(-5)  // 非法的offset值
                    .build();
            engine.buildSQL(t);
        });
    }

    @Test
    public void testDSLErrorHandling_InvalidTimeout() {
        assertThrows(IllegalArgumentException.class, () -> {
            Template t = Query.create("invalid_timeout_test")
                    .from("user")
                    .select("*")
                    .options(OptionsClause.create().timeoutMs(700000))  // 超过600秒的超时
                    .build();
            engine.buildSQL(t);
        });
    }

    @Test
    public void testDSLErrorHandling_UnsupportedDialect() {
        assertThrows(IllegalArgumentException.class, () -> {
            Template t = Query.create("unsupported_dialect")
                    .dialect("oracle")  // 不支持的方言
                    .from("user")
                    .select("*")
                    .build();
            engine.buildSQL(t);
        });
    }

    @Test
    public void testDSLPerformanceMetrics() {
        long initialSuccessCount = metrics.getSuccessQueries();

        Template t = Query.create("performance_test")
                .from("user")
                .select("id", "name", "email")
                .where(leaf("status", "=", param("status")))
                .param("status", "STRING")
                .build();

        long startTime = System.currentTimeMillis();
        EasySQLEngine.QueryResult result = engine.buildSQL(Query.create("performance_test")
                .from("user")
                .select("id", "name", "email")
                .where(leaf("status", "=", param("status")))
                .param("status", "STRING"));
        long endTime = System.currentTimeMillis();

        // 验证SQL构建成功
        assertNotNull(result.sql);
        assertEquals("performance_test", result.template.id);

        // 验证性能指标
        assertTrue(result.buildTimeMs >= 0);
        assertTrue(result.buildTimeMs < 1000); // 构建时间应该很快
        assertEquals(initialSuccessCount + 1, metrics.getSuccessQueries());

        // 验证实际构建时间
        long actualBuildTime = endTime - startTime;
        assertTrue(actualBuildTime < 100, "DSL构建时间过长: " + actualBuildTime + "ms");
    }

    @Test
    public void testDSLConcurrentBuilding() throws InterruptedException {
        int threadCount = 10;
        int buildsPerThread = 5;
        long initialSuccessCount = metrics.getSuccessQueries();

        Thread[] threads = new Thread[threadCount];
        final Exception[] exceptions = new Exception[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < buildsPerThread; j++) {
                        Template t = Query.create("concurrent_test_" + threadId + "_" + j)
                                .from("user")
                                .select("id", "name")
                                .where(leaf("thread_id", "=", val(String.valueOf(threadId))))
                                .limit(10)
                                .build();
                        
                        String sql = engine.buildSQL(t);
                        assertNotNull(sql);
                        assertTrue(sql.contains(String.valueOf(threadId)));
                    }
                } catch (Exception e) {
                    exceptions[threadId] = e;
                }
            });
        }

        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // 检查是否有异常
        for (int i = 0; i < threadCount; i++) {
            if (exceptions[i] != null) {
                fail("Thread " + i + " failed with exception: " + exceptions[i].getMessage());
            }
        }

        // 验证指标统计正确性
        long expectedTotalSuccess = initialSuccessCount + (threadCount * buildsPerThread);
        assertEquals(expectedTotalSuccess, metrics.getSuccessQueries());
    }

    @Test
    public void testDSLWithSubquerySimulation() {
        // 通过复杂条件模拟子查询场景
        Template t = Query.create("subquery_simulation")
                .from("orders", "o")
                .select("o.id", "o.user_id", "o.total")
                .where(and(
                        leaf("o.status", "=", val("completed")),
                        leaf("o.user_id", "IN", val("(SELECT id FROM users WHERE vip_level = 'gold')")),
                        leaf("o.created_at", ">=", param("start_date"))
                ))
                .orderBy(OrderByClause.by("o.total").desc())
                .limit(100)
                .param("start_date", "DATE")
                .build();

        String sql = engine.buildSQL(t);

        assertNotNull(sql);
        assertTrue(sql.contains("SELECT id FROM users"));
        assertTrue(sql.contains("vip_level = 'gold'"));
        assertTrue(sql.contains(":start_date"));
    }
}