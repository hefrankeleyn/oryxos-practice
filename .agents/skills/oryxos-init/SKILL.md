---
name: oryxos-init
description: >-
  初始化或增量补齐当前 OryxOS 项目的 Java 工程基础设施：日志、监控、HTTP Server、
  API 规范、Google Java 格式、阿里编码规约、代码安全检查及 Maven/CI 验证。
  当用户要求搭建工程骨架、初始化项目或补齐这些基础设施时使用；仅处理指定范围，
  不用于重建已有 Runtime、实现业务端点或初始化运行时 .oryxos 工作区。
---

# OryxOS 项目初始化

以当前仓库为起点补齐工程基础设施。完整初始化覆盖下列步骤；用户只指定其中一项时，
只执行该项及其必要验证。创建、修改或评估本 Skill 本身，不代表执行项目初始化。

## 开始前：识别现状与范围

1. 用 `git rev-parse --show-toplevel` 定位项目根目录，检查 `git status --short`，
   记录已有改动并保留。检查目标目录适用的 `AGENTS.md`。
2. 阅读根 `AGENTS.md`、`.specify/memory/constitution.md`、
   `docs/IMPLEMENTATION_STATUS.md`，再核对相关 POM、实际源码、启动配置与测试。
   状态文档可能滞后，模块名称和 POM 描述不证明能力已实现。
3. API 契约按需查 `docs/03-TechnicalSolution.md` 第 7 章；开发流程查
   `docs/04-AiProgrammingGuide.md`。`docs/prompt/` 不作为设计来源、不修改。
4. 列出“已有能力、缺口、拟修改文件、验证方式”。从现有 POM 和配置推导参数，
   不重复询问已有的 groupId、模块、Java 版本和端口；真正缺失或冲突的决定再澄清。
5. 跨模块完整初始化沿用项目的 Spec-Kit 流程；需要规格、计划、任务时使用现有
   `speckit-specify`、`speckit-plan`、`speckit-tasks`，实施时按对应 tasks 推进。
   工程基础设施使用独立范围，保留已有业务功能的规格和未完成验收项。

新增用户可见接口、响应字段、提示或 Swagger 页面，先核对是否在已确认范围内；
额外内容先给出具体方案与影响，取得确认后再实现。已授权的内部配置、修复和验证继续执行。
监控与文档入口先在本地隔离环境验证，发布和正常环境启用遵循用户的授权范围。

## 项目约束

以当前用户要求、已确认 constitution 和适用 `AGENTS.md` 为准。发现未解决的来源冲突，
说明冲突及影响，不改写 constitution 来消除冲突。

- 保持 JDK 21、Spring Boot 3.x、Maven 九模块、Spring MVC 与 virtual thread。
  核对并保留当前依赖基线，不以初始化为由升级到最新大版本或引入 WebFlux。
- 九模块为 `oryxos-core`、`oryxos-provider`、`oryxos-memory`、`oryxos-tool`、
  `oryxos-channel-cli`、`oryxos-web`、`oryxos-storage`、`oryxos-cli`、`oryxos-boot`。
  core 保持内部模块独立；boot 负责聚合，公共 Web 基础设施放在 web。
- 保持 SQLite、显式 DDL 与 `ddl-auto: none`。保留已有调用审计，不重建或清空工作区、
  数据库和业务表，不把审计实现降为建表占位或普通日志。
- 保留已有 Provider/ReAct/Memory/Tool、CLI 和打包行为；基础设施初始化不扩展业务能力。
  后续 REST 业务接入仍调用 `AgentService.process(Session, String)`。
- 密钥由环境等外部注入；日志和扫描报告不得泄露密钥、完整 Prompt、敏感 Tool 参数或原始模型响应。
- 不自动提交、推送或部署；用户要求提交时只暂存明确目标文件。

## 实施步骤

### 1. 复用骨架与锁定兼容版本

核对父 POM、模块依赖、根包 `com.oryxlabs.oryxos`、Boot 主类和可执行 JAR。
已有骨架直接复用，仅创建确实缺失且在范围内的文件；保留 Boot Runtime 与独立 CLI 的职责。

新增依赖及插件先查实际 JDK/Boot/BOM 兼容矩阵和官方文档，锁定兼容版本。
第三方库由根 POM 的 properties/dependencyManagement 管理；插件版本统一放在根 POM。
质量插件还要在实际构建的 plugins/executions 中启用，单写 pluginManagement 不算接入。
不要将参考 Skill 的示例版本、插件参数或 ruleset 路径当作已验证配置。

### 2. 日志

- 复用 SLF4J/Logback，按已有运行模式配置可读控制台日志与需要的 JSON 结构化日志。
  优先评估当前 Boot 的内置能力；只有确有缺口才增加兼容的 JSON encoder。
- HTTP 请求关联标识使用 MDC；检查入口、异常出口及 virtual thread 切换时的上下文，
  在请求结束时清理。未经约定不新增对外响应字段或响应头。
- 保留敏感日志抑制配置；检查异常 cause、HTTP 客户端和 SQL 绑定日志是否泄露原值。
  如启用文件日志，明确滚动、保留和工作区路径。
- 日志不得混入 CLI 的命令结果：保留 CLI 日志到 stderr、正常结果到 stdout 的契约。
  禁止用 `System.out` 写日志，不把合法的 CLI 结果输出判为违规。
- LLM/Tool 审计仍落库；JSON 日志、MDC 和 Actuator 均不替代审计。

### 3. 监控

在合适的运行模块接入 Actuator、Micrometer 与 Prometheus registry，避免使轻量 CLI
意外启动 HTTP 服务。指标标签保持有界，不用 sessionId、requestId、Prompt 或任意用户输入作标签。

根据已确认范围选择 `health`、`info`、`prometheus`，需要时再加 `metrics`；明确管理地址、
端口、访问控制与启用环境，不使用 `exposure.include: "*"`。
健康详情默认不向普通调用方展开；探针不调用真实模型、不消耗付费配额。
区分管理端点 `/actuator/health` 与产品契约 `/api/v1/health`，不暗中更换路径或响应结构。

### 4. HTTP Server

复用 `spring-boot-starter-web` 和内嵌 Tomcat，检查 `spring.threads.virtual.enabled=true`。
保留现有端口覆盖和启动入口；CLI 的 init/chat/help/version 不得因监控或日志配置而意外开启 Web。
按已有需求实现输入大小、超时等基础约束，不额外创建业务 Controller、认证系统或调度功能。
`oryxos serve` 只有在相应命令已实现或本次明确要求时才作为可用启动方式。

### 5. API 公共规范

在 `oryxos-web` 复用或补齐以下公共能力，不在 core 引入 Servlet/Web 依赖：

- 按现有技术方案使用 `/api/v1`、HTTP 状态码、统一错误码和输入校验。
- 成功与错误统一采用 `ApiResponse<T>`：`code`、`message`、`data`、`timestamp`。
  如已有不同契约，先核对采纳依据；不再另建 `errorCode` 信封或无说明改成 ProblemDetail。
- `GlobalExceptionHandler` 覆盖相关参数校验、资源不存在、内部失败、Provider 故障及已约定超时；
  不将所有错误包装成 HTTP 200，不向调用方透出堆栈或原始异常。
- 使用与当前 Boot 匹配的 springdoc WebMVC 集成。OpenAPI 和 Swagger UI 的路径、
  开启环境及访问方式按确认的范围配置，并实际验证，不默认向正常用户开放文档 UI。
- 响应包装与异常处理限定业务 API，不破坏 Actuator、Prometheus 文本输出或 OpenAPI 文档格式。

### 6. Google 格式、阿里规约与安全检查

执行此步骤前阅读 [质量检查配置与兼容验证](references/quality-gates.md)。
按其中的职责分工配置 Spotless/google-java-format、阿里 P3C、Checkstyle、PMD、
SpotBugs/Find Security Bugs、OWASP Dependency-Check 及敏感信息检查。

Google 负责格式、阿里规则负责非冲突的编码约束；这是一套项目组合规则，
不宣称完全满足两份手册的所有条款。兼容性与覆盖缺口必须说明，不能用空规则或跳过检查伪造通过。

### 7. CI 与本地提交前检查

完整初始化包含 Java CI；局部任务按用户指定范围处理。保留现有网站发布工作流，
将 Java 构建、测试、格式、规约和安全检查接入对应 workflow。
CI 只检查，不自动格式化、提交或发布。明确每项检查触发条件及失败结果。

本地 hook 复用已存在机制，优先检查暂存内容；不能把用户未暂存的编辑混入提交，
也不能自动 stage 或改写全仓库。提供或修改仓库内 hook 文件与实际激活 hook 分开记录；
激活时保留已有 hook，不覆盖全局 Git 配置。
CI 文件存在、CI 已运行和远端分支保护已生效分别报告；不自动修改远端仓库规则。

## 验证与交付

验证使用临时工作区、临时 SQLite 和本地 fixture，不连接真实 Provider 或生产系统。
依赖下载及漏洞库同步使用正常工具链渠道，不将它们误报为已执行真实业务验收。

| 范围 | 必要证据 |
| --- | --- |
| 依赖、打包或启动变更 | `mvn clean package`；质量检查已接入后运行 `mvn verify`，确认实际执行而非被跳过 |
| HTTP/监控/API | 临时目录运行实际 Boot JAR，主服务和独立管理端口均绑定回环地址；验证健康、指标、公开范围与统一错误格式，结束后停止本次进程 |
| 日志 | 解析 JSON、检查关联标识及清理，使用虚构敏感值验证脱敏，验证 CLI stdout/stderr 分离 |
| CLI 与 Runtime | 回归已有版本/帮助/init/chat 相关可控测试；验证两种打包方式不受影响 |
| 质量检查 | 在可丢弃 fixture 中确认格式或规则违规返回失败；不得向工作树提交故意漏洞或有 CVE 的旧依赖 |
| 重复执行 | 复用同名配置、规则、插件和 workflow；不重复添加依赖或重写已完成部分 |

启动命令、JAR 文件名和端口以当前产物及配置为准；避免直接在真实 `.oryxos` 下启动服务。
扫描所需的漏洞库、网络或兼容工具不可用时，报告未完成项与原因，不记作通过。

完成时说明修改文件、验证命令和结果、未覆盖检查及外部验收、保留的用户原有改动。
只有实现与相应验证证据具备后才更新 `docs/IMPLEMENTATION_STATUS.md`；
基础设施完成不等于 Runtime MVP 或三个业务 Demo 已验收。
