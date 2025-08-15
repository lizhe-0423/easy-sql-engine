package com.easysql.engine;

import com.easysql.engine.model.Template;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TemplateMapperTest {

    @Test
    public void testFromJsonAndValidate() throws Exception {
        String json = "{\n" +
                "  \"id\": \"q1\",\n" +
                "  \"datasource\": \"ds1\",\n" +
                "  \"dialect\": \"mysql\",\n" +
                "  \"select\": [{\"expr\": \"u.id\", \"alias\": \"id\"}, {\"expr\": \"u.name\"}],\n" +
                "  \"from\": {\"schema\": \"test\", \"table\": \"user\", \"alias\": \"u\"},\n" +
                "  \"where\": {\n" +
                "    \"leaf\": {\"left\": \"u.status\", \"operator\": \"=\", \"right\": {\"value\": \"enabled\"}}\n" +
                "  },\n" +
                "  \"orderBy\": [{\"expr\": \"u.id\", \"direction\": \"DESC\"}],\n" +
                "  \"limit\": 10\n" +
                "}";

        Template t = TemplateMapper.fromJson(json);
        assertNotNull(t);
        Validator.validateBasic(t);
        assertEquals("q1", t.id);
        assertEquals("user", t.from.table);
        assertEquals(2, t.select.size());
    }

    @Test
    public void testValidateMissingSelect() {
        Template t = new Template();
        t.id = "q2";
        t.from = new Template.From();
        t.from.table = "user";
        // 没有select
        assertThrows(IllegalArgumentException.class, () -> Validator.validateBasic(t));
    }
}