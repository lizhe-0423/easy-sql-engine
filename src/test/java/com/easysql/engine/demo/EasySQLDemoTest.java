package com.easysql.engine.demo;

import com.easysql.engine.EasySQLEngine;
import com.easysql.engine.TemplateMapper;
import com.easysql.engine.executor.JDBCSQLExecutor;
import com.easysql.engine.executor.SQLExecutor;
import com.easysql.engine.model.Template;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 最小可执行演示：
 * - 读取 src/test/resources/template-example.json 模板
 * - 生成带命名参数的SQL
 * - 绑定参数并在H2内存库执行
 * - 打印SQL、参数、列名、首行数据，方便截图
 */
public class EasySQLDemoTest {

    @Test
    public void runDemo() throws Exception {
        // 1) 准备内存数据库与示例数据（H2/兼容MySQL语法）
        try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:demodb;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "")) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE SCHEMA IF NOT EXISTS test");
                st.execute("CREATE TABLE test.`user` (id INT PRIMARY KEY, name VARCHAR(100), email VARCHAR(200), status VARCHAR(20), created_at DATE)");
                st.execute("CREATE TABLE test.`profile` (id INT PRIMARY KEY, user_id INT, name VARCHAR(100))");
                st.execute("CREATE TABLE test.`orders` (id INT PRIMARY KEY, user_id INT, status VARCHAR(20))");
                st.execute("INSERT INTO test.`user` VALUES (1,'Alice','alice@example.com','active','2023-01-01'), (2,'Bob','bob@example.com','inactive','2023-02-01')");
                st.execute("INSERT INTO test.`profile` VALUES (10,1,'AliceProfile'), (20,2,'BobProfile')");
                st.execute("INSERT INTO test.`orders` VALUES (100,1,'completed'), (101,1,'pending'), (102,2,'completed')");
            }

            // 2) 加载模板并生成SQL
            Template template = TemplateMapper.fromResource("template-example.json");
            EasySQLEngine engine = new EasySQLEngine();
            String sql = engine.buildSQL(template);

            System.out.println("\n=== Easy-SQL Demo (Template -> SQL -> Execute) ===");
            System.out.println("Generated SQL:\n" + sql);

            // 3) 准备参数并执行
            Map<String, Object> params = new HashMap<>();
            params.put("start_date", Date.valueOf("2022-12-31"));
            System.out.println("Params: " + params);

            JDBCSQLExecutor executor = new JDBCSQLExecutor();
            SQLExecutor.QueryResult result = executor.executeQuery(conn, sql, template, params);

            // 4) 打印结果（便于截图）
            System.out.println("RowCount: " + result.getRowCount());
            System.out.println("Columns: " + result.getColumnNames());
            if (result.getRowCount() > 0) {
                System.out.println("First Row: " + result.getRows().get(0));
            }

            // 简单断言，保证演示可通过
            assertTrue(result.getRowCount() >= 1);
        }
    }
}