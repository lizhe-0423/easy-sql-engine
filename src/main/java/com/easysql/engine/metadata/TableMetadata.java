package com.easysql.engine.metadata;

import java.util.List;
import java.util.Map;

/**
 * 表元数据信息
 */
public class TableMetadata {
    
    private String catalog;
    private String schema;
    private String tableName;
    private String tableType; // TABLE, VIEW, etc.
    private String comment;
    private List<ColumnMetadata> columns;
    private Map<String, String> properties;
    private long lastUpdated; // 最后更新时间戳
    
    public TableMetadata() {}
    
    public TableMetadata(String catalog, String schema, String tableName) {
        this.catalog = catalog;
        this.schema = schema;
        this.tableName = tableName;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public String getCatalog() {
        return catalog;
    }
    
    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }
    
    public String getSchema() {
        return schema;
    }
    
    public void setSchema(String schema) {
        this.schema = schema;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    public String getTableType() {
        return tableType;
    }
    
    public void setTableType(String tableType) {
        this.tableType = tableType;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public List<ColumnMetadata> getColumns() {
        return columns;
    }
    
    public void setColumns(List<ColumnMetadata> columns) {
        this.columns = columns;
    }
    
    public Map<String, String> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
    
    public long getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    /**
     * 获取表的完整标识符
     */
    public String getFullTableName() {
        StringBuilder sb = new StringBuilder();
        if (catalog != null && !catalog.trim().isEmpty()) {
            sb.append(catalog).append(".");
        }
        if (schema != null && !schema.trim().isEmpty()) {
            sb.append(schema).append(".");
        }
        sb.append(tableName);
        return sb.toString();
    }
    
    /**
     * 根据列名查找列元数据
     */
    public ColumnMetadata getColumn(String columnName) {
        if (columns == null || columnName == null) return null;
        return columns.stream()
                .filter(c -> columnName.equalsIgnoreCase(c.getColumnName()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 检查是否有指定列
     */
    public boolean hasColumn(String columnName) {
        return getColumn(columnName) != null;
    }
    
    @Override
    public String toString() {
        return "TableMetadata{" +
                "catalog='" + catalog + '\'' +
                ", schema='" + schema + '\'' +
                ", tableName='" + tableName + '\'' +
                ", tableType='" + tableType + '\'' +
                ", columnsCount=" + (columns != null ? columns.size() : 0) +
                '}';
    }
}