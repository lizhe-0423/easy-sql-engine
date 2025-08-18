package com.easysql.engine.dsl;

import com.easysql.engine.model.Template;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WhereClause {
    private final Template.Condition condition;

    private WhereClause(Template.Condition c) {
        this.condition = c;
    }

    public static WhereClause and(WhereClause... subs) {
        Template.Condition c = new Template.Condition();
        c.op = "AND";
        c.conditions = new ArrayList<>();
        for (WhereClause sub : subs) {
            c.conditions.add(sub.build());
        }
        return new WhereClause(c);
    }

    public static WhereClause or(WhereClause... subs) {
        Template.Condition c = new Template.Condition();
        c.op = "OR";
        c.conditions = new ArrayList<>();
        for (WhereClause sub : subs) {
            c.conditions.add(sub.build());
        }
        return new WhereClause(c);
    }

    public static WhereClause not(WhereClause sub) {
        Template.Condition c = new Template.Condition();
        c.op = "NOT";
        c.conditions = Arrays.asList(sub.build());
        return new WhereClause(c);
    }

    public static WhereClause leaf(String left, String operator, RightValue right) {
        Template.Condition c = new Template.Condition();
        Template.On on = new Template.On();
        on.left = left;
        on.operator = operator;
        on.right = right.build();
        c.leaf = on;
        return new WhereClause(c);
    }

    public static RightValue val(String literal) {
        return new RightValue(literal, null);
    }

    public static RightValue param(String name) {
        return new RightValue(null, name);
    }

    public Template.Condition build() {
        return condition;
    }

    public static class RightValue {
        private final Template.RightValue rv = new Template.RightValue();
        private RightValue(String value, String param) {
            this.rv.value = value;
            this.rv.param = param;
        }
        Template.RightValue build() { return rv; }
    }
}