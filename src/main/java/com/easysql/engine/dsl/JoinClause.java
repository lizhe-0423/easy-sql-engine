package com.easysql.engine.dsl;

import com.easysql.engine.model.Template;

import java.util.ArrayList;

public class JoinClause {
    private final Template.Join join = new Template.Join();

    private JoinClause(String type, String table) {
        this.join.type = type;
        this.join.table = new Template.From();
        this.join.table.table = table;
    }

    public static JoinClause inner(String table) {
        return new JoinClause("INNER", table);
    }

    public static JoinClause left(String table) {
        return new JoinClause("LEFT", table);
    }

    public static JoinClause right(String table) {
        return new JoinClause("RIGHT", table);
    }

    public static JoinClause full(String table) {
        return new JoinClause("FULL", table);
    }

    public JoinClause alias(String alias) {
        this.join.table.alias = alias;
        return this;
    }

    public JoinClause on(WhereClause on) {
        if (this.join.on == null) this.join.on = new ArrayList<>();
        // 将WhereClause转换为AND叶子序列（简化：仅支持由多个叶子AND连接）
        // 如果是复合逻辑，这里简单展开：当op为AND时提取所有叶子；其他情况整体作为一个叶子条件表达式left
        Template.Condition cond = on.build();
        flattenAnd(cond);
        return this;
    }

    private void flattenAnd(Template.Condition cond) {
        if (cond.leaf != null) {
            this.join.on.add(cond.leaf);
            return;
        }
        if (cond.conditions != null && "AND".equalsIgnoreCase(cond.op)) {
            for (Template.Condition sub : cond.conditions) {
                flattenAnd(sub);
            }
        } else {
            // 其他逻辑整体作为一个表达式叶子，放在left，右侧为常量1=1以避免语法错误（保守处理）
            Template.On on = new Template.On();
            on.left = "(" + rebuild(cond) + ")";
            on.operator = "=";
            Template.RightValue rv = new Template.RightValue();
            rv.value = "1";
            on.right = rv;
            this.join.on.add(on);
        }
    }

    private String rebuild(Template.Condition c) {
        if (c.leaf != null) {
            String right = c.leaf.right.param != null ? (":" + c.leaf.right.param) : (c.leaf.right.value);
            return c.leaf.left + " " + c.leaf.operator + " " + right;
        }
        if (c.conditions == null || c.conditions.isEmpty()) return "";
        String op = c.op == null ? "AND" : c.op.toUpperCase();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < c.conditions.size(); i++) {
            if (i > 0) sb.append(" ").append(op).append(" ");
            sb.append("(").append(rebuild(c.conditions.get(i))).append(")");
        }
        if ("NOT".equals(op) && c.conditions.size() == 1) {
            return "NOT (" + rebuild(c.conditions.get(0)) + ")";
        }
        return sb.toString();
    }

    Template.Join build() {
        return join;
    }
}