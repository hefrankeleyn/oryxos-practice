# 003 基础设施初始化：使用与验收

核对日期：2026-11-04（Asia/Shanghai）。实际工作分支始终为 `main`。
运行方式及工具版本见 [工程基础设施](../../docs/FOUNDATION.md)。

## 基线保存

用户要求先保存全部本地未提交内容。开始时已位于 main，因此无需额外合并：

- `d969be1 feat: add provider runtime and audited CLI execution`：104 个文件。
- `bfcca5f docs: add project initialization guidance`：4 个文件。

两个提交保存了初始化前的全部 Git 可见改动；本次基础设施修改在这两个提交之后进行。
2026-11-05 按用户要求将初始化纳入后续 main 本地提交，NVD 扫描保留待办；未推送或部署。
既有 002 规格、真实 Provider 未验收状态及用户执行记录保留。

## 已执行验证

| 命令/检查 | 结果 | 范围 |
| --- | --- | --- |
| 初始化前 `mvn -o -B -ntp clean package` | 通过 | 原有九模块基线 |
| `./mvnw -v` | 通过 | Maven 3.9.16、Temurin JDK 21.0.12.1 |
| `./mvnw -o -B -ntp clean package` | 通过 | 初始化后九模块编译、测试及两种可执行 JAR |
| `./mvnw -o -B -ntp verify` | 通过 | 最终源码；格式、Checkstyle、测试、PMD、SpotBugs/FindSecBugs 实际执行 |
| JUnit XML 汇总 | 29 个测试类、63 项测试；62 通过、1 默认跳过，0 失败/错误 | 含原 Runtime/审计/CLI，以及新增 Web/HTTP、安全边界回归 |
| 九份 PMD 报告 | 0 违规、0 分析错误 | 自定义启用规则下的生产源码 |
| 九份 SpotBugs 报告 | 合计扫描 104 个生产 class，0 未处理违规、0 分析错误 | 已启用 FindSecBugs；逐项例外见质量配置及注释 |
| `./mvnw -o -B -ntp spotless:apply spotless:check` | 通过，121 个 Java 文件前后 SHA-256 完全相同 | 重复执行不改写已格式化源码 |
| `python3 scripts/quality/smoke-http.py` | 通过 | 两个真实 JAR、临时 SQLite、随机回环端口；所有进程随后停止 |
| 当前源码 Gitleaks 扫描 | 通过；真实 Git 索引不变 | 隔离临时索引包含本次改动，默认规则、脱敏输出；不代表全历史审计 |
| 初始两个新提交 Gitleaks 扫描 | 通过 | `HEAD~2..HEAD`，未扩展扫描全部历史 |
| XML/YAML/Python/shell 语法、`git diff --check` | 通过 | POM、规则、配置、workflow、检查脚本与 hook |

真实 JAR 验收覆盖：

- 默认与 `local-docs` 两种服务配置；独立管理端口的 health/info/prometheus 与探针，
  主端口不提供管理健康入口，env 等端点为 404。
- `/api/v1` 错误信封保留 404；JSON 日志每行均可解析，请求日志有 `traceId`，
  虚构敏感查询值不进入日志。单元/HTTP 测试另覆盖虚拟线程、MDC 清理和多字节输入边界。
- 默认 Swagger 主入口、WebJar 入口及编码路径均为 404；本地 profile 下 OpenAPI、
  Swagger 配置和 UI HTML 可访问，返回原生协议，不被 API 信封包装。
- Boot 与轻量 CLI 的 version/help/init，stdout 不含运行日志；这些命令不创建数据库或启动 HTTP。
  原 chat 和审计回归由集成测试覆盖，未调用真实付费模型。
- 主 JAR 和嵌套内部模块均未打入项目测试/fixture 类。

当前已验收产物 SHA-256：

```text
oryxos-boot/target/oryxos.jar
c370713c74f4798a3a0c9ddd81b8d69215c0a7ac70c9353880563f8a1a26e847

oryxos-cli/target/oryxos-cli-0.1.0-SNAPSHOT-exec.jar
edaddc23d1fd2a126cd2d35812d135b11bcf07744b4817401746101f7931d841
```

CLI 构建时间由 Maven 注入，再次打包可改变产物哈希。

## 检查失败路径与格式审查

所有负向样例均在仓库之外的可丢弃 fixture 中创建，没有提交故意漏洞或旧依赖：

1. 未格式化 Java 令 Spotless 返回非零；格式化后，Java 21 record、模式匹配 switch、
   文本块及 Lombok 样例可通过编译、PMD 和 SpotBugs/FindSecBugs。
2. `equals(null)` 被 PMD `EqualsNull` 拒绝；MD5 使用被 FindSecBugs
   `WEAK_MESSAGE_DIGEST_MD5` 拒绝，均为非零退出。
3. 暂存错误格式、工作树已修复时，hook 仍拒绝；暂存正确格式、工作树未暂存错误时，
   hook 通过。两次索引与工作树字节均不变化。
4. 暂存随机生成、无实际凭证含义的 AWS 格式样例后删除工作树文件，密钥检查仍拒绝；
   输出已脱敏且索引不变。

从已保存 HEAD 在隔离目录重新运行同版 formatter，生成了本机独立审查文件
`/tmp/oryxos-format-only.patch`：原有 110 个 Java 文件的纯 Google 格式迁移。
与该格式基线相比，另有 13 个原有文件包含明确的日志/静态分析修复或对应测试，列表在
`/tmp/oryxos-semantic-files.txt`；新增 Web 类和 HTTP 测试单独审查。两个临时审查文件不入库。

本仓库原先没有激活的提交 hook；现已执行 `git config --local core.hooksPath .githooks`。
没有覆盖既有 hook 或全局 Git 配置。远端 CI 尚未运行，分支保护尚未配置。

## 用户确认后的规约方案

用户于 2026-11-04 确认采用 PMD 7 + Checkstyle 兼容映射，其余条款保留人工清单。
已建立 [alibaba-mapping.md](../../config/quality/alibaba-mapping.md)，明确每项检查的范围与缺口；
仍然不加载官方 P3C，也不宣称完整手册合规。

包名规则显式限制为小写；空 catch 不能通过注释或 ignored/expected 名称绕过。
`./mvnw -o -B -ntp compile pmd:check` 对九模块通过，日志为 `/tmp/oryxos-alibaba-check.log`。
另在可丢弃目录验证了实际规则行为：

- Java 21 record 与 try-with-resources 的正常样例通过编译及规则检查。
- 错误类名、方法名及含大写字母的包名均被 Checkstyle 拒绝。
- 名为 ignored 的空 catch、未关闭资源、String 的 `==` 比较及仅重写 equals 的类型，
  分别被 EmptyCatchBlock、CloseResource、UseEqualsToCompareStrings、
  OverrideBothEqualsAndHashcode 拒绝；已核对报告中的规则名及非零退出码。

样例未加入仓库，临时目录已删除。对应日志为 `/tmp/oryxos-alibaba-fixture-positive.log`、
`/tmp/oryxos-alibaba-fixture-naming.log`、`/tmp/oryxos-alibaba-fixture-semantics.log`。

本轮只改规约配置和文档，没有修改 Java 业务源码或 JAR；上文的构建、63 项测试与真实 JAR
证据来自前一轮，不重复声称本轮重跑了整套运行验收。

## 未闭环事项

- **依赖漏洞扫描未完成**：实际执行了联网
  `./mvnw -B -ntp -DskipTests -Pdependency-audit package dependency-check:aggregate`。
  reactor 产物构建成功，扫描阶段因缺少有效 `NVD_API_KEY` 出现
  `NvdApiException: Invalid API Key` 与 `NoDataException: No documents exist`，非零退出。
  没有可用漏洞报告，不能据此声称依赖无高危漏洞。更新漏洞库的 `update-only` 也验证了同一阻塞。
  通过环境/CI Secret 配置有效 key 后重跑；不要使用 Maven `-o` 执行要求联网的 aggregate。
  用户已明确选择先完成其他验证，将漏洞扫描保留待配置。
  T016 的负向静态样例已完成，真实依赖扫描部分保持未完成；CI 审计门槛未关闭或改成忽略失败。
- 尚未执行远端 Actions、设置合并保护、部署、真实 DeepSeek 或最终三个业务 Demo。
  002 的真实验收任务保持原状态。
- 本次是 API 公共基础设施，不包含业务 Controller、产品 `/api/v1/health`、
  60 秒同步 Agent 执行/取消机制、全历史密钥审计或完整手册人工审查。

本机详细日志：`/tmp/oryxos-foundation-package2.log`、
`/tmp/oryxos-foundation-verify-final.log`、`/tmp/oryxos-packaged-smoke.log`、
`/tmp/oryxos-dependency-audit-online.log`；Maven 报告保存在各模块 `target/` 中。
