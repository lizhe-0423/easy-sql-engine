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