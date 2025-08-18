package com.easysql.engine.optimizer;

import com.easysql.engine.model.Template;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BasicOptimizer {

    public Template optimize(Template t) {
        if (t == null) return null;
        // 1) 归一化WHERE与HAVING：移除空子条件、扁平化同构AND/OR
        t.where = normalize(t.where);
        t.having = normalize(t.having);
        // 2) 裁剪空的JOIN.on 条件（无on则省略ON）—builder已兼容
        // 3) 预留：列裁剪、谓词下推（当前不改写计划结构，留空）
        // 4) Hints去重
        if (t.options != null && t.options.hints != null) {
            t.options.hints = new ArrayList<>(new java.util.LinkedHashSet<>(t.options.hints));
        }
        return t;
    }

    private Template.Condition normalize(Template.Condition c) {
        if (c == null) return null;
        if (c.leaf != null) return c; // 叶子直接返回
        if (c.conditions == null || c.conditions.isEmpty()) return null;
        // 递归归一化
        List<Template.Condition> list = new ArrayList<>();
        for (Template.Condition sub : c.conditions) {
            Template.Condition n = normalize(sub);
            if (n != null) list.add(n);
        }
        // 扁平化相同op
        String op = c.op == null ? "AND" : c.op.toUpperCase();
        if (list.isEmpty()) return null;
        if (list.size() == 1 && !"NOT".equals(op)) {
            return list.get(0);
        }
        List<Template.Condition> flattened = new ArrayList<>();
        if (!"NOT".equals(op)) {
            for (Template.Condition sub : list) {
                if (sub.leaf == null && op.equalsIgnoreCase(sub.op)) {
                    flattened.addAll(sub.conditions);
                } else {
                    flattened.add(sub);
                }
            }
        } else {
            flattened.addAll(list);
        }
        Template.Condition out = new Template.Condition();
        out.op = op;
        out.conditions = flattened;
        return out;
    }
}