# Easy-SQL 引擎

轻量、通用、高性能的动态SQL生成与执行引擎。基于JSON模板描述复杂查询，支持参数化和多种数据库方言。

## 文档概览

- [PRD（产品需求文档）](./PRD.md)
- [TRD（技术方案/技术设计文档）](./TRD.md)
- [DRD（数据设计文档）](./DRD.md)

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>com.easysql</groupId>
    <artifactId>easy-sql-engine</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 基本使用

#### 1. JSON模板定义

创建模板文件 `user-query.json`：

```json
{
  "id": "user_query",
  "select": [
    {"expr": "u.id", "alias": "user_id"},
    {"expr": "u.name", "alias": "user_name"},
    {"expr": "u.email"},
    {"expr": "p.name", "alias": "profile_name"}
  ],
  "from": {
    "schema": "test",
    "table": "user",
    "alias": "u"
  },
  "joins": [
    {
      "type": "LEFT",
      "table": {"schema": "test", "table": "profile", "alias": "p"},
      "on": [
        {"left": "u.id", "operator": "=", "right": {"value": "p.user_id"}}
      ]
    }
  ],
  "where": {
    "op": "AND",
    "conditions": [
      {
        "leaf": {
          "left": "u.status",
          "operator": "=",
          "right": {"value": "active"}
        }
      },
      {
        "leaf": {
          "left": "u.created_at",
          "operator": ">=",
          "right": {"param": "start_date"}
        }
      }
    ]
  },
  "orderBy": [
    {"expr": "u.created_at", "direction": "DESC"}
  ],
  "limit": 100,
  "params": [
    {
      "name": "start_date",
      "type": "DATE",
      "required": true,
      "description": "查询起始日期"
    }
  ]
}
```

#### 2. 生成SQL

```java
import com.easysql.engine.EasySQLEngine;
import com.easysql.engine.TemplateMapper;
import com.easysql.engine.model.Template;

// 从JSON文件加载模板
Template template = TemplateMapper.fromResource("user-query.json");

// 创建引擎实例
EasySQLEngine engine = new EasySQLEngine();

// 生成SQL
String sql = engine.buildSQL(template);
System.out.println(sql);
// 输出：SELECT u.id AS `user_id`, u.name AS `user_name`, u.email, p.name AS `profile_name` 
//       FROM `test`.`user` `u` LEFT JOIN `test`.`profile` `p` ON u.id = p.user_id 
//       WHERE (u.status = 'active') AND (u.created_at >= :start_date) 
//       ORDER BY u.created_at DESC LIMIT 100
```

#### 3. 执行SQL查询

```java
import com.easysql.engine.executor.JDBCSQLExecutor;
import com.easysql.engine.executor.SQLExecutor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

// 建立数据库连接
Connection conn = DriverManager.getConnection(
    "jdbc:mysql://localhost:3306/mydb", "user", "password");

// 准备参数
Map<String, Object> params = new HashMap<>();
params.put("start_date", Date.valueOf("2023-01-01"));

// 执行查询
JDBCSQLExecutor executor = new JDBCSQLExecutor();
SQLExecutor.QueryResult result = executor.executeQuery(conn, sql, template, params);

// 处理结果
System.out.println("查询到 " + result.getRowCount() + " 行数据");
System.out.println("列名：" + result.getColumnNames());

for (Map<String, Object> row : result.getRows()) {
    System.out.println("用户ID: " + row.get("user_id") + 
                      ", 姓名: " + row.get("user_name"));
}
```

## 核心特性

### 1. 灵活的模板系统

- **JSON模板**：声明式描述复杂SQL查询
- **参数化**：支持命名参数，类型安全
- **嵌套条件**：支持AND/OR/NOT逻辑组合
- **多表关联**：支持各种JOIN类型

### 2. 数据库方言支持

```java
import com.easysql.engine.dialect.MySQLDialect;

// 使用特定方言
EasySQLEngine engine = new EasySQLEngine(new MySQLDialect());
```

### 3. 参数类型支持

支持的参数类型：
- `STRING` - 字符串
- `INT` - 整数
- `BIGINT` - 长整数
- `DOUBLE` - 双精度浮点数
- `BOOLEAN` - 布尔值
- `DATE` - 日期
- `TIMESTAMP` - 时间戳

## 高级用法

### 复杂查询示例

```java
// 从JSON字符串直接解析
String jsonTemplate = """
{
  "id": "complex_query",
  "select": [
    {"expr": "COUNT(*)", "alias": "total"},
    {"expr": "AVG(amount)", "alias": "avg_amount"}
  ],
  "from": {"table": "orders", "alias": "o"},
  "joins": [
    {
      "type": "INNER",
      "table": {"table": "users", "alias": "u"},
      "on": [{"left": "o.user_id", "operator": "=", "right": {"value": "u.id"}}]
    }
  ],
  "where": {
    "op": "AND",
    "conditions": [
      {
        "leaf": {"left": "o.status", "operator": "IN", "right": {"param": "statuses"}}
      },
      {
        "op": "OR",
        "conditions": [
          {"leaf": {"left": "u.level", "operator": "=", "right": {"value": "VIP"}}},
          {"leaf": {"left": "o.amount", "operator": ">", "right": {"param": "min_amount"}}}
        ]
      }
    ]
  },
  "groupBy": ["u.level"],
  "having": {
    "leaf": {"left": "COUNT(*)", "operator": ">", "right": {"value": "5"}}
  },
  "orderBy": [{"expr": "total", "direction": "DESC"}],
  "params": [
    {"name": "statuses", "type": "STRING", "required": true},
    {"name": "min_amount", "type": "DOUBLE", "required": false, "defaultValue": "1000.0"}
  ]
}
""";

Template template = engine.parseAndBuild(jsonTemplate);
String sql = engine.buildSQL(template);
```

### 自定义方言

```java
import com.easysql.engine.dialect.SQLDialect;

public class CustomDialect implements SQLDialect {
    @Override
    public String escapeIdentifier(String identifier) {
        return "[" + identifier + "]"; // SQL Server style
    }
    
    @Override
    public String escapeString(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
    
    @Override
    public String limitSQL(String baseSql, Integer limit, Integer offset) {
        if (limit == null) return baseSql;
        return baseSql + " OFFSET " + (offset != null ? offset : 0) + 
               " ROWS FETCH NEXT " + limit + " ROWS ONLY";
    }
    
    // 实现其他方法...
}
```

## 性能与最佳实践

### 1. 模板缓存

```java
// 推荐：重用引擎实例和预编译模板
EasySQLEngine engine = new EasySQLEngine();
Template template = TemplateMapper.fromResource("user-query.json");

// 多次使用相同模板
for (Map<String, Object> paramSet : parameterSets) {
    String sql = engine.buildSQL(template);
    SQLExecutor.QueryResult result = executor.executeQuery(conn, sql, template, paramSet);
    // 处理结果...
}
```

### 2. 连接池使用

```java
// 推荐：使用连接池
DataSource dataSource = // 配置连接池...
try (Connection conn = dataSource.getConnection()) {
    SQLExecutor.QueryResult result = executor.executeQuery(conn, sql, template, params);
    // 处理结果...
}
```

## API 参考

### EasySQLEngine

| 方法 | 描述 |
|------|------|
| `buildSQL(Template)` | 从模板生成SQL |
| `parseAndBuild(String)` | 从JSON字符串解析并生成SQL |

### TemplateMapper

| 方法 | 描述 |
|------|------|
| `fromResource(String)` | 从classpath资源加载模板 |
| `fromFile(String)` | 从文件路径加载模板 |
| `fromJson(String)` | 从JSON字符串解析模板 |

### JDBCSQLExecutor

| 方法 | 描述 |
|------|------|
| `executeQuery(Connection, String, Template, Map)` | 执行查询 |
| `executeUpdate(Connection, String, Template, Map)` | 执行更新 |

## 里程碑

- ✅ **M1**：MySQL方言 + JSON模板 + 参数化执行 + 基础校验 + 单元测试
- 🚧 **M2**：对象DSL + 元数据缓存 + 监控指标 + 简单优化器
- 📋 **M3**：多方言适配 + Server模式（HTTP）+ 模板版本与审计

---

## 演示运行（推荐截图用）

内置了一个可运行的演示测试：读取 JSON 模板 → 生成 SQL → 绑定参数 → 在内存 H2 执行 → 打印结果（包含表格、列名、耗时）。

- 演示类位置：`src/test/java/com/easysql/engine/demo/EasySQLDemo.java`
- 模板文件位置：`src/test/resources/template-example.json`

运行命令（需安装 JDK8+ 与 Maven）：

```bash
mvn -q -Dtest=com.easysql.engine.demo.EasySQLDemo test
```

预期控制台输出（节选）：

```
================================================================================
                     Easy-SQL Engine Demo
              JSON模板 → SQL生成 → 参数绑定 → 执行查询
================================================================================

📄 模板信息:
  模板ID: user_query
  数据源: test_db
  方言: mysql

🔧 生成的SQL:
SELECT u.id AS `user_id`, u.name AS `user_name`, u.email, p.name AS `profile_name` ...

📋 参数绑定:
  start_date     : 2022-12-31 (Date)

📊 执行结果:
  查询行数: 2 行
  执行耗时: 3 ms
  列数量: 4 列

| user_id        | user_name      | email          | profile_name   |
|---------------------------------------------------------------|
| 1              | Alice          | alice@...      | AliceProfile   |
| 3              | Charlie        | charlie@...    | CharlieProfile |
```

如果遇到编译或类版本问题（如 “class file has wrong version 55.0, should be 52.0”），请先清理再编译：

```bash
mvn -q clean test
```

或者删除 `target/` 目录后再执行上述命令，确保使用的 JDK 版本与 `pom.xml` 中的 `1.8` 设置一致。

---

## template-example.json 使用说明

本仓库提供的示例模板文件位于 `src/test/resources/template-example.json`，用于演示如何声明一个跨表查询并进行参数化过滤、排序与限制。

关键字段说明：
- `id`: 模板唯一标识，便于日志与缓存。
- `datasource`: 逻辑数据源名称（示例中仅用于展示）。
- `dialect`: 方言名称，例如 `mysql`（影响标识符转义、limit 语法等）。
- `select`: 选择列数组，支持 `expr` 表达式与可选 `alias` 别名。
- `from`: 基表，包含 `schema`、`table`、`alias`。
- `joins`: 关联表数组，每个包含 `type`（left、inner...）、`table` 与 `on` 条件。
- `where`: 过滤条件树，支持嵌套 `op`（AND/OR/NOT）与 `leaf`（比较）结构。
- `orderBy`: 排序字段及方向（ASC/DESC）。
- `limit`: 限制返回行数（可配合 `offset`）。
- `params`: 参数列表，定义参数名、类型、是否必填、默认值等。
- `options`: 执行期选项（如超时、最大行数、只读开关等）。

最小使用示例：

```java
import com.easysql.engine.EasySQLEngine;
import com.easysql.engine.TemplateMapper;
import com.easysql.engine.executor.JDBCSQLExecutor;
import com.easysql.engine.executor.SQLExecutor;
import com.easysql.engine.model.Template;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

// 1) 加载模板
Template template = TemplateMapper.fromResource("template-example.json");

// 2) 生成SQL
EasySQLEngine engine = new EasySQLEngine();
String sql = engine.buildSQL(template);

// 3) 执行（以 H2 内存库为例，业务中替换为你的 DataSource/连接）
try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:demodb;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "")) {
    Map<String, Object> params = new HashMap<>();
    params.put("start_date", Date.valueOf("2022-12-31"));

    JDBCSQLExecutor executor = new JDBCSQLExecutor();
    SQLExecutor.QueryResult result = executor.executeQuery(conn, sql, template, params);

    System.out.println("行数: " + result.getRowCount());
    System.out.println("列: " + result.getColumnNames());
}
```

生产项目中建议将模板放置在 `src/main/resources/`，并通过 `TemplateMapper.fromResource("your-template.json")` 加载；本仓库的演示模板位于 `src/test/resources/`，便于随演示测试一并运行。