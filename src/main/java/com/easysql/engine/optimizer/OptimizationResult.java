package com.easysql.engine.optimizer;

import java.util.ArrayList;
import java.util.List;

/**
 * 优化结果
 * - originalSQL: 原始SQL
 * - optimizedSQL: 优化后的SQL（若有）
 * - advice: 综合建议/结论
 * - reason: 建议依据说明
 */
public class OptimizationResult {
    private final String originalSQL;
    private final String optimizedSQL;
    private final String advice;
    private final String reason;

    public OptimizationResult(String originalSQL, String optimizedSQL, String advice, String reason) {
        this.originalSQL = originalSQL;
        this.optimizedSQL = optimizedSQL;
        this.advice = advice;
        this.reason = reason;
    }

    public String getOriginalSQL() {
        return originalSQL;
    }

    public String getOptimizedSQL() {
        return optimizedSQL;
    }

    public String getAdvice() {
        return advice;
    }

    public String getReason() {
        return reason;
    }
}