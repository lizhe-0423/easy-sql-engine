package com.easysql.engine;

import com.easysql.engine.builder.SQLBuilder;
import com.easysql.engine.dialect.MySQLDialect;
import com.easysql.engine.model.Template;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SQLBuilderTest {

    @Test
    public void testBuildSimpleSelect() {
        Template t = new Template();
        t.id = "t1";
        t.from = new Template.From();
        t.from.schema = "test";
        t.from.table = "user";
        t.from.alias = "u";
        Template.SelectItem s1 = new Template.SelectItem();
        s1.expr = "u.id";
        s1.alias = "id";
        Template.SelectItem s2 = new Template.SelectItem();
        s2.expr = "u.name";
        t.select = java.util.Arrays.asList(s1, s2);
        t.limit = 10;

        SQLBuilder builder = new SQLBuilder(new MySQLDialect());
        String sql = builder.buildSelect(t);
        assertTrue(sql.startsWith("SELECT u.id AS `id`, u.name FROM `test`.`user` `u`"));
        assertTrue(sql.endsWith("LIMIT 10"));
    }
}