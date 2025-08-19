package com.easysql.engine.dsl;

import com.easysql.engine.EasySQLEngine;
import com.easysql.engine.monitor.MetricsCollector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static com.easysql.engine.dsl.WhereClause.*;

public class BuildFromQueryMethodTest {

    @Test
    public void testBuildSQLQueryMethodAndMetrics() {
        EasySQLEngine engine = new EasySQLEngine();
        MetricsCollector metrics = engine.getMetrics();
        long totalBefore = metrics.getTotalQueries();

        Query q = Query.create("dsl_build_method_1")
                .dialect("mysql")
                .from("users")
                .select("id", "name")
                .where(and(
                        leaf("status", "=", val("active"))
                ));

        EasySQLEngine.QueryResult result = engine.buildSQL(q);
        assertNotNull(result);
        assertNotNull(result.sql);
        assertTrue(result.sql.toLowerCase().contains("select"));
        assertTrue(result.sql.toLowerCase().contains("from"));
        assertTrue(result.buildTimeMs >= 0);
        assertNotNull(result.template);

        // 构建阶段应记录一次指标
        assertEquals(totalBefore + 1, metrics.getTotalQueries());
    }
}