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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 最小可执行演示：展示 Easy-SQL 完整流程
 * - 读取 template-example.json 模板
 * - 生成带命名参数的SQL
 * - 绑定参数并在H2内存库执行
 * - 美观输出SQL、参数、结果，方便截图
 */
public class EasySQLDemoM2Test {

    @Test
    public void runDemo() throws Exception {
        printHeader();

        // 1) 准备内存数据库与示例数据（H2/兼容MySQL语法）
        try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:demodb;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "")) {
            setupTestData(conn);

            // 2) 加载模板并生成SQL
            Template template = TemplateMapper.fromResource("template-example.json");
            EasySQLEngine engine = new EasySQLEngine();
            String sql = engine.buildSQL(template);

            printSQLSection(template, sql);

            // 3) 准备参数并执行
            Map<String, Object> params = new HashMap<>();
            params.put("start_date", Date.valueOf("2022-12-31"));

            printParametersSection(params);

            JDBCSQLExecutor executor = new JDBCSQLExecutor();
            SQLExecutor.QueryResult result = executor.executeQuery(conn, sql, template, params);

            // 4) 美观打印结果
            printResultsSection(result);

            // 简单断言，保证演示可通过
            assertTrue(result.getRowCount() >= 1);

            printFooter();
        }
    }

    private void printHeader() {
        System.out.println("\n" + repeat("=", 80));
        System.out.println("                     Easy-SQL Engine Demo");
        System.out.println("              JSON模板 → SQL生成 → 参数绑定 → 执行查询");
        System.out.println(repeat("=", 80));
    }

    private void setupTestData(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE SCHEMA IF NOT EXISTS test");
            st.execute("CREATE TABLE test.`user` (id INT PRIMARY KEY, name VARCHAR(100), email VARCHAR(200), status VARCHAR(20), created_at DATE)");
            st.execute("CREATE TABLE test.`profile` (id INT PRIMARY KEY, user_id INT, name VARCHAR(100))");
            st.execute("CREATE TABLE test.`orders` (id INT PRIMARY KEY, user_id INT, status VARCHAR(20))");

            // 增加更多测试数据
            st.execute("INSERT INTO test.`user` VALUES " +
                    "(1,'Alice','alice@example.com','active','2023-01-01'), " +
                    "(2,'Bob','bob@example.com','inactive','2023-02-01'), " +
                    "(3,'Charlie','charlie@example.com','active','2023-01-15'), " +
                    "(4,'Diana','diana@example.com','active','2023-02-10')");

            st.execute("INSERT INTO test.`profile` VALUES " +
                    "(10,1,'AliceProfile'), " +
                    "(20,2,'BobProfile'), " +
                    "(30,3,'CharlieProfile'), " +
                    "(40,4,'DianaProfile')");

            st.execute("INSERT INTO test.`orders` VALUES " +
                    "(100,1,'completed'), " +
                    "(101,1,'pending'), " +
                    "(102,2,'completed'), " +
                    "(103,3,'completed'), " +
                    "(104,4,'pending')");
        }
    }

    private void printSQLSection(Template template, String sql) {
        System.out.println("\n📄 模板信息:");
        System.out.println(repeat("-", 50));
        System.out.println("  模板ID: " + template.id);
        System.out.println("  数据源: " + template.datasource);
        System.out.println("  方言: " + template.dialect);

        System.out.println("\n🔧 生成的SQL:");
        System.out.println(repeat("-", 50));
        System.out.println(sql);
    }

    private void printParametersSection(Map<String, Object> params) {
        System.out.println("\n📋 参数绑定:");
        System.out.println(repeat("-", 50));
        params.forEach((key, value) ->
                System.out.printf("  %-15s: %s (%s)%n", key, value, value.getClass().getSimpleName()));
    }

    private void printResultsSection(SQLExecutor.QueryResult result) {
        System.out.println("\n📊 执行结果:");
        System.out.println(repeat("-", 50));
        System.out.printf("  查询行数: %d 行%n", result.getRowCount());
        System.out.printf("  执行耗时: %d ms%n", result.getExecutionTimeMs());
        System.out.printf("  列数量: %d 列%n", result.getColumnNames().size());

        System.out.println("\n📋 列信息:");
        System.out.println(repeat("-", 50));
        List<String> columns = result.getColumnNames();
        for (int i = 0; i < columns.size(); i++) {
            System.out.printf("  [%d] %s%n", i + 1, columns.get(i));
        }

        if (result.getRowCount() > 0) {
            System.out.println("\n📄 数据展示:");
            System.out.println(repeat("-", 80));

            // 表头
            System.out.print("| ");
            for (String col : columns) {
                System.out.printf("%-15s | ", col);
            }
            System.out.println();
            System.out.println("|" + repeat("-", 19 * columns.size() - 1) + "|");

            // 数据行（显示前3行）
            List<Map<String, Object>> rows = result.getRows();
            int displayRows = Math.min(3, rows.size());
            for (int i = 0; i < displayRows; i++) {
                Map<String, Object> row = rows.get(i);
                System.out.print("| ");
                for (String col : columns) {
                    Object value = row.get(col);
                    String displayValue = value != null ? value.toString() : "NULL";
                    if (displayValue.length() > 15) {
                        displayValue = displayValue.substring(0, 12) + "...";
                    }
                    System.out.printf("%-15s | ", displayValue);
                }
                System.out.println();
            }

            if (rows.size() > 3) {
                System.out.printf("... 还有 %d 行数据%n", rows.size() - 3);
            }
        }
    }

    private void printFooter() {
        System.out.println("\n" + repeat("=", 80));
        System.out.println("✅ Easy-SQL Demo 执行完成！");
        System.out.println(repeat("=", 80));
    }

    private static String repeat(String s, int n) {
        if (n <= 0) return "";
        StringBuilder sb = new StringBuilder(s.length() * n);
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }
}