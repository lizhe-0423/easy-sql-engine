package com.easysql.engine.optimizer;

/**
 * 优化建议
 * - advice: 建议内容（结论）
 * - reason: 为什么这样优化（建议的依据）
 * - optimizedSQL: 改写后的SQL（可选，不一定能通用改写）
 */
public class OptimizationSuggestion {
    private final String advice;
    private final String reason;
    private final String optimizedSQL;

    public OptimizationSuggestion(String advice, String reason, String optimizedSQL) {
        this.advice = advice;
        this.reason = reason;
        this.optimizedSQL = optimizedSQL;
    }

    public String getAdvice() {
        return advice;
    }

    public String getReason() {
        return reason;
    }

    public String getOptimizedSQL() {
        return optimizedSQL;
    }
}