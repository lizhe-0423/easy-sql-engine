package com.easysql.engine.executor;

import com.easysql.engine.EasySQLEngine;
import com.easysql.engine.TemplateMapper;
import com.easysql.engine.model.Template;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JDBCSQLExecutorTest {

    private static Connection conn;

    @BeforeAll
    public static void setup() throws Exception {
        conn = DriverManager.getConnection("jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE SCHEMA IF NOT EXISTS test");
            st.execute("CREATE TABLE test.`user` (id INT PRIMARY KEY, name VARCHAR(100), email VARCHAR(200), status VARCHAR(20), created_at DATE)");
            st.execute("CREATE TABLE test.`profile` (id INT PRIMARY KEY, user_id INT, name VARCHAR(100))");
            st.execute("CREATE TABLE test.`orders` (id INT PRIMARY KEY, user_id INT, status VARCHAR(20))");
            st.execute("INSERT INTO test.`user` VALUES (1,'Alice','alice@example.com','active','2023-01-01'), (2,'Bob','bob@example.com','inactive','2023-02-01')");
            st.execute("INSERT INTO test.`profile` VALUES (10,1,'AliceProfile'), (20,2,'BobProfile')");
            st.execute("INSERT INTO test.`orders` VALUES (100,1,'completed'), (101,1,'pending'), (102,2,'completed')");
        }
    }

    @AfterAll
    public static void teardown() throws Exception {
        if (conn != null) conn.close();
    }

    @Test
    public void testExecuteQueryWithTemplate() throws Exception {
        Template t = TemplateMapper.fromResource("template-example.json");
        EasySQLEngine engine = new EasySQLEngine();
        String sql = engine.buildSQL(t);
        // 新增：打印生成的SQL
        System.out.println("\n=== Easy-SQL Demo ===");
        System.out.println("Generated SQL:\n" + sql);

        assertTrue(sql.contains(":start_date"));

        Map<String, Object> params = new HashMap<>();
        params.put("start_date", Date.valueOf("2022-12-31"));
        // 新增：打印参数
        System.out.println("Params: " + params);

        JDBCSQLExecutor executor = new JDBCSQLExecutor();
        SQLExecutor.QueryResult result = executor.executeQuery(conn, sql, t, params);
        assertNotNull(result);
        assertTrue(result.getRowCount() >= 1);
        assertFalse(result.getColumnNames().isEmpty());

        // 新增：打印结果统计与首行
        System.out.println("RowCount: " + result.getRowCount());
        System.out.println("Columns: " + result.getColumnNames());
        if (result.getRowCount() > 0) {
            System.out.println("First Row: " + result.getRows().get(0));
        }
    }
}