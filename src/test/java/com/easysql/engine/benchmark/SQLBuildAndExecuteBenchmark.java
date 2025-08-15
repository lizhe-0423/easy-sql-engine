package com.easysql.engine.benchmark;

import com.easysql.engine.EasySQLEngine;
import com.easysql.engine.TemplateMapper;
import com.easysql.engine.executor.JDBCSQLExecutor;
import com.easysql.engine.executor.SQLExecutor;
import com.easysql.engine.model.Template;
import org.openjdk.jmh.annotations.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class SQLBuildAndExecuteBenchmark {

    private EasySQLEngine engine;
    private Template template;
    private Connection conn;
    private JDBCSQLExecutor executor;
    private Map<String,Object> params;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        engine = new EasySQLEngine();
        template = TemplateMapper.fromResource("template-example.json");
        executor = new JDBCSQLExecutor();
        conn = DriverManager.getConnection("jdbc:h2:mem:bmdb;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE SCHEMA IF NOT EXISTS test");
            st.execute("CREATE TABLE test.`user` (id INT PRIMARY KEY, name VARCHAR(100), email VARCHAR(200), status VARCHAR(20), created_at DATE)");
            st.execute("CREATE TABLE test.`profile` (id INT PRIMARY KEY, user_id INT, name VARCHAR(100))");
            st.execute("CREATE TABLE test.`orders` (id INT PRIMARY KEY, user_id INT, status VARCHAR(20))");
            st.execute("INSERT INTO test.`user` VALUES (1,'Alice','alice@example.com','active','2023-01-01'), (2,'Bob','bob@example.com','inactive','2023-02-01')");
            st.execute("INSERT INTO test.`profile` VALUES (10,1,'AliceProfile'), (20,2,'BobProfile')");
            st.execute("INSERT INTO test.`orders` VALUES (100,1,'completed'), (101,1,'pending'), (102,2,'completed')");
        }
        params = new HashMap<>();
        params.put("start_date", Date.valueOf("2022-12-31"));
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        if (conn != null) conn.close();
    }

    @Benchmark
    public String benchmarkBuildSQL() {
        return engine.buildSQL(template);
    }

    @Benchmark
    public SQLExecutor.QueryResult benchmarkExecuteQuery() throws Exception {
        String sql = engine.buildSQL(template);
        return executor.executeQuery(conn, sql, template, params);
    }
}