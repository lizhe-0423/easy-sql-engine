package com.easysql.engine.dsl;

import com.easysql.engine.model.Template;

public class SelectClause {
    private final Template.SelectItem item = new Template.SelectItem();

    private SelectClause(String expr) {
        this.item.expr = expr;
    }

    public static SelectClause expr(String expr) {
        return new SelectClause(expr);
    }

    public SelectClause as(String alias) {
        this.item.alias = alias;
        return this;
    }

    Template.SelectItem build() {
        return item;
    }
}