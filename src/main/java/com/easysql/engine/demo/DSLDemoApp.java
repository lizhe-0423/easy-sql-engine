package com.easysql.engine.demo;

import com.easysql.engine.EasySQLEngine;
import com.easysql.engine.dsl.*;
import com.easysql.engine.monitor.MetricsCollector;

import static com.easysql.engine.dsl.WhereClause.*;

/**
 * DSL演示应用程序
 * 展示EasySQL引擎的DSL构建和性能指标输出功能
 */
public class DSLDemoApp {

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("   EasySQL引擎 DSL演示应用程序 v2.0");
        System.out.println("==========================================\n");

        // 创建引擎实例
        EasySQLEngine engine = new EasySQLEngine();
        MetricsCollector metrics = engine.getMetrics();

        try {
            // 示例1：简单查询
            demonstrateSimpleQuery(engine, metrics);

            // 示例2：复杂JOIN查询
            demonstrateComplexJoinQuery(engine, metrics);

            // 示例3：聚合分组查询
            demonstrateGroupByQuery(engine, metrics);

            // 示例4：带参数的查询
            demonstrateParameterizedQuery(engine, metrics);

            // 示例5：高级选项查询
            demonstrateAdvancedOptionsQuery(engine, metrics);

            // 示例6：错误处理演示
            demonstrateErrorHandling(engine, metrics);

            // 输出最终性能指标统计
            displayFinalMetrics(metrics);

        } catch (Exception e) {
            System.err.println("❌ 演示过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 演示简单查询
     */
    private static void demonstrateSimpleQuery(EasySQLEngine engine, MetricsCollector metrics) {
        System.out.println("🔸 示例1：简单查询演示");
        System.out.println("----------------------------------------");

        // 构建简单查询
        Query query = Query.create("simple_demo")
                .from("users")
                .select("id", "name", "email")
                .where(and(
                        leaf("status", "=", val("active")),
                        leaf("created_at", ">=", val("'2024-01-01'"))
                ))
                .orderBy(OrderByClause.by("name").asc())
                .limit(10);

        // 执行构建
        EasySQLEngine.QueryResult result = engine.buildSQL(query);

        // 输出结果
        System.out.println("📋 生成的SQL:");
        System.out.println(result.sql);
        System.out.println("⏱️  构建耗时: " + result.buildTimeMs + "ms");
        System.out.println("📊 当前指标 - 总查询: " + metrics.getTotalQueries() + 
                         ", 成功: " + metrics.getSuccessQueries() + 
                         ", 失败: " + metrics.getFailedQueries());
        System.out.println();
    }

    /**
     * 演示复杂JOIN查询
     */
    private static void demonstrateComplexJoinQuery(EasySQLEngine engine, MetricsCollector metrics) {
        System.out.println("🔸 示例2：复杂JOIN查询演示");
        System.out.println("----------------------------------------");

        Query query = Query.create("complex_join_demo")
                .dialect("mysql")
                .from(FromClause.table("test", "users").alias("u"))
                .select(
                        SelectClause.expr("u.id").as("user_id"),
                        SelectClause.expr("u.name").as("user_name"),
                        SelectClause.expr("p.level").as("user_level"),
                        SelectClause.expr("COUNT(o.id)").as("order_count"),
                        SelectClause.expr("SUM(o.amount)").as("total_amount")
                )
                .join(JoinClause.left("test.profiles").alias("p").on(
                        leaf("u.id", "=", val("p.user_id"))
                ))
                .join(JoinClause.inner("test.orders").alias("o").on(
                        and(
                                leaf("u.id", "=", val("o.user_id")),
                                leaf("o.status", "=", val("'completed'"))
                        )
                ))
                .where(and(
                        leaf("u.status", "=", val("'active'")),
                        leaf("u.created_at", ">=", val("'2024-01-01'")),
                        or(
                                leaf("p.level", "=", val("'VIP'")),
                                leaf("o.amount", ">", val("1000"))
                        )
                ))
                .groupBy("u.id", "u.name", "p.level")
                .having(leaf("COUNT(o.id)", ">", val("0")))
                .orderBy(
                        OrderByClause.by("total_amount").desc(),
                        OrderByClause.by("user_name").asc()
                )
                .limit(20, 10);

        EasySQLEngine.QueryResult result = engine.buildSQL(query);

        System.out.println("📋 生成的SQL:");
        System.out.println(result.sql);
        System.out.println("⏱️  构建耗时: " + result.buildTimeMs + "ms");
        System.out.println("📊 当前指标 - 总查询: " + metrics.getTotalQueries() + 
                         ", 成功: " + metrics.getSuccessQueries() + 
                         ", 失败: " + metrics.getFailedQueries());
        System.out.println();
    }

    /**
     * 演示聚合分组查询
     */
    private static void demonstrateGroupByQuery(EasySQLEngine engine, MetricsCollector metrics) {
        System.out.println("🔸 示例3：聚合分组查询演示");
        System.out.println("----------------------------------------");

        Query query = Query.create("group_by_demo")
                .from("orders", "o")
                .select(
                        SelectClause.expr("DATE(o.created_at)").as("order_date"),
                        SelectClause.expr("o.region").as("region"),
                        SelectClause.expr("COUNT(*)").as("order_count"),
                        SelectClause.expr("SUM(o.amount)").as("total_revenue"),
                        SelectClause.expr("AVG(o.amount)").as("avg_order_value")
                )
                .where(and(
                        leaf("o.status", "IN", val("('completed', 'shipped')")),
                        leaf("o.created_at", ">=", val("'2024-01-01'"))
                ))
                .groupBy("DATE(o.created_at)", "o.region")
                .having(leaf("COUNT(*)", ">", val("10")))
                .orderBy(
                        OrderByClause.by("order_date").desc(),
                        OrderByClause.by("total_revenue").desc()
                )
                .limit(100);

        EasySQLEngine.QueryResult result = engine.buildSQL(query);

        System.out.println("📋 生成的SQL:");
        System.out.println(result.sql);
        System.out.println("⏱️  构建耗时: " + result.buildTimeMs + "ms");
        System.out.println("📊 当前指标 - 总查询: " + metrics.getTotalQueries() + 
                         ", 成功: " + metrics.getSuccessQueries() + 
                         ", 失败: " + metrics.getFailedQueries());
        System.out.println();
    }

    /**
     * 演示带参数的查询
     */
    private static void demonstrateParameterizedQuery(EasySQLEngine engine, MetricsCollector metrics) {
        System.out.println("🔸 示例4：参数化查询演示");
        System.out.println("----------------------------------------");

        Query query = Query.create("parameterized_demo")
                .from("products", "p")
                .select("p.id", "p.name", "p.price", "p.category")
                .where(and(
                        leaf("p.price", "BETWEEN", param("min_price")),
                        leaf("p.category", "=", param("target_category")),
                        leaf("p.status", "=", val("'active'")),
                        leaf("p.created_at", ">=", param("start_date"))
                ))
                .orderBy(OrderByClause.by("p.price").asc())
                .limit(50)
                .param("min_price", "DECIMAL")
                .param("target_category", "STRING")
                .param("start_date", "DATE");

        EasySQLEngine.QueryResult result = engine.buildSQL(query);

        System.out.println("📋 生成的SQL:");
        System.out.println(result.sql);
        System.out.println("📋 参数定义:");
        if (result.template.params != null) {
            for (com.easysql.engine.model.Template.Param param : result.template.params) {
                System.out.println("  - " + param.name + " (" + param.type + ")" + 
                                 (param.required ? " [必需]" : " [可选]"));
            }
        }
        System.out.println("⏱️  构建耗时: " + result.buildTimeMs + "ms");
        System.out.println("📊 当前指标 - 总查询: " + metrics.getTotalQueries() + 
                         ", 成功: " + metrics.getSuccessQueries() + 
                         ", 失败: " + metrics.getFailedQueries());
        System.out.println();
    }

    /**
     * 演示高级选项查询
     */
    private static void demonstrateAdvancedOptionsQuery(EasySQLEngine engine, MetricsCollector metrics) {
        System.out.println("🔸 示例5：高级选项查询演示");
        System.out.println("----------------------------------------");

        Query query = Query.create("advanced_options_demo")
                .datasource("analytics_db")
                .dialect("mysql")
                .from("large_table", "lt")
                .select(
                        SelectClause.expr("lt.id").as("record_id"),
                        SelectClause.expr("lt.data").as("record_data"),
                        SelectClause.expr("lt.timestamp").as("event_time")
                )
                .where(and(
                        leaf("lt.partition_date", "=", val("'2024-01-15'")),
                        leaf("lt.status", "=", val("'processed'"))
                ))
                .orderBy(OrderByClause.by("lt.timestamp").desc())
                .limit(1000)
                .options(OptionsClause.create()
                        .timeoutMs(30000)         // 30秒超时
                        .maxRows(5000)            // 最大返回5000行
                        .readOnly(true)           // 只读查询
                        .scanPartitions(4)        // 扫描4个分区
                        .fetchSize(500)           // 批量获取500行
                        .hint("USE_INDEX(lt, idx_partition_status)")
                        .hint("FORCE INDEX(idx_timestamp)")
                );

        EasySQLEngine.QueryResult result = engine.buildSQL(query);

        System.out.println("📋 生成的SQL:");
        System.out.println(result.sql);
        System.out.println("📋 查询选项:");
        if (result.template.options != null) {
            com.easysql.engine.model.Template.Options opts = result.template.options;
            System.out.println("  - 数据源: " + result.template.datasource);
            System.out.println("  - 方言: " + result.template.dialect);
            System.out.println("  - 超时: " + opts.timeoutMs + "ms");
            System.out.println("  - 最大行数: " + opts.maxRows);
            System.out.println("  - 只读: " + opts.readOnly);
            System.out.println("  - 扫描分区: " + opts.scanPartitions);
            System.out.println("  - 批量大小: " + opts.fetchSize);
            if (opts.hints != null && !opts.hints.isEmpty()) {
                System.out.println("  - 优化提示: " + String.join(", ", opts.hints));
            }
        }
        System.out.println("⏱️  构建耗时: " + result.buildTimeMs + "ms");
        System.out.println("📊 当前指标 - 总查询: " + metrics.getTotalQueries() + 
                         ", 成功: " + metrics.getSuccessQueries() + 
                         ", 失败: " + metrics.getFailedQueries());
        System.out.println();
    }

    /**
     * 演示错误处理
     */
    private static void demonstrateErrorHandling(EasySQLEngine engine, MetricsCollector metrics) {
        System.out.println("🔸 示例6：错误处理演示");
        System.out.println("----------------------------------------");

        try {
            // 故意创建一个有问题的查询：没有设置FROM
            Query invalidQuery = Query.create("error_demo")
                    .select("id", "name")  // 没有FROM子句
                    .limit(-1);            // 非法的limit值

            EasySQLEngine.QueryResult result = engine.buildSQL(invalidQuery);
            System.out.println("⚠️ 意外成功，这可能是个bug");

        } catch (Exception e) {
            System.out.println("✅ 成功捕获预期错误: " + e.getMessage());
        }

        // 再尝试一个超时值过大的查询
        try {
            Query timeoutQuery = Query.create("timeout_error_demo")
                    .from("users")
                    .select("*")
                    .options(OptionsClause.create().timeoutMs(700000)); // 超过600秒

            EasySQLEngine.QueryResult result = engine.buildSQL(timeoutQuery);
            System.out.println("⚠️ 超时验证可能未生效");

        } catch (Exception e) {
            System.out.println("✅ 成功捕获超时错误: " + e.getMessage());
        }

        System.out.println("📊 当前指标 - 总查询: " + metrics.getTotalQueries() + 
                         ", 成功: " + metrics.getSuccessQueries() + 
                         ", 失败: " + metrics.getFailedQueries());
        System.out.println();
    }

    /**
     * 显示最终指标统计
     */
    private static void displayFinalMetrics(MetricsCollector metrics) {
        System.out.println("==========================================");
        System.out.println("             最终性能指标统计");
        System.out.println("==========================================");
        System.out.println("📊 总查询数: " + metrics.getTotalQueries());
        System.out.println("✅ 成功查询: " + metrics.getSuccessQueries());
        System.out.println("❌ 失败查询: " + metrics.getFailedQueries());
        
        if (metrics.getTotalQueries() > 0) {
            double successRate = (double) metrics.getSuccessQueries() / metrics.getTotalQueries() * 100;
            System.out.printf("📈 成功率: %.1f%%\n", successRate);
        }
        
        System.out.println("\n🎉 DSL演示完成！");
        System.out.println("==========================================");
    }
}