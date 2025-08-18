package com.easysql.engine.builder;

import com.easysql.engine.dialect.SQLDialect;
import com.easysql.engine.model.Template;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SQLBuilder {

    private final SQLDialect dialect;

    public SQLBuilder(SQLDialect dialect) {
        this.dialect = dialect;
    }

    public String buildSelect(Template t) {
        StringBuilder sql = new StringBuilder();
        // SELECT
        sql.append("SELECT ");
        // 注入方言hint（如有），采用通用注释/*+ ... */形式
        if (t.options != null && t.options.hints != null && !t.options.hints.isEmpty()) {
            sql.append("/*+ ").append(String.join(" ", t.options.hints)).append(" */ ");
        }
        String selectPart = t.select.stream()
                .map(it -> it.alias != null && !it.alias.isEmpty()
                        ? it.expr + " AS " + dialect.escapeIdentifier(it.alias)
                        : it.expr)
                .collect(Collectors.joining(", "));
        sql.append(selectPart).append(" ");
        // FROM
        sql.append("FROM ");
        sql.append(qualify(t.from)).append(" ");
        // JOIN
        if (t.joins != null && !t.joins.isEmpty()) {
            for (Template.Join j : t.joins) {
                String jt = j.type == null ? "INNER" : j.type.toUpperCase();
                sql.append(jt).append(" JOIN ").append(qualify(j.table)).append(" ");
                if (j.on != null && !j.on.isEmpty()) {
                    sql.append("ON ");
                    List<String> onParts = new ArrayList<>();
                    for (Template.On on : j.on) {
                        onParts.add(on.left + " " + dialect.mapOperator(on.operator) + " " + renderRight(on.right));
                    }
                    sql.append(String.join(" AND ", onParts)).append(" ");
                }
            }
        }
        // WHERE
        if (t.where != null) {
            String where = renderCondition(t.where);
            if (where != null && !where.isEmpty()) {
                sql.append("WHERE ").append(where).append(" ");
            }
        }
        // GROUP BY
        if (t.groupBy != null && !t.groupBy.isEmpty()) {
            sql.append("GROUP BY ").append(String.join(", ", t.groupBy)).append(" ");
        }
        // HAVING
        if (t.having != null) {
            String having = renderCondition(t.having);
            if (having != null && !having.isEmpty()) {
                sql.append("HAVING ").append(having).append(" ");
            }
        }
        // ORDER BY
        if (t.orderBy != null && !t.orderBy.isEmpty()) {
            String order = t.orderBy.stream()
                    .map(o -> o.expr + (o.direction != null ? (" " + o.direction) : ""))
                    .collect(Collectors.joining(", "));
            sql.append("ORDER BY ").append(order).append(" ");
        }
        // LIMIT/OFFSET
        String base = sql.toString().trim();
        return dialect.limitSQL(base, t.limit, t.offset);
    }

    private String qualify(Template.From f) {
        StringBuilder q = new StringBuilder();
        if (f.catalog != null && !f.catalog.isEmpty()) {
            q.append(dialect.escapeIdentifier(f.catalog)).append(".");
        }
        if (f.schema != null && !f.schema.isEmpty()) {
            q.append(dialect.escapeIdentifier(f.schema)).append(".");
        }
        q.append(dialect.escapeIdentifier(f.table));
        if (f.alias != null && !f.alias.isEmpty()) {
            q.append(" ").append(dialect.escapeIdentifier(f.alias));
        }
        return q.toString();
    }

    private String renderRight(Template.RightValue rv) {
        if (rv == null) return "NULL";
        if (rv.param != null) {
            // 参数占位符，M1使用命名参数格式 :name
            return ":" + rv.param;
        }
        String v = rv.value;
        if (v == null) return "NULL";
        String trimmed = v.trim();
        // NULL 直出
        if (trimmed.equalsIgnoreCase("null")) return "NULL";
        // 数字直出（整数或小数）
        if (trimmed.matches("^-?\\d+(\\.\\d+)?$")) return trimmed;
        // 布尔直出
        if (trimmed.equalsIgnoreCase("true") || trimmed.equalsIgnoreCase("false")) return trimmed.toUpperCase();
        // 列引用/表达式直出：形如 a.b 或包含函数调用括号
        if (trimmed.matches("^[a-zA-Z_][\\w$]*(\\.[a-zA-Z_][\\w$]*)+$") || trimmed.contains("(")) {
            return trimmed;
        }
        // 其他作为字符串常量处理
        return dialect.escapeString(trimmed);
    }

    private String renderCondition(Template.Condition c) {
        if (c.leaf != null) {
            return c.leaf.left + " " + dialect.mapOperator(c.leaf.operator) + " " + renderRight(c.leaf.right);
        }
        if (c.conditions == null || c.conditions.isEmpty()) return null;
        String op = c.op == null ? "AND" : c.op.toUpperCase();
        List<String> parts = new ArrayList<>();
        for (Template.Condition sub : c.conditions) {
            String r = renderCondition(sub);
            if (r != null && !r.isEmpty()) parts.add("(" + r + ")");
        }
        if (parts.isEmpty()) return null;
        if ("NOT".equals(op) && parts.size() == 1) {
            return "NOT " + parts.get(0);
        }
        return String.join(" " + op + " ", parts);
    }
}