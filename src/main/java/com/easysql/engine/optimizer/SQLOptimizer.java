package com.easysql.engine.optimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL优化分析引擎
 * 
 * 功能：
 * 1) 解析输入的SQL语句
 * 2) 识别常见性能问题（如缺失索引、全表扫描、不当JOIN等）
 * 3) 生成优化建议与根据说明
 */
public class SQLOptimizer {
    
    private final List<OptimizationRule> rules;
    
    public SQLOptimizer() {
        this.rules = initializeRules();
    }
    
    /**
     * 分析SQL并返回优化结果
     * @param sql 原始SQL语句
     * @return 优化分析结果
     */
    public OptimizationResult analyze(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return new OptimizationResult(sql, sql, "无需优化", "输入为空");
        }
        
        String normalizedSQL = normalizeSQL(sql);
        List<OptimizationSuggestion> suggestions = new ArrayList<>();
        String optimizedSQL = sql;
        
        // 应用所有优化规则
        for (OptimizationRule rule : rules) {
            if (rule.matches(normalizedSQL)) {
                OptimizationSuggestion suggestion = rule.suggest(normalizedSQL);
                suggestions.add(suggestion);
                
                // 应用优化（如果有具体的SQL改写）
                if (suggestion.getOptimizedSQL() != null) {
                    optimizedSQL = suggestion.getOptimizedSQL();
                }
            }
        }
        
        // 汇总建议
        if (suggestions.isEmpty()) {
            return new OptimizationResult(sql, sql, "SQL语句看起来已经比较优化", "未发现明显的性能问题");
        }
        
        StringBuilder advice = new StringBuilder();
        StringBuilder reason = new StringBuilder();
        
        for (int i = 0; i < suggestions.size(); i++) {
            OptimizationSuggestion suggestion = suggestions.get(i);
            if (i > 0) {
                advice.append("; ");
                reason.append("; ");
            }
            advice.append(suggestion.getAdvice());
            reason.append(suggestion.getReason());
        }
        
        return new OptimizationResult(sql, optimizedSQL, advice.toString(), reason.toString());
    }
    
    /**
     * 标准化SQL（去除多余空格、转小写等）
     */
    private String normalizeSQL(String sql) {
        return sql.replaceAll("\\s+", " ").trim().toLowerCase();
    }
    
    /**
     * 初始化优化规则库
     */
    private List<OptimizationRule> initializeRules() {
        List<OptimizationRule> ruleList = new ArrayList<>();
        
        // 规则1: SELECT * 优化
        ruleList.add(new OptimizationRule(
            "select \\\\* from",
            sql -> new OptimizationSuggestion(
                "避免使用 SELECT *，明确指定需要的列",
                "根据数据库查询优化原则，SELECT * 会：1) 增加网络传输开销 2) 降低查询缓存效率 3) 影响索引覆盖优化",
                sql.replaceFirst("select \\\\*", "select column1, column2, ...")
            )
        ));
        
        // 规则2: 没有WHERE条件的查询
        ruleList.add(new OptimizationRule(
            "select .+ from .+(?!.*where)",
            sql -> new OptimizationSuggestion(
                "建议添加WHERE条件限制查询范围，或使用LIMIT限制返回行数",
                "根据数据库性能调优指南，全表扫描会：1) 消耗大量IO资源 2) 占用过多内存 3) 影响并发查询性能",
                sql + (sql.contains("limit") ? "" : " LIMIT 1000")
            )
        ));
        
        // 规则3: ORDER BY没有LIMIT
        ruleList.add(new OptimizationRule(
            "order by .+(?!.*limit)",
            sql -> new OptimizationSuggestion(
                "ORDER BY 后建议添加 LIMIT，避免对大结果集排序",
                "根据SQL性能优化最佳实践，无限制排序会：1) 消耗大量CPU和内存 2) 增加查询响应时间 3) 可能导致临时文件创建",
                sql + " LIMIT 100"
            )
        ));
        
        // 规则4: 使用函数在WHERE条件中
        ruleList.add(new OptimizationRule(
            "where .+\\\\([^)]+\\\\)",
            sql -> new OptimizationSuggestion(
                "避免在WHERE条件中使用函数，这会阻止索引使用",
                "根据数据库索引优化原理，WHERE子句中的函数会：1) 使相关列索引失效 2) 强制进行全表扫描 3) 大幅降低查询性能",
                null // 这种情况需要具体分析，不提供通用改写
            )
        ));
        
        // 规则5: NOT IN 优化
        ruleList.add(new OptimizationRule(
            "not in\\s*\\(",
            sql -> new OptimizationSuggestion(
                "建议使用 NOT EXISTS 或 LEFT JOIN + IS NULL 替代 NOT IN",
                "根据SQL查询优化准则，NOT IN存在性能问题：1) 无法有效利用索引 2) 遇到NULL值时行为异常 3) 执行效率低于NOT EXISTS",
                sql.replaceAll("not in\\s*\\(", "not exists (select 1 from ... where ...")
            )
        ));
        
        // 规则6: LIKE '%...' 开头模糊查询
        ruleList.add(new OptimizationRule(
            "like\\s+['\"]%",
            sql -> new OptimizationSuggestion(
                "避免使用前置通配符LIKE '%...'，考虑使用全文索引或其他搜索方案",
                "根据数据库索引机制，前置通配符查询会：1) 无法使用B+树索引 2) 必须进行全表扫描 3) 查询性能随数据量线性下降",
                null
            )
        ));
        
        // --- 新增规则：通用查询优化 ---
        
        // 规则7: 子查询 IN 替换为 EXISTS（相关子查询）
        ruleList.add(new OptimizationRule(
            "\\bin\\s*\\(\\s*select",
            sql -> new OptimizationSuggestion(
                "子查询建议由 IN 改为 EXISTS，通常可获得更好执行计划",
                "IN(subquery) 往往会触发去重与物化，EXISTS 则基于布尔存在判断，优化器更易转为半连接，执行更高效",
                sql.replaceFirst("\\bin\\s*\\(\\s*select", " exists (select 1 from ... where ...")
            )
        ));
        
        // 规则8: DISTINCT 使用检查
        ruleList.add(new OptimizationRule(
            "\\bdistinct\\b",
            sql -> new OptimizationSuggestion(
                "请确认是否确需 DISTINCT，如为去重可考虑源头数据治理或索引辅助",
                "DISTINCT 会触发排序/哈希去重，成本较高。若仅为避免重复，可通过唯一索引、分组汇总或查询条件避免重复",
                null
            )
        ));
        
        // 规则9: ORDER BY RAND() 反模式
        ruleList.add(new OptimizationRule(
            "order\\s+by\\s+rand\\s*\\(\\s*\\)",
            sql -> new OptimizationSuggestion(
                "避免使用 ORDER BY RAND()，建议使用预生成随机列或采样方案",
                "ORDER BY RAND() 会对全部结果集计算随机值并排序，代价极高。可使用随机键/散列列或限制范围的采样方法",
                null
            )
        ));
        
        // 规则10: UNION 优化为 UNION ALL（若业务允许）
        ruleList.add(new OptimizationRule(
            "\\bunion\\b(?!\\s+all)",
            sql -> new OptimizationSuggestion(
                "若允许重复，请使用 UNION ALL，避免不必要的去重排序",
                "UNION 隐式去重需要排序/哈希，UNION ALL 直接合并结果集，性能更佳",
                sql.replaceFirst("\\bunion\\b(?!\\s+all)", "union all")
            )
        ));
        
        // 规则11: 大偏移量分页（OFFSET）
        ruleList.add(new OptimizationRule(
            "\\boffset\\s+\\d+",
            sql -> new OptimizationSuggestion(
                "检测到 OFFSET 分页，建议改为基于主键游标的 Keyset Pagination",
                "OFFSET N LIMIT M 会扫描并丢弃前 N 行，N 越大代价越高。推荐使用 where id > last_id order by id limit M 的方式",
                null
            )
        ));
        
        // 规则12: WHERE 中包含 OR（可能破坏索引合并）
        ruleList.add(new OptimizationRule(
            "\\bwhere\\b[\\\\s\\\\S]*\\bor\\b",
            sql -> new OptimizationSuggestion(
                "WHERE 子句包含 OR，建议考虑使用 UNION ALL 拆分或改写为 IN 列表",
                "OR 条件可能阻碍索引的有效使用，拆分为多个子查询（UNION ALL）或改写为 IN 更利于索引",
                null
            )
        ));
        
        // 规则13: HAVING 用于非聚合过滤
        ruleList.add(new OptimizationRule(
            "\\bhaving\\b[\\\\s\\\\S]*\\b(=|<>|<|>|<=|>=)\\b[\\\\s\\\\S]*",
            sql -> new OptimizationSuggestion(
                "检测到 HAVING 过滤，若非聚合条件，建议下推到 WHERE",
                "HAVING 在分组之后执行，若条件不依赖聚合，提前在 WHERE 过滤能缩小数据量，降低分组/聚合成本",
                null
            )
        ));
        
        // 规则14: SELECT 列表中包含标量子查询
        ruleList.add(new OptimizationRule(
            "\\bselect\\b[\\\\s\\\\S]*\\(\\s*select\\b",
            sql -> new OptimizationSuggestion(
                "检测到标量子查询，建议改为 JOIN 获取字段，避免行级重复子查询",
                "SELECT 列表中的子查询会对每一行执行，可能形成 N+1 查询；JOIN 一次性获取可显著降低开销",
                null
            )
        ));
        
        // 规则15: JOIN 条件中使用函数
        ruleList.add(new OptimizationRule(
            "\\bjoin\\b[\\\\s\\\\S]*\\bon\\b[\\\\s\\\\S]*\\(.*\\)",
            sql -> new OptimizationSuggestion(
                "JOIN 条件中存在函数调用，可能导致无法使用连接键索引",
                "连接键上的函数会使索引失效，建议预处理列值或改写条件以利用索引",
                null
            )
        ));
        
        // 规则16: 隐式连接（逗号连接）
        ruleList.add(new OptimizationRule(
            "\\bfrom\\b\\s+\\w+\\s*,\\s*\\w+",
            sql -> new OptimizationSuggestion(
                "检测到隐式连接，建议改为显式 JOIN ... ON ... 提升可读性与优化器可用信息",
                "显式 JOIN 能清晰表达连接关系，便于优化器选择合适的连接算法与顺序",
                null
            )
        ));
        
        // 规则17: LIKE '%...%' 两端通配
        ruleList.add(new OptimizationRule(
            "like\\s+['\"][^%_]*%[^'\"]*%['\"]",
            sql -> new OptimizationSuggestion(
                "检测到包含式模糊匹配，建议考虑全文索引/倒排索引或搜索引擎方案",
                "'%...%' 模式无法使用前缀索引，通常需要全表扫描；可引入全文索引或检索服务（如 ES）",
                null
            )
        ));
        
        // 规则18: IS NULL / IS NOT NULL
        ruleList.add(new OptimizationRule(
            "\\bis\\s+not\\s+null|\\bis\\s+null",
            sql -> new OptimizationSuggestion(
                "检测到 NULL 判断，请确保相应列具备合适的索引与选择性",
                "对 NULL 的判断可能影响索引使用（视数据库与存储引擎而定），可通过部分索引/条件式索引与业务约束优化",
                null
            )
        ));
        
        // 规则19: 常量布尔过滤（无效条件）
        ruleList.add(new OptimizationRule(
            "where\\s+\\d+\\s*=\\s*\\d+",
            sql -> new OptimizationSuggestion(
                "检测到常量布尔条件，请移除无效条件，避免误导优化器",
                "恒真/恒假的条件会让优化器误判选择性或增加不必要的谓词，建议在生成SQL时剔除",
                null
            )
        ));
        
        return ruleList;
    }
}