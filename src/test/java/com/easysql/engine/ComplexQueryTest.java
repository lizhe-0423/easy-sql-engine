package com.easysql.engine;

import com.easysql.engine.model.Template;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ComplexQueryTest {

    @Test
    public void testComplexJoinWithNestedWhere() throws Exception {
        String json = "{\n" +
                "  \"id\": \"complex_query\",\n" +
                "  \"dialect\": \"mysql\",\n" +
                "  \"select\": [\n" +
                "    {\"expr\": \"u.id\", \"alias\": \"user_id\"},\n" +
                "    {\"expr\": \"u.name\", \"alias\": \"user_name\"},\n" +
                "    {\"expr\": \"p.name\", \"alias\": \"profile_name\"},\n" +
                "    {\"expr\": \"COUNT(o.id)\", \"alias\": \"order_count\"}\n" +
                "  ],\n" +
                "  \"from\": {\"schema\": \"test\", \"table\": \"user\", \"alias\": \"u\"},\n" +
                "  \"joins\": [\n" +
                "    {\n" +
                "      \"type\": \"left\",\n" +
                "      \"table\": {\"schema\": \"test\", \"table\": \"profile\", \"alias\": \"p\"},\n" +
                "      \"on\": [{\"left\": \"u.id\", \"operator\": \"=\", \"right\": {\"value\": \"p.user_id\"}}]\n" +
                "    },\n" +
                "    {\n" +
                "      \"type\": \"inner\",\n" +
                "      \"table\": {\"schema\": \"test\", \"table\": \"orders\", \"alias\": \"o\"},\n" +
                "      \"on\": [{\"left\": \"u.id\", \"operator\": \"=\", \"right\": {\"value\": \"o.user_id\"}}]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"where\": {\n" +
                "    \"op\": \"AND\",\n" +
                "    \"conditions\": [\n" +
                "      {\n" +
                "        \"leaf\": {\"left\": \"u.status\", \"operator\": \"=\", \"right\": {\"param\": \"user_status\"}}\n" +
                "      },\n" +
                "      {\n" +
                "        \"op\": \"OR\",\n" +
                "        \"conditions\": [\n" +
                "          {\"leaf\": {\"left\": \"u.created_at\", \"operator\": \">=\", \"right\": {\"param\": \"start_date\"}}},\n" +
                "          {\"leaf\": {\"left\": \"o.status\", \"operator\": \"=\", \"right\": {\"value\": \"completed\"}}}\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"groupBy\": [\"u.id\", \"u.name\", \"p.name\"],\n" +
                "  \"having\": {\n" +
                "    \"leaf\": {\"left\": \"COUNT(o.id)\", \"operator\": \">\", \"right\": {\"value\": \"0\"}}\n" +
                "  },\n" +
                "  \"orderBy\": [\n" +
                "    {\"expr\": \"order_count\", \"direction\": \"DESC\"},\n" +
                "    {\"expr\": \"u.name\", \"direction\": \"ASC\"}\n" +
                "  ],\n" +
                "  \"limit\": 50,\n" +
                "  \"offset\": 10,\n" +
                "  \"params\": [\n" +
                "    {\"name\": \"user_status\", \"type\": \"STRING\", \"required\": true},\n" +
                "    {\"name\": \"start_date\", \"type\": \"DATE\", \"required\": true}\n" +
                "  ]\n" +
                "}";

        EasySQLEngine engine = new EasySQLEngine();
        String sql = engine.buildSQL(json);
        
        // 验证SQL包含关键元素
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("LEFT JOIN"));
        assertTrue(sql.contains("INNER JOIN"));
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("GROUP BY"));
        assertTrue(sql.contains("HAVING"));
        assertTrue(sql.contains("ORDER BY"));
        assertTrue(sql.contains("LIMIT 50 OFFSET 10"));
        
        // 验证参数占位符
        assertTrue(sql.contains(":user_status"));
        assertTrue(sql.contains(":start_date"));
        
        // 验证别名和转义
        assertTrue(sql.contains("`user_id`"));
        assertTrue(sql.contains("`profile_name`"));
        assertTrue(sql.contains("`order_count`"));
    }

    @Test
    public void testPerformanceAndTemplateLoad() throws Exception {
        Template t = TemplateMapper.fromResource("template-example.json");
        assertNotNull(t);
        assertEquals("user_query", t.id);
        assertEquals("mysql", t.dialect);
        assertEquals(4, t.select.size());
        
        Validator.validateBasic(t);
        
        // 性能基准测试 - 同一模板多次构建
        EasySQLEngine engine = new EasySQLEngine();
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 10; i++) {
            String sql = engine.buildSQL(t);
            assertNotNull(sql);
            assertTrue(sql.contains("SELECT"));
        }
        
        long endTime = System.currentTimeMillis();
        long avgTimeMs = (endTime - startTime) / 10;
        
        // 验证平均每次构建耗时在合理范围内（小于50ms）
        assertTrue(avgTimeMs < 50, "SQL构建性能过慢：平均" + avgTimeMs + "ms");
    }

    @Test
    public void testValidationFailures() {
        // 测试无效模板
        assertThrows(IllegalArgumentException.class, () -> {
            String invalidJson = "{\"id\":\"test\",\"select\":[],\"from\":{\"table\":\"user\"}}";
            EasySQLEngine engine = new EasySQLEngine();
            engine.buildSQL(invalidJson);
        });
        
        // 测试不支持的方言
        assertThrows(IllegalArgumentException.class, () -> {
            String invalidDialectJson = "{\"id\":\"test\",\"dialect\":\"unknown\",\"select\":[{\"expr\":\"id\"}],\"from\":{\"table\":\"user\"}}";
            EasySQLEngine engine = new EasySQLEngine();
            engine.buildSQL(invalidDialectJson);
        });
    }
}