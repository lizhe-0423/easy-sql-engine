package com.easysql.engine.benchmark;

import com.easysql.engine.EasySQLEngine;
import com.easysql.engine.dsl.*;
import com.easysql.engine.model.Template;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * DSL构建基准测试，评估不同复杂度模板的构建性能
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class DSLBenchmark {

    private EasySQLEngine engine;

    @Setup(Level.Trial)
    public void setup() {
        engine = new EasySQLEngine();
    }

    /**
     * 简单查询基准：单表查询，少量条件
     */
    @Benchmark
    public EasySQLEngine.QueryResult benchmarkSimpleQuery() {
        Query query = Query.create("simple_query")
                .from("users")
                .select("id", "name", "email")
                .where(WhereClause.and(
                        WhereClause.leaf("status", "=", WhereClause.val("active")),
                        WhereClause.leaf("created_at", ">=", WhereClause.param("start_date"))
                ))
                .orderBy(OrderByClause.by("name").asc())
                .limit(100);
        
        return engine.buildSQL(query);
    }

    /**
     * 复杂查询基准：多表JOIN，复杂WHERE条件，GROUP BY/HAVING
     */
    @Benchmark
    public EasySQLEngine.QueryResult benchmarkComplexQuery() {
        Query query = Query.create("complex_query")
                .from(FromClause.table("test", "users").alias("u"))
                .select(
                        SelectClause.expr("u.id").as("user_id"),
                        SelectClause.expr("u.name").as("user_name"),
                        SelectClause.expr("COUNT(o.id)").as("order_count"),
                        SelectClause.expr("SUM(o.amount)").as("total_amount")
                )
                .join(JoinClause.left("test.orders").alias("o").on(
                        WhereClause.leaf("u.id", "=", WhereClause.val("o.user_id"))
                ))
                .join(JoinClause.inner("test.profiles").alias("p").on(
                        WhereClause.leaf("u.id", "=", WhereClause.val("p.user_id"))
                ))
                .where(WhereClause.and(
                        WhereClause.leaf("u.status", "=", WhereClause.val("active")),
                        WhereClause.or(
                                WhereClause.leaf("p.level", "=", WhereClause.val("VIP")),
                                WhereClause.leaf("o.amount", ">", WhereClause.param("min_amount"))
                        ),
                        WhereClause.not(
                                WhereClause.leaf("u.email", "LIKE", WhereClause.val("%test%"))
                        )
                ))
                .groupBy("u.id", "u.name")
                .having(WhereClause.leaf("COUNT(o.id)", ">", WhereClause.val("5")))
                .orderBy(
                        OrderByClause.by("total_amount").desc(),
                        OrderByClause.by("user_name").asc()
                )
                .limit(50, 0)
                .options(OptionsClause.create()
                        .timeoutMs(30000)
                        .maxRows(1000)
                        .readOnly(true)
                        .hint("USE_INDEX")
                );
        
        return engine.buildSQL(query);
    }

    /**
     * 中等复杂度查询基准：单JOIN，中等条件
     */
    @Benchmark
    public EasySQLEngine.QueryResult benchmarkMediumQuery() {
        Query query = Query.create("medium_query")
                .from("users", "u")
                .select("u.id", "u.name", "p.profile_name")
                .leftJoin("profiles p", WhereClause.leaf("u.id", "=", WhereClause.val("p.user_id")))
                .where(WhereClause.and(
                        WhereClause.leaf("u.status", "IN", WhereClause.param("statuses")),
                        WhereClause.leaf("u.created_at", "BETWEEN", WhereClause.param("date_range"))
                ))
                .orderBy(OrderByClause.by("u.created_at").desc())
                .limit(200);
        
        return engine.buildSQL(query);
    }

    /**
     * 超复杂查询基准：多层嵌套条件，多个JOIN，复杂聚合
     */
    @Benchmark
    public EasySQLEngine.QueryResult benchmarkVeryComplexQuery() {
        Query query = Query.create("very_complex_query")
                .datasource("analytics_db")
                .dialect("mysql")
                .from(FromClause.table("analytics", "users").alias("u"))
                .select(
                        SelectClause.expr("u.id").as("user_id"),
                        SelectClause.expr("u.name").as("user_name"),
                        SelectClause.expr("p.level").as("user_level"),
                        SelectClause.expr("COUNT(DISTINCT o.id)").as("unique_orders"),
                        SelectClause.expr("AVG(o.amount)").as("avg_order_amount"),
                        SelectClause.expr("MAX(o.created_at)").as("last_order_date"),
                        SelectClause.expr("SUM(CASE WHEN o.status = 'completed' THEN o.amount ELSE 0 END)").as("completed_revenue")
                )
                .join(JoinClause.left("analytics.profiles").alias("p").on(
                        WhereClause.leaf("u.id", "=", WhereClause.val("p.user_id"))
                ))
                .join(JoinClause.left("analytics.orders").alias("o").on(
                        WhereClause.and(
                                WhereClause.leaf("u.id", "=", WhereClause.val("o.user_id")),
                                WhereClause.leaf("o.created_at", ">=", WhereClause.param("analysis_start_date"))
                        )
                ))
                .join(JoinClause.left("analytics.order_items").alias("oi").on(
                        WhereClause.leaf("o.id", "=", WhereClause.val("oi.order_id"))
                ))
                .where(WhereClause.and(
                        WhereClause.leaf("u.status", "=", WhereClause.val("active")),
                        WhereClause.or(
                                WhereClause.and(
                                        WhereClause.leaf("p.level", "IN", WhereClause.param("premium_levels")),
                                        WhereClause.leaf("u.created_at", ">=", WhereClause.param("premium_cutoff_date"))
                                ),
                                WhereClause.and(
                                        WhereClause.leaf("o.amount", ">", WhereClause.param("high_value_threshold")),
                                        WhereClause.leaf("o.status", "=", WhereClause.val("completed"))
                                )
                        ),
                        WhereClause.not(
                                WhereClause.or(
                                        WhereClause.leaf("u.email", "LIKE", WhereClause.val("%test%")),
                                        WhereClause.leaf("u.email", "LIKE", WhereClause.val("%demo%"))
                                )
                        ),
                        WhereClause.leaf("u.region", "=", WhereClause.param("target_region"))
                ))
                .groupBy("u.id", "u.name", "p.level")
                .having(WhereClause.and(
                        WhereClause.leaf("COUNT(DISTINCT o.id)", ">", WhereClause.param("min_order_count")),
                        WhereClause.leaf("SUM(o.amount)", ">", WhereClause.param("min_total_amount"))
                ))
                .orderBy(
                        OrderByClause.by("completed_revenue").desc(),
                        OrderByClause.by("unique_orders").desc(),
                        OrderByClause.by("user_name").asc()
                )
                .limit(100, 0)
                .options(OptionsClause.create()
                        .timeoutMs(60000)
                        .maxRows(5000)
                        .readOnly(true)
                        .scanPartitions(8)
                        .fetchSize(1000)
                        .hint("USE_INDEX(u, idx_status_region)")
                        .hint("USE_INDEX(o, idx_user_created)")
                );
        
        return engine.buildSQL(query);
    }

    /**
     * 仅DSL构建基准（不包含引擎处理）：评估纯DSL API性能
     */
    @Benchmark
    public Template benchmarkDSLBuildOnly() {
        Query query = Query.create("dsl_only_test")
                .from("test", "users")
                .select("id", "name", "email")
                .where(WhereClause.and(
                        WhereClause.leaf("status", "=", WhereClause.val("active")),
                        WhereClause.leaf("created_at", ">=", WhereClause.param("start_date"))
                ))
                .orderBy(OrderByClause.by("u.created_at").desc())
                .limit(100);
        
        return query.build();
    }
}