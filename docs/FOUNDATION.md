# 工程基础设施

本页说明 003 初始化范围。业务 REST 端点、完整 Runtime MVP 和三个 Demo 的状态以
[IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md) 为准。

## 本地构建

使用 JDK 21 和 Maven Wrapper（固定 Maven 3.9.16）：

```bash
./mvnw -B -ntp clean verify
./mvnw -B -ntp spotless:check
./mvnw -B -ntp spotless:apply
python3 scripts/quality/smoke-http.py
```

`apply` 只在主动进行格式修改时运行；CI 和 hook 都只检查。首次运行 wrapper 需要下载
Maven。Apple Silicon 若 PATH 中残留不可执行的 Intel `wget`，可用
`env PATH=/opt/homebrew/bin:/usr/bin:/bin ./mvnw -v` 完成首次下载，不修改全局环境。

## HTTP、日志和监控

Boot JAR 保留无子命令启动 MVC 的方式；`serve` 尚未实现。以下命令仅用于本地隔离验证，
先创建临时目录并使用绝对路径启动 JAR：

```bash
test_workspace=$(mktemp -d)
java -Doryxos.home="$test_workspace" -jar oryxos-boot/target/oryxos.jar \
  --server.address=127.0.0.1 --server.port=8080 --management.server.port=9091 \
  --spring.profiles.active=json-logs
```

结束后停止该进程并清理自己的临时目录。`scripts/quality/smoke-http.py` 自动管理临时
SQLite、随机回环端口和进程，适合重复验收；它不调用真实模型。

- 主端口默认为 8080，支持既有 Spring 参数覆盖；使用同步 MVC 和 Java 21 虚拟线程。
- 管理服务默认 `127.0.0.1:9091`；可用 `ORYXOS_MANAGEMENT_PORT` 或 Spring 参数改端口。
  默认只暴露 `/actuator/health`、`/actuator/info`、`/actuator/prometheus`；不开放 env、
  configprops、metrics。健康详情隐藏，liveness/readiness 不调用真实 Provider。
- 管理入口没有新增认证系统，依赖回环隔离；容器端口转发或更改监听地址前必须单独评估访问控制。
- 普通控制台日志便于本地阅读；`json-logs` 使用 Boot 内置 Logstash JSON，不另装 encoder。
  `/api/v1` 请求由过滤器产生 MDC `traceId`，记录方法、状态、耗时，结束时恢复上下文；
  不记录 URL 查询、请求体或原始异常，也不新增对外追踪响应头。
- CLI 继续将正常结果写 stdout、日志写 stderr；JSON 服务日志配置不改变 CLI 协议。
  日志不是审计替代品，现有 LLM/Tool 成功、失败、拒绝仍由审计链路写 SQLite。
- 不配置文件日志，因此本次没有新增文件轮转/留存策略。

## API 公共契约

公共类位于 `oryxos-web`，生产包中不增加测试 Controller 或业务接口。

- API 前缀 `/api/v1`；`ApiResponse<T>` 包含 `code/message/data/timestamp`。
  Controller 显式使用 `ApiResponse.success`，不对任意框架响应全局包壳。
- `GlobalExceptionHandler` 限定业务路径，覆盖参数 400、资源 404、内部 500、
  Provider 503、超时 504；Sandbox 拒绝保留 403。客户端收到固定安全消息。
- `@Utf8Length` 默认上限 32768 UTF-8 字节；后续消息 DTO 需结合 `@NotBlank` 和 `@Valid`。
  这是字段校验组件，不是所有 HTTP 请求体的统一传输大小限制。
- Tomcat 的 10 秒连接等待不是 Agent 执行期限；本次未实现业务 REST，
  因此未宣称同步 Agent 的 60 秒执行/取消语义已经完成。
- 默认关闭 `/v3/api-docs` 和 Swagger UI。显式启用 `local-docs` profile 时，主服务绑定
  回环地址，OpenAPI 在 `/v3/api-docs`，UI 在 `/swagger-ui/index.html`；扫描仅限 `/api/v1/**`。
  不为正常运行环境自动启用该 profile。框架文档和 Prometheus 保持各自原始协议。

## 质量检查与范围

| 工具 | 固定版本 | 执行范围与失败条件 |
| --- | --- | --- |
| Spotless / google-java-format | 3.10.4 / 1.36.1 | validate；生产和测试 Java；任何格式差异失败 |
| Checkstyle plugin / engine | 3.6.0 / 14.3.0 | validate；生产和测试 Java；非格式冲突的命名等规则，0 容忍 |
| PMD plugin / engine | 3.28.0 / 7.27.0 | verify；生产源码；启用规则的全部优先级失败 |
| SpotBugs plugin / engine | 4.10.5.0 / 4.10.5 | verify；生产字节码；FindSecBugs 1.14.0，Low 阈值，0 容忍 |
| Dependency-Check | 13.0.0 | 独立 profile 聚合；依赖 CVSS ≥ 7 或分析错误失败 |
| Gitleaks | 8.30.1 | 暂存快照/CI 跟踪源码；默认规则、脱敏输出；发现密钥失败 |

规则在 `config/quality/`，插件在父 POM 的实际 executions 中启用。
SpotBugs 的逐项例外记录在 `spotbugs-exclude.xml`：共享服务/Session/工厂的构造器注入、
固定类路径 DDL、随机 UUID 日志、URI ASCII 主机比较和第三方协议边界。
每项都限定类、方法/字段及规则，并注明重新审查条件；不全局关闭安全分类。
PMD 的两处资源例外分别说明 Picocli 借用输出流及 RuntimeHandle 接管 Spring 上下文。
Google 的 2 空格格式优先；不同时启用阿里手册的 4 空格格式。
合法 CLI stdout 不作为日志违规。静态扫描不证明没有漏洞，也不替代人工审查。

官方 P3C 2.1.1 基于旧 PMD，不能直接加载到本项目 Java 21 所需的 PMD 7。
用户已于 2026-11-04 确认采用 **PMD 7 + Checkstyle 兼容规则映射**。
自动检查包括部分命名、空 catch、资源关闭、equals/hashCode、字符串比较和 Thread.run 等；
逐项覆盖、默认规则的限制、例外及未覆盖条款的人工清单见
[阿里规约映射](../config/quality/alibaba-mapping.md)。
该方案已接入现有 Maven/CI 检查，不是运行官方 P3C，也不代表完整手册合规或人工审查已完成。

依赖漏洞检查单独运行：

```bash
./mvnw -B -ntp -Pdependency-audit package dependency-check:aggregate
```

构建与聚合放在同一次 Maven 调用中，确保尚未 install 的内部模块可由 reactor 解析。
`NVD_API_KEY` 通过环境/CI secret 注入，不能写到 POM 或命令行；当前选定工具在空 key 下
无法完成 NVD 更新，CI 明确要求配置该 secret。
用户本轮选择先完成其余验证，漏洞扫描保留待配置；未调整失败阈值或关闭 CI 的依赖审计门槛。
漏洞数据有效期 24 小时；首次同步可能较久，失败仍失败，不关闭 `failOnError`。
报告为根目录 `target/dependency-check-report.html/json`。聚合扫描 Maven 依赖与模块
`src/main/resources`，不包含测试依赖、Maven 构建插件和网站 npm 依赖；这些范围并未被宣称完成漏洞审计。

## Git hook 与 CI

安装 Gitleaks 8.30.1 后，`scripts/quality/check-staged.py` 用 `git checkout-index`
生成临时快照，运行密钥与格式检查。它不 `git add`、不改写文件、不改变暂存区；
未暂存编辑不会混入检查快照。

仓库提供 `.githooks/pre-commit`。激活之前先检查已有 `core.hooksPath` 和实际 hook；
没有既有机制时才执行 `git config --local core.hooksPath .githooks`，已有机制则合并调用。
提供 hook 文件与本机激活状态分别记录在验收记录中。

`java-ci.yml` 在 main push、PR 或手动触发时运行两个独立作业：`quality` 执行构建、
测试、静态扫描和密钥检查；`dependency-audit` 同步漏洞数据并聚合扫描。
现有网站发布流程保持不变。CI 不自动格式化、提交或部署。需要的远端合并检查是这两个
作业，但创建文件不等于远端已运行或分支保护已配置。

实际执行证据、未完成项见 [003 验收记录](../specs/003-project-foundation/quickstart.md)。
