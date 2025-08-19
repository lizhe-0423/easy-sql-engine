# JDBC 压测脚本使用说明

此目录包含基于 JDBC 直连的压测脚本模板 `easysql-jdbc-demo.jmx`（占位）。

## 场景说明

- 直接在 JMeter 中配置 JDBC Connection Configuration（推荐使用连接池驱动）。
- 通过 JDBC Request 发送 SQL（由 EasySQLEngine 生成的 SQL，或脚本中静态 SQL）。

## 使用步骤

1. 配置 JDBC 连接串、驱动类名、用户名、密码。
2. 在 `templates.csv` 或 PreProcessor 中准备 SQL 模板与参数。
3. 在 JDBC Request 中引用变量，执行查询或更新。
4. 使用 `View Results in Table` 或 HTML 报告查看吞吐与延迟。

## 注意事项

- 使用测试库/只读账号进行压测；
- 控制并发避免连接池耗尽或目标库过载；
- 针对高并发场景，建议提升 fetchSize、使用只读事务。