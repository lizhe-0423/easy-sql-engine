package com.easysql.engine;

import com.easysql.engine.model.Template;

public class Validator {

    public static void validateBasic(Template t) {
        if (t == null) throw new IllegalArgumentException("template is null");
        if (isBlank(t.id)) throw new IllegalArgumentException("template.id is required");
        if (t.from == null || isBlank(t.from.table)) {
            throw new IllegalArgumentException("template.from.table is required");
        }
        if (t.select == null || t.select.isEmpty()) {
            throw new IllegalArgumentException("template.select must have at least one item");
        }
        // limit/offset 边界
        if (t.limit != null && t.limit < 0) throw new IllegalArgumentException("limit must be >= 0");
        if (t.offset != null && t.offset < 0) throw new IllegalArgumentException("offset must be >= 0");
        // options边界
        if (t.options != null) {
            if (t.options.timeoutMs != null && (t.options.timeoutMs <= 0 || t.options.timeoutMs > 600000)) {
                throw new IllegalArgumentException("options.timeoutMs out of range");
            }
            if (t.options.maxRows != null && t.options.maxRows < 0) {
                throw new IllegalArgumentException("options.maxRows must be >= 0");
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}