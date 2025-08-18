package com.easysql.engine.metadata;

import cn.hutool.cache.Cache;
import cn.hutool.cache.impl.LRUCache;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 元数据缓存：按数据源别名 + 表全名 缓存表与列信息，支持TTL与容量
 */
public class MetadataCache {

    private final Cache<String, TableMetadata> tableCache;
    private final long ttlMs;

    // 数据源别名到连接提供者（外部注入）
    private final Map<String, java.util.function.Supplier<Connection>> connectionSuppliers = new ConcurrentHashMap<>();

    public MetadataCache(int capacity, long ttlMs) {
        this.tableCache = new LRUCache<>(capacity);
        this.ttlMs = ttlMs;
    }

    public void registerDatasource(String name, java.util.function.Supplier<Connection> connectionSupplier) {
        connectionSuppliers.put(name, connectionSupplier);
    }

    public void unregisterDatasource(String name) {
        connectionSuppliers.remove(name);
    }

    private String key(String datasource, String catalog, String schema, String table) {
        String full = (catalog == null ? "" : catalog + ".") + (schema == null ? "" : schema + ".") + table;
        return datasource + "|" + full.toLowerCase();
    }

    public TableMetadata getTable(String datasource, String catalog, String schema, String table) throws SQLException {
        String k = key(datasource, catalog, schema, table);
        TableMetadata tm = tableCache.get(k, false);
        if (tm != null && (System.currentTimeMillis() - tm.getLastUpdated() <= ttlMs)) {
            return tm;
        }
        // 重新加载
        TableMetadata loaded = loadTable(datasource, catalog, schema, table);
        if (loaded != null) {
            tableCache.put(k, loaded);
        }
        return loaded;
    }

    private TableMetadata loadTable(String datasource, String catalog, String schema, String table) throws SQLException {
        java.util.function.Supplier<Connection> supplier = connectionSuppliers.get(datasource);
        if (supplier == null) {
            throw new IllegalStateException("No datasource registered: " + datasource);
        }
        try (Connection conn = supplier.get()) {
            DatabaseMetaData meta = conn.getMetaData();
            String[] types = {"TABLE", "VIEW"};
            try (ResultSet rs = meta.getTables(catalog, schema, table, types)) {
                if (!rs.next()) return null;
                TableMetadata tm = new TableMetadata(rs.getString("TABLE_CAT"), rs.getString("TABLE_SCHEM"), rs.getString("TABLE_NAME"));
                tm.setTableType(rs.getString("TABLE_TYPE"));
                tm.setComment(rs.getString("REMARKS"));
                // 列信息
                List<ColumnMetadata> cols = new ArrayList<>();
                try (ResultSet crs = meta.getColumns(catalog, schema, table, null)) {
                    while (crs.next()) {
                        ColumnMetadata cm = new ColumnMetadata();
                        cm.setColumnName(crs.getString("COLUMN_NAME"));
                        cm.setDataType(jdbcTypeToStd(crs.getInt("DATA_TYPE")));
                        cm.setSize(crs.getInt("COLUMN_SIZE"));
                        cm.setScale(crs.getInt("DECIMAL_DIGITS"));
                        cm.setNullable(crs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                        cm.setComment(crs.getString("REMARKS"));
                        cols.add(cm);
                    }
                }
                tm.setColumns(cols);
                tm.setLastUpdated(System.currentTimeMillis());
                return tm;
            }
        }
    }

    private String jdbcTypeToStd(int jdbcType) {
        switch (jdbcType) {
            case java.sql.Types.INTEGER:
            case java.sql.Types.SMALLINT:
            case java.sql.Types.TINYINT:
                return "INT";
            case java.sql.Types.BIGINT:
                return "BIGINT";
            case java.sql.Types.FLOAT:
            case java.sql.Types.REAL:
            case java.sql.Types.DOUBLE:
                return "DOUBLE";
            case java.sql.Types.DECIMAL:
            case java.sql.Types.NUMERIC:
                return "DECIMAL";
            case java.sql.Types.DATE:
                return "DATE";
            case java.sql.Types.TIMESTAMP:
            case java.sql.Types.TIMESTAMP_WITH_TIMEZONE:
                return "TIMESTAMP";
            case java.sql.Types.BOOLEAN:
            case java.sql.Types.BIT:
                return "BOOLEAN";
            case java.sql.Types.VARCHAR:
            case java.sql.Types.CHAR:
            case java.sql.Types.LONGVARCHAR:
            default:
                return "STRING";
        }
    }
}