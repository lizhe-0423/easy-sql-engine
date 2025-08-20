# Easy-SQL 引擎 - TRD（技术方案/技术设计文档）

## 1. 架构概览
- 运行形态
  - SDK模式（优先）：以Java库的形式引入，调用API传入JSON模板或DSL对象，返回结果。
  - Server模式（可选）：嵌入SDK提供HTTP接口，便于跨语言调用与治理。
- 模块划分
  - template-json：JSON模板模型与JSON Schema校验
  - dsl-core：面向对象的查询DSL（Builder/Fluent API）
  - parser-builder：模板/DSL到抽象查询计划（LogicalPlan）与SQL字符串生成（方言感知）
  - dialect：方言适配（MySQL/ClickHouse/PostgreSQL…），内含函数映射、保留字、占位符策略
  - validator：结构与安全校验（必填、类型、白名单、黑名单、注入风险）
  - metadata：元数据访问（JDBC元数据/配置中心），本地缓存
  - executor：执行器（基于DataSource/JDBC），参数绑定、超时、限流、重试、结果映射
  - cache：查询计划缓存、SQL文本缓存、结果缓存（可组合开关）
  - monitor：指标与日志（Hutool Log、Micrometer可选），审计埋点
  - config：统一配置（数据源、方言、白名单、限额、缓存策略、超时）
  - spi-ext：扩展点（函数、操作符、hook、方言实现）
- 核心数据流：输入（JSON/DSL） -> 语义校验 -> LogicalPlan -> 优化（可选）-> SQL文本 + 参数 -> 执行器 -> 结果映射 -> 指标与审计

## 2. JSON模板与DSL输入规范（概要）
- JSON模板关键字段
  - select: 列清单（支持别名、表达式、聚合、窗口函数）
  - from: 主表（库/表/别名）
  - join: 多个join项（表、别名、类型、条件）
  - where: 条件树（支持AND/OR/NOT、比较、IN、BETWEEN、LIKE、正则方言化）
  - groupBy、having、orderBy、limit、offset
  - union/unionAll: 子模板数组
  - hints/options: 方言hint、执行超时、最大行数、扫描限制
  - params: 参数声明（名称、类型、是否必填、默认值、绑定策略）
- 对象DSL（示意）
  - Query.new().from("db.tbl").select(col("a").as("a1"), sum(col("b")))
    .join(inner("db.dim").on(eq(col("tbl.k"), col("dim.k"))))
    .where(and(gt(col("ts"), param("start")), lt(col("ts"), param("end"))))
    .groupBy(col("region")).orderBy(desc("cnt")).limit(100)
- 参数化策略
  - 所有外部传入值通过占位符绑定；不同方言占位符策略（?、:name）由dialect决定。
  - 列名、表名、函数名等结构性标识禁止作为可变参数传入（需白名单或安全转义）。

## 3. 方言适配设计
- 抽象接口 Dialect：
  - 占位符策略、转义策略、关键字与保留字
  - 函数映射（如日期、字符串、窗口函数的差异）
  - 限定语法（limit/offset、top、sample、final等方言差异）
  - 特性开关（CTE、窗口函数、正则、数组/Map类型）
- 扩展：通过SPI加载，按配置选择目标Dialect。

## 4. 计划与优化
- LogicalPlan节点类型：Project、Filter、Join、Aggregate、Window、Sort、Limit、Union、CTE等。
- 轻量优化器（可选启用）：
  - 谓词下推（到join前/子查询内）
  - 常量折叠与冗余条件消除
  - 列裁剪（最小列选择）
  - 方言hint注入（可配置）

## 5. 执行器与资源控制
- 执行器
  - 基于DataSource（外部注入），内部使用PreparedStatement，统一超时配置，最大行数限制。
  - 结果映射：ResultSet -> List<Map<String,Object>> 或自定义RowMapper。
- 资源与限额
  - 超时（per query）
  - 最大行数与最大扫描分区（方言/业务可配置）
  - 并发与连接池：推荐外部统一连接池（如HikariCP）通过DataSource提供；引擎不内置。

## 6. 校验与安全
- 模板结构与必填校验、类型校验、参数声明与绑定校验。
- 安全校验：表/库白名单、列白名单、函数白名单；危险语句与关键字拦截。
- 审计：记录模板ID、调用方、traceId、执行时间、影响行数、错误码。

## 7. 缓存策略
- 计划缓存：模板结构 -> 计划树/SQL片段缓存，命中降低生成开销。
- SQL文本缓存：模板+参数签名 -> 生成的SQL文本（注意参数化占位）。
- 结果缓存（可选）：短时缓存只读查询结果（具备失效策略与容量控制）。

## 8. 监控与日志
- 指标：生成耗时、执行耗时、返回行数、错误率、缓存命中率。
- 日志：模板ID、SQL摘要（脱敏）、异常堆栈（裁剪敏感）。
- Hutool日志门面；必要时对接Micrometer/Prometheus（可选）。

## 9. 配置与部署
- 配置项：数据源、目标方言、白名单、限额、缓存开关、监控开关、审计开关。
- 部署：
  - SDK：随业务服务一起部署，低延迟。
  - Server：单体或集群（无状态，后端复用同一数据源），网关/鉴权/限流放前置。

## 10. 测试与性能
- 单元测试（JUnit）：模板解析、方言转换、参数绑定、边界与异常。
- 基准测试（JMH）：计划生成耗时、SQL拼接耗时、缓存命中与未命中对比。
- 压力测试（JMeter）：典型报表与复杂join场景，配合真实数据规模评估QPS、p95、错误率。

## 11. 风险与对策
- 方言差异复杂：抽象清晰、分层良好、逐步覆盖。
- 模板滥用导致慢查询：白名单+限额+审计+离线评审策略。
- 参数校验不严导致注入：全参数化、禁结构性标识参数化、强校验。
- 元数据同步不及时：定时刷新+手动刷新接口+本地失效策略（使用旧缓存）。

## 12. SQL 优化建议技术方案（新增）

### 12.1 目标与范围
- 目标：为输入的 SQL 提供“证据化”的性能诊断与（可选）示例改写，形成结构化输出，便于在 UI/API 呈现与追溯。
- 范围（MVP）：以静态规则库为主（正则/轻量解析），不执行真实 SQL，仅对接 EXPLAIN（可选开关）。
- 非目标：不替代 DBA；不在未经验证的情况下自动改写并执行线上 SQL。

### 12.2 模块划分
- optimizer-core：规则接口、规则注册表、建议聚合器、评分与去重、数据模型（与DRD一致）。
- explain-adapter（规划）：按方言对接 EXPLAIN 并解析为统一计划结构（ExecutionPlan）。
- ast-adapter（规划）：集成 SQL AST 解析器（如JSQLParser/Calcite等），统一为轻量节点模型。
- optimizer-web（可选）：HTTP 控制器与/optimizer 页面（若以Server形态暴露）。

### 12.3 数据流
SQL 输入 -> 预处理（注释剥离、大小写规范化、长度/黑名单校验） -> 规则引擎（逐条规则检测） -> 建议列表（去重/聚合/排序） -> 可选：执行 EXPLAIN 并抽取要点 -> 证据合并（规则命中片段 + 计划要点）-> 结构化返回（OptimizationResult）。

### 12.4 数据模型（与 DRD 对齐）
- OptimizationResult：originalSQL, optimizedSQL, advice, reason。
- OptimizationSuggestion：advice, reason, optimizedSQL?。
- Evidence（规划）：items[{ruleId, snippet, location, impact}]；EXPLAIN 要点（算子、rows、key、extra）。

### 12.5 规则库设计
- 规则接口：Rule{id, name, severity, description, detect(ctx)->Optional<OptimizationSuggestion>}。
- 上下文：原始SQL、AST?、PLAN?、元数据（可选）、方言。
- 注册与优先级：系统规则（内置）< 组织规则 < 团队规则 < 项目/应用规则（就近覆盖）。
- 执行策略：
  - 快速失败型（如安全黑名单、时间范围强制）；
  - 可累积型（风格与可读性）；
  - 互斥/去重策略（如 OR→IN 与 UNION ALL 提示冲突时只保留收益更大者）。
- 例规（MVP）：
  - SELECT * -> 列裁剪建议；
  - 无 WHERE -> 风险高，强制给出时间/分区维度建议；
  - ORDER BY 无 LIMIT -> 建议增加 LIMIT 或采用分页策略；
  - LIKE '%xxx' 前缀缺失 -> 建议前缀索引或改写为右匹配；
  - NOT IN / 子查询 -> 考虑改 EXISTS / JOIN；
  - 多个 OR 等值 -> 提示使用 IN 或拆分 UNION ALL；
  - HAVING 过滤 -> 尝试谓词下推至 WHERE；
  - 标量子查询 -> 评估改为 JOIN；
  - RAND()/随机排序 -> 风险提示；
  - OFFSET 大分页 -> 游标/基于主键的 seek 分页；
  - 函数包装索引列 -> 建议改写以命中索引。

### 12.6 EXPLAIN 接入（规划）
- MySQL 优先：优先使用 EXPLAIN FORMAT=JSON；若不支持则解析传统表格输出。
- 解析字段：select_type/table/type/key/possible_keys/rows/filtered/Extra。
- 风险判定示例：
  - type=ALL（全表扫描）；
  - Extra 包含 Using temporary / Using filesort；
  - 估算行数 rows 过大且 filtered 低。
- 安全与开关：仅执行 EXPLAIN，不执行 DML/DDL；默认关闭，可通过配置/请求参数 enableExplain=true 启用。

### 12.7 AST 解析（规划）
- 解析器选择：JSQLParser/Calcite（择一），统一映射到轻量 AST 节点模型。
- 用途：
  - 更精准的 OR/IN/UNION 识别；
  - 谓词下推机会识别；
  - 列裁剪、连接条件完整性校验；
  - 子查询分类（相关/不相关、标量/集合）。

### 12.8 AI 诊断补充层（规划）
- 输入：原始SQL + 计划要点摘要 + 规则命中清单 + 组织规范片段。
- 输出：结构化 JSON（建议/依据/风险等级/示例改写片段/注意事项）。
- 约束：AI 仅作为“描述性补充”与“改写草案”来源，最终结论需由规则/AST/PLAN 校验通过后才返回到前端。

### 12.9 API 设计
- POST /api/optimizer/analyze
  - Request: { sql: string, dialect?: string, datasource?: string, enableExplain?: boolean, timeoutMs?: number }
  - Response: { originalSQL, optimizedSQL?, advice, reason, evidence? }
  - 错误：400（参数错误）、422（SQL 非只读/不允许）、504（EXPLAIN 超时）、501（方言未支持）。
- GET /optimizer（可选）：简单输入页面，供人工试用与演示。

### 12.10 安全与合规
- 黑名单：禁止 DML/DDL/事务控制/数据定义/危险函数；
- 白名单：数据源与 schema/table 访问范围（可选）；
- 参数限制：SQL 文本最大长度、禁止多语句、禁止分号拼接；
- 脱敏：日志仅记录 SQL 摘要（哈希/前缀），不落参数值；
- 只读：仅 EXPLAIN 权限；禁止执行原 SQL。

### 12.11 观测与调试
- 指标：请求数、规则命中数、各规则耗时、EXPLAIN 成功率与耗时、建议条数分布。
- 日志：traceId、数据源/方言、规则命中明细、裁剪后的 SQL 片段。
- 采样：对长 SQL/复杂计划开启采样日志，避免全量落盘。

### 12.12 测试策略
- 规则单测：给定 SQL 输入断言建议输出；
- 快照测试：典型复杂 SQL 的建议列表稳定性；
- 解析鲁棒性：异常字符、注释、大小写、空白、超长 SQL；
- EXPLAIN 适配：不同版本 MySQL 的字段兼容性用例。

### 12.13 演进与里程碑
- M2：SQL 优化建议原型（静态规则 + UI/API）。
- M3：EXPLAIN 解析与证据化输出，对接一类方言（MySQL）。
- M4：AST/PLAN 复合规则 + AI 诊断补充层 + 业务规则接入。