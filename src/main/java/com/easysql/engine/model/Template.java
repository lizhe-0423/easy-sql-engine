package com.easysql.engine.model;

import java.util.List;
import java.util.Map;

public class Template {
    public String id;
    public String version; // 模板版本号，用于版本控制和审计追踪
    public String datasource;
    public String dialect; // 可选，优先采用数据源配置

    public List<SelectItem> select;
    public From from;
    public List<Join> joins;
    public Condition where;
    public List<String> groupBy;
    public Condition having;
    public List<OrderBy> orderBy;
    public Integer limit;
    public Integer offset;

    public List<UnionItem> unions;
    public Options options;
    public List<Param> params;

    public static class SelectItem {
        public String expr;
        public String alias;
    }

    public static class From {
        public String catalog;
        public String schema;
        public String table;
        public String alias;
    }

    public static class Join {
        public String type; // inner,left,right,full
        public From table;
        public List<On> on;
    }

    public static class On {
        public String left; // 列或表达式
        public String operator; // =, >, <, >=, <=, !=, IN, BETWEEN, LIKE
        public RightValue right;
    }

    public static class RightValue {
        public String value; // 常量值（字符串/数字）
        public String param; // 参数名（与params声明对应）
    }

    public static class Condition {
        public String op; // AND, OR, NOT；若为叶子则置空
        public List<Condition> conditions; // 逻辑组合
        public On leaf; // 叶子条件
    }

    public static class OrderBy {
        public String expr;
        public String direction; // ASC, DESC
        public String nulls; // FIRST, LAST
    }

    public static class UnionItem {
        public boolean unionAll;
        public Template query;
    }

    public static class Options {
        public Integer timeoutMs;
        public Integer maxRows;
        public Integer scanPartitions;
        public List<String> hints;
        public Integer fetchSize;
        public Boolean readOnly;
    }

    public static class Param {
        public String name;
        public String type; // STRING, INT, BIGINT, DOUBLE, DECIMAL, DATE, TIMESTAMP, BOOLEAN
        public Boolean required;
        public String def; // 默认值字符串表示
        public Map<String, Object> rules; // 其他校验规则
    }
}