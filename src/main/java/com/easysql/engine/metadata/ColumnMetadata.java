package com.easysql.engine.metadata;

/**
 * 列元数据信息
 */
public class ColumnMetadata {
    private String columnName;
    private String dataType; // 标准化类型，如 STRING/INT/DECIMAL/TIMESTAMP
    private Integer size;
    private Integer scale;
    private Boolean nullable;
    private String comment;

    public ColumnMetadata() {}

    public ColumnMetadata(String columnName, String dataType) {
        this.columnName = columnName;
        this.dataType = dataType;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getScale() {
        return scale;
    }

    public void setScale(Integer scale) {
        this.scale = scale;
    }

    public Boolean getNullable() {
        return nullable;
    }

    public void setNullable(Boolean nullable) {
        this.nullable = nullable;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        return "ColumnMetadata{" +
                "columnName='" + columnName + '\'' +
                ", dataType='" + dataType + '\'' +
                ", size=" + size +
                ", scale=" + scale +
                ", nullable=" + nullable +
                '}';
    }
}