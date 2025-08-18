package com.easysql.engine.dsl;

import com.easysql.engine.model.Template;

import java.util.ArrayList;
import java.util.List;

/**
 * DSL查询构建器主入口
 * 示例: Query.create().from("users").select("id", "name").where(eq("status", 1))
 */
public class Query {
    
    private final Template template;
    
    private Query() {
        this.template = new Template();
        this.template.select = new ArrayList<>();
        this.template.params = new ArrayList<>();
    }
    
    /**
     * 创建新的查询构建器
     */
    public static Query create() {
        return new Query();
    }
    
    /**
     * 创建查询并设置ID
     */
    public static Query create(String id) {
        Query q = new Query();
        q.template.id = id;
        return q;
    }
    
    /**
     * 设置查询ID（用于缓存和审计）
     */
    public Query id(String id) {
        this.template.id = id;
        return this;
    }
    
    /**
     * 设置数据源
     */
    public Query datasource(String datasource) {
        this.template.datasource = datasource;
        return this;
    }
    
    /**
     * 设置SQL方言
     */
    public Query dialect(String dialect) {
        this.template.dialect = dialect;
        return this;
    }
    
    /**
     * 设置FROM表
     */
    public Query from(String table) {
        this.template.from = new Template.From();
        this.template.from.table = table;
        return this;
    }
    
    /**
     * 设置FROM表（含schema）
     */
    public Query from(String schema, String table) {
        this.template.from = new Template.From();
        this.template.from.schema = schema;
        this.template.from.table = table;
        return this;
    }
    
    /**
     * 设置FROM表（含catalog.schema）
     */
    public Query from(String catalog, String schema, String table) {
        this.template.from = new Template.From();
        this.template.from.catalog = catalog;
        this.template.from.schema = schema;
        this.template.from.table = table;
        return this;
    }
    
    /**
     * 设置FROM表（完整From对象）
     */
    public Query from(FromClause from) {
        this.template.from = from.build();
        return this;
    }
    
    /**
     * 添加SELECT列
     */
    public Query select(String... expressions) {
        for (String expr : expressions) {
            Template.SelectItem item = new Template.SelectItem();
            item.expr = expr;
            this.template.select.add(item);
        }
        return this;
    }
    
    /**
     * 添加SELECT列（带别名）
     */
    public Query select(SelectClause... selects) {
        for (SelectClause s : selects) {
            this.template.select.add(s.build());
        }
        return this;
    }
    
    /**
     * 设置WHERE条件
     */
    public Query where(WhereClause where) {
        this.template.where = where.build();
        return this;
    }
    
    /**
     * 添加JOIN
     */
    public Query join(JoinClause join) {
        if (this.template.joins == null) {
            this.template.joins = new ArrayList<>();
        }
        this.template.joins.add(join.build());
        return this;
    }
    
    /**
     * 添加INNER JOIN
     */
    public Query innerJoin(String table, WhereClause on) {
        return join(JoinClause.inner(table).on(on));
    }
    
    /**
     * 添加LEFT JOIN
     */
    public Query leftJoin(String table, WhereClause on) {
        return join(JoinClause.left(table).on(on));
    }
    
    /**
     * 设置GROUP BY
     */
    public Query groupBy(String... expressions) {
        if (this.template.groupBy == null) {
            this.template.groupBy = new ArrayList<>();
        }
        for (String expr : expressions) {
            this.template.groupBy.add(expr);
        }
        return this;
    }
    
    /**
     * 设置HAVING条件
     */
    public Query having(WhereClause having) {
        this.template.having = having.build();
        return this;
    }
    
    /**
     * 设置ORDER BY
     */
    public Query orderBy(OrderByClause... orders) {
        if (this.template.orderBy == null) {
            this.template.orderBy = new ArrayList<>();
        }
        for (OrderByClause order : orders) {
            this.template.orderBy.add(order.build());
        }
        return this;
    }
    
    /**
     * 设置LIMIT
     */
    public Query limit(int limit) {
        this.template.limit = limit;
        return this;
    }
    
    /**
     * 设置OFFSET
     */
    public Query offset(int offset) {
        this.template.offset = offset;
        return this;
    }
    
    /**
     * 设置LIMIT和OFFSET
     */
    public Query limit(int limit, int offset) {
        this.template.limit = limit;
        this.template.offset = offset;
        return this;
    }
    
    /**
     * 添加参数声明
     */
    public Query param(String name, String type) {
        Template.Param param = new Template.Param();
        param.name = name;
        param.type = type;
        param.required = true;
        this.template.params.add(param);
        return this;
    }
    
    /**
     * 添加参数声明（可选）
     */
    public Query param(String name, String type, boolean required) {
        Template.Param param = new Template.Param();
        param.name = name;
        param.type = type;
        param.required = required;
        this.template.params.add(param);
        return this;
    }
    
    /**
     * 设置选项
     */
    public Query options(OptionsClause options) {
        this.template.options = options.build();
        return this;
    }
    
    /**
     * 构建Template对象
     */
    public Template build() {
        if (template.id == null || template.id.trim().isEmpty()) {
            template.id = "dsl_query_" + System.currentTimeMillis();
        }
        return template;
    }
}