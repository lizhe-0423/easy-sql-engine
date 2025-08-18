package com.easysql.engine.dsl;

import com.easysql.engine.model.Template;

public class FromClause {
    private final Template.From from = new Template.From();

    private FromClause() {}

    public static FromClause table(String table) {
        FromClause f = new FromClause();
        f.from.table = table;
        return f;
    }

    public static FromClause table(String schema, String table) {
        FromClause f = new FromClause();
        f.from.schema = schema;
        f.from.table = table;
        return f;
    }

    public static FromClause table(String catalog, String schema, String table) {
        FromClause f = new FromClause();
        f.from.catalog = catalog;
        f.from.schema = schema;
        f.from.table = table;
        return f;
    }

    public FromClause alias(String alias) {
        this.from.alias = alias;
        return this;
    }

    Template.From build() {
        return from;
    }
}