# JMeter 压测脚本

本目录包含了针对 Easy-SQL 引擎的 JMeter 压测脚本模板。

## 目录结构

```
jmeter/
├── README.md                    # 本文件
├── http/                        # HTTP API 压测脚本
│   ├── easysql-http-demo.jmx   # 主脚本
│   ├── params.csv              # 参数化数据
│   └── README.md               # HTTP 压测使用说明
└── jdbc/                       # JDBC 直连压测脚本
    ├── easysql-jdbc-demo.jmx   # 主脚本
    ├── templates.csv           # 模板数据
    └── README.md               # JDBC 压测使用说明
```

## 快速开始

1. **安装 JMeter**：
   ```bash
   # 下载 Apache JMeter 5.x
   wget https://downloads.apache.org//jmeter/binaries/apache-jmeter-5.5.tgz
   tar -zxf apache-jmeter-5.5.tgz
   cd apache-jmeter-5.5/bin
   ```

2. **选择压测场景**：
   - **HTTP 压测**：先启动 Demo Server，再运行 `http/easysql-http-demo.jmx`
   - **JDBC 压测**：直接运行 `jdbc/easysql-jdbc-demo.jmx`（针对内存库或测试库）

3. **修改配置**：
   - 线程组：并发用户数（推荐从 10 开始）
   - Ramp-up Period：梯度加压时间
   - 循环次数或持续时间
   - 目标服务器 IP/端口
   - 数据库连接参数

4. **运行压测**：
   ```bash
   # GUI 模式（开发调试用）
   ./jmeter.sh

   # 命令行模式（推荐用于正式压测）
   ./jmeter.sh -n -t /path/to/easysql-http-demo.jmx -l results.jtl -e -o report/

   # 查看报告
   open report/index.html
   ```

## 性能基线参考

以下数据基于 H2 内存库与单机环境，仅供参考：

| 场景 | 并发数 | 持续时间 | 平均吞吐量 | P95 延迟 | 错误率 |
|------|--------|----------|------------|----------|--------|
| 简单查询构建 | 50 | 60s | ~2000 req/s | <5ms | <0.1% |
| 复杂查询构建 | 20 | 60s | ~800 req/s | <15ms | <0.1% |
| 查询执行（内存库） | 10 | 60s | ~500 req/s | <20ms | <0.5% |

**注意**：实际性能受硬件、JVM 配置、数据库类型与网络环境影响，请以自己环境的测试结果为准。

## 监控指标

压测时建议监控：

1. **应用指标**（通过 `MetricsCollector` 暴露）：
   - 构建成功/失败次数
   - 构建耗时分布（P50/P95/P99）
   - 执行成功/失败次数
   - 执行耗时分布

2. **系统指标**：
   - CPU 使用率
   - 内存使用率
   - GC 频率与耗时
   - 线程数

3. **数据库指标**：
   - 连接池使用率
   - 慢查询数量
   - 锁等待时间

## 故障排查

- **高错误率**：检查线程数是否过高，数据库连接池是否耗尽
- **高延迟**：检查 GC 状态，考虑调整 JVM 参数
- **吞吐量低**：检查 CPU 瓶颈，考虑优化 SQL 模板复杂度
- **内存泄漏**：长时间压测后检查堆内存趋势，关注 DSL 对象与连接释放

有关更多 JMeter 使用技巧，请参考 [Apache JMeter 官方文档](https://jmeter.apache.org/usermanual/index.html)。