package com.easysql.engine.executor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简易命名参数语句：将 :name 转换为 ? 并记录位置
 */
public class NamedParameterStatement {

    private static final Pattern PARAM_PATTERN = Pattern.compile("(:)([a-zA-Z_][a-zA-Z0-9_]*)");

    private final PreparedStatement statement;
    private final List<String> order;

    public NamedParameterStatement(Connection connection, String namedSql) throws SQLException {
        // 解析 :name -> ?
        Matcher matcher = PARAM_PATTERN.matcher(namedSql);
        StringBuffer sb = new StringBuffer();
        this.order = new ArrayList<>();
        while (matcher.find()) {
            String name = matcher.group(2);
            order.add(name);
            matcher.appendReplacement(sb, "?");
        }
        matcher.appendTail(sb);
        this.statement = connection.prepareStatement(sb.toString());
    }

    public PreparedStatement getStatement() {
        return statement;
    }

    public List<String> getOrder() {
        return Collections.unmodifiableList(order);
    }

    public void setObject(int index, Object value) throws SQLException {
        statement.setObject(index, value);
    }
}