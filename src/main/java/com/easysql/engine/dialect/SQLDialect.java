package com.easysql.engine.dialect;

public interface SQLDialect {
    
    /**
     * @return 方言名称，如 "mysql"、"postgresql"、"oracle"
     */
    String getName();

    /**
     * 转义标识符（表名、列名）
     */
    String escapeIdentifier(String identifier);

    /**
     * 转义字符串常量
     */
    String escapeString(String value);

    /**
     * 生成分页SQL
     */
    String limitSQL(String baseSql, Integer limit, Integer offset);

    /**
     * 支持的操作符映射
     */
    String mapOperator(String operator);

    /**
     * 获取数据类型映射
     */
    String mapType(String logicalType);
}