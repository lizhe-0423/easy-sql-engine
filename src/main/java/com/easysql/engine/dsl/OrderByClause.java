package com.easysql.engine.dsl;

import com.easysql.engine.model.Template;

public class OrderByClause {
    private final Template.OrderBy order = new Template.OrderBy();

    private OrderByClause(String expr) {
        this.order.expr = expr;
    }

    public static OrderByClause by(String expr) {
        return new OrderByClause(expr);
    }

    public OrderByClause asc() {
        this.order.direction = "ASC";
        return this;
    }

    public OrderByClause desc() {
        this.order.direction = "DESC";
        return this;
    }

    public OrderByClause nullsFirst() {
        this.order.nulls = "FIRST";
        return this;
    }

    public OrderByClause nullsLast() {
        this.order.nulls = "LAST";
        return this;
    }

    Template.OrderBy build() {
        return order;
    }
}