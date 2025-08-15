package com.easysql.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EasySQLEngineTest {

    @Test
    public void testEndToEndBuild() throws Exception {
        String json = "{\n" +
                "  \"id\": \"q1\",\n" +
                "  \"dialect\": \"mysql\",\n" +
                "  \"select\": [{\"expr\": \"u.id\", \"alias\": \"id\"}, {\"expr\": \"u.name\"}],\n" +
                "  \"from\": {\"schema\": \"test\", \"table\": \"user\", \"alias\": \"u\"},\n" +
                "  \"limit\": 5\n" +
                "}";
        EasySQLEngine engine = new EasySQLEngine();
        String sql = engine.buildSQL(json);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM"));
        assertTrue(sql.endsWith("LIMIT 5"));
    }
}