package com.easysql.engine.optimizer;

import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 优化规则
 * - 使用正则表达式匹配SQL模式
 * - 提供建议生成器
 */
public class OptimizationRule {
    private final Pattern pattern;
    private final Function<String, OptimizationSuggestion> generator;

    public OptimizationRule(String regex, Function<String, OptimizationSuggestion> generator) {
        this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        this.generator = generator;
    }

    public boolean matches(String sql) {
        return pattern.matcher(sql).find();
    }

    public OptimizationSuggestion suggest(String sql) {
        return generator.apply(sql);
    }
}