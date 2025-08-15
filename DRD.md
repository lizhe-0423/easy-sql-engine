# Easy-SQL 引擎 - DRD（数据设计文档）

## 1. 数据源与连接
- 支持的数据源：关系型数据库（MySQL、PostgreSQL、ClickHouse等）；后续可扩展。
- 连接信息：由外部以DataSource注入；引擎不管理账号密钥，避免泄露风险。
- 多数据源：通过命名DataSource管理；模板可引用数据源别名（受白名单限制）。

## 2. 逻辑数据模型抽象
- Catalog/Schema/Table/Column 抽象，映射到各方言元数据。
- 列类型：标准化到统一中间类型（BOOLEAN、INT、BIGINT、DECIMAL、DOUBLE、STRING、DATE、TIMESTAMP、ARRAY、MAP等），由Dialect做类型映射。
- 元数据来源：JDBC元数据 + 配置中心（可选）；本地缓存（LRU + TTL）。

## 3. 类型系统与映射
- 双向映射：JDBC/DB类型 <-> 中间类型 <-> Java类型。
- 时间与时区：内部统一UTC存储与转换策略（可配置），模板可声明期望时区/格式化。

## 4. JSON模板数据结构定义（概要）
- 顶层字段
  - version: 模板版本
  - id: 模板唯一标识（用于审计/缓存）
  - datasource: 数据源别名
  - dialect: 目标方言（可选，默认跟随数据源）
  - select: [ { expr, alias, agg, window } ... ]
  - from: { catalog, schema, table, alias }
  - joins: [ { type, table:{...}, on:[条件表达式] } ]
  - where: { op: "AND/OR/NOT/…", conditions:[…] } 或叶子条件 { left, operator, right }
  - groupBy: [ 表达式... ]
  - having: 同where结构
  - orderBy: [ { expr, direction, nulls } ... ]
  - limit: number
  - offset: number
  - unions: [ { unionAll: true/false, query: <子模板> } ]
  - options: { timeoutMs, maxRows, scanPartitions, hints:[…], fetchSize, readOnly }
  - params: [ { name, type, required, default, pattern, min, max } ]
- 条件表达式节点
  - 比较：=, !=, >, >=, <, <=, IN, BETWEEN, LIKE, REGEXP（按方言化）
  - 逻辑：AND/OR/NOT
  - 函数表达式：fn(name, args:[…])，由Dialect解析
- 约束
  - from/ select 至少一项
  - 所有右值引用params必须在params里声明
  - 表/列/函数必须通过白名单或元数据校验通过

## 5. JSON Schema（简化示意）
- 可为模板提供JSON Schema用于静态校验，例如：
  - id: string, required
  - select: array(minItems>=1)
  - from.table: string, required
  - params[].name/type/required: required
  - options.timeoutMs/maxRows: number with upper bound
- 校验失败需返回明确错误码与可读信息。

## 6. 对象DSL到数据模型映射
- DSL中的列、表、表达式映射到与JSON模板相同的中间表达式结构，确保两种输入通道共用同一套计划/方言/执行流水线。

## 7. 示例模板（简化）
- 目的：统计近7天活跃用户按地区分组并分页
- 关键字段说明：
  - params: startTs/endTs 为时间范围
  - select: count(distinct user_id) as dau, region
  - groupBy: region
  - orderBy: dau desc
- JSON示意（缩写，便于阅读）
  - {
      "id": "dau_by_region_v1",
      "datasource": "olap_main",
      "select": [
        { "expr": "region" },
        { "expr": "COUNT(DISTINCT user_id)", "alias": "dau" }
      ],
      "from": { "table": "user_events", "alias": "ue" },
      "where": {
        "op": "AND",
        "conditions": [
          { "left": "ue.event_name", "operator": "=", "right": { "param": "eventName" } },
          { "left": "ue.ts", "operator": ">=", "right": { "param": "startTs" } },
          { "left": "ue.ts", "operator": "<", "right": { "param": "endTs" } }
        ]
      },
      "groupBy": [ "region" ],
      "orderBy": [ { "expr": "dau", "direction": "DESC" } ],
      "limit": 100,
      "params": [
        { "name": "eventName", "type": "STRING", "required": true },
        { "name": "startTs", "type": "TIMESTAMP", "required": true },
        { "name": "endTs", "type": "TIMESTAMP", "required": true }
      ],
      "options": { "timeoutMs": 2000, "maxRows": 10000 }
    }

## 8. 索引、分区与物化建议
- 行为/日志表建议按时间分区（天/小时）与热点字段建索引/二级索引/跳表（视方言）。
- 高频聚合可用物化视图或预计算表；引擎侧通过from引用。
- 大宽表建议列式存储（如ClickHouse/Parquet湖仓）；select只取必要列以降低IO。

## 9. 数据质量与校验
- 参数校验：范围、格式、必填；时间范围上限（如最多90天）。
- 结果校验（可选）：返回行数、空值比例阈值告警。
- 元数据一致性：表/列变更检测与告警（依据元数据缓存刷新时落盘对比）。

## 10. 数据生命周期
- 模板版本：id+version，旧版本保留回溯；审计记录引用版本号。
- 缓存：计划/SQL/结果缓存均需TTL与容量限制；与数据变更事件联动失效（可选）。

## 11. 数据字典与变更流程
- 数据字典：来源于自动抽取+人工补充（描述、口径、示例）。
- 模板变更流程：草稿 -> 校验 -> 评审（白名单/限额视角） -> 发布 -> 审计追踪。
- 元数据刷新：定时+手动触发；失败重试与降级策略（使用旧缓存）。

## 12. 补充：交付与后续计划
- 近期交付物
  - SDK最小可用版本（MVP）：JSON模板->SQL生成->参数化执行->基础校验与监控
  - 使用样例与模板示例集
  - JUnit用例、JMH基准样例、JMeter压测脚本样例
- 后续演进
  - 增强DSL与优化器、覆盖更多方言、Server化治理与配额、模板版本与灰度