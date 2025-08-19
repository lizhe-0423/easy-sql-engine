# HTTP 压测脚本使用说明

此目录包含基于 HTTP API 的压测脚本模板 `easysql-http-demo.jmx`（占位）。

## 场景说明

假定你有一个 HTTP 服务端，暴露如下接口：
- POST /api/sql/build    （请求体为 JSON 模板或 DSL 参数，返回生成的 SQL）
- POST /api/sql/execute  （请求体包含模板与参数，返回查询结果概要）

## 使用步骤

1. 修改脚本中的 HTTP 请求默认服务器、端口、路径。
2. 调整线程组并发、持续时间与采样器定时器（固定思考时间或吞吐量控制器）。
3. 使用 `params.csv` 进行数据参数化（如 start_date、statuses 等）。
4. 可选：追加 `Backend Listener` 推送结果到 InfluxDB/Grafana。

## 注意事项

- 若你的接口需要认证（如JWT/Basic Auth），请在脚本中加入 Header Manager。
- 建议将请求体大小控制在合理范围，并开启 HTTP Keep-Alive。
- 生产压测请使用独立环境，避免对正式服务与数据库造成影响。