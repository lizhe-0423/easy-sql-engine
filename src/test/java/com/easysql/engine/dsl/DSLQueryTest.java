package com.easysql.engine.dsl;

import com.easysql.engine.EasySQLEngine;
import com.easysql.engine.model.Template;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.easysql.engine.dsl.SelectClause.expr;
import static com.easysql.engine.dsl.WhereClause.*;

public class DSLQueryTest {

    @Test
    public void testSimpleDSLToSQL() throws Exception {
        Template t = Query.create("dsl_test_1")
                .from("users")
                .select("id", "name")
                .where(and(
                        leaf("status", "=", val("1")),
                        leaf("age", ">", param("minAge"))
                ))
                .orderBy(com.easysql.engine.dsl.OrderByClause.by("id").desc())
                .limit(10)
                .build();

        // 声明参数
        Query.create().param("minAge", "INT");

        EasySQLEngine engine = new EasySQLEngine();
        String sql = engine.buildSQL(t);
        Assertions.assertTrue(sql.toLowerCase().contains("from `users`"));
        Assertions.assertTrue(sql.toLowerCase().contains("where (status = '1') and (age > :minAge)".toLowerCase()));
        Assertions.assertTrue(sql.toLowerCase().contains("limit 10"));
    }
}