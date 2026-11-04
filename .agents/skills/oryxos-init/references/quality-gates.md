# 质量检查配置与兼容验证

在配置或调整格式、编码规约、安全插件和相应 CI 时读取。
本文件定义选型和验收边界，不提供未经当前依赖验证的整段 POM 模板。

## 先验证工具组合

记录 JDK、Boot、Maven、插件、分析引擎及规则扩展的实际版本。
分别管理 `maven-pmd-plugin` 与 PMD 引擎、Spotless 与 google-java-format、
SpotBugs Maven 插件与分析引擎的版本，不用一个模糊属性代替不同组件。
沿用 Boot 管理的依赖时先核对 BOM，避免重复覆盖。

新引入的分析器用实际源码中的代表性语法做可丢弃 fixture 验证，
例如 Java 21 record、模式匹配 switch、文本块和 Lombok 生成代码。
区分“运行扫描的 JVM 版本”“源码语法支持”和“class 文件支持”；
插件能启动不等于能正确分析项目。

## 格式与编码规约

| 工具 | 职责 | 配置边界 |
| --- | --- | --- |
| Spotless + google-java-format | 自动格式化与一致性检查 | 锁定 formatter 版本，使用 GOOGLE 风格；校验实际 import 顺序，避免再叠加相互冲突的排序器 |
| `.editorconfig` | 编辑器基础设置 | Java 的缩进和换行与 formatter 一致；按文件类型设置，不全局改写 YAML/XML/Markdown |
| Checkstyle | 补充命名、导入等约束 | 按当前版本审查 Google 规则集，删除或协调与 formatter 重复冲突的规则 |
| PMD + 阿里 P3C | 可机器检查的编码规约 | 验证引擎/API/规则集兼容性；保留未覆盖手册条目的人工审查 |

Google 的块缩进为 2 空格，阿里手册的格式条款使用 4 空格；本组合采用 Google 格式，
不同时开启两套缩进规则。阿里手册中的命名、集合、异常、并发等规则也需逐项核对适用性：

- 不因传统线程池规则而替换项目已采纳的 Java 21 virtual thread 模型。
- MySQL 专用规则不直接施加于 SQLite/JPA；不引入 MySQL 或 Flyway 来满足检查。
- 注释保持项目中文 Javadoc 与职责/边界要求，不机械添加作者、日期或无意义注释。
- CLI stdout 是产品输出通道；日志禁用 `System.out` 的规则不能误伤命令输出。
- 每项排除有明确范围和原因；不通过全局忽略来掩盖新增违规。

P3C 接入先检查发布物及其依赖，使用 `jar tf` 查看真实 ruleset 资源路径，
再验证 PMD 扩展 API 和 Java 21 源码解析。不能直接复制未核实的 `ali-pmd.xml` 等路径，
也不能让新的 PMD 引擎无条件加载基于旧引擎的规则 JAR。

若没有可验证的兼容组合，明确报告“阿里 P3C 自动检查未完成”。可以提出兼容 PMD 规则映射
与手册审查的替代方案，列出覆盖和缺口；用户接受替代前，不把它标为已完成 P3C 接入。
继续推进其他独立且已授权的基础设施，不为 P3C 降级项目 Java 版本。

已有代码先运行 `spotless:check` 查看差异。完整仓库格式迁移作为单独、可审查的变更，
与业务修改分开；用户已有未提交内容不因格式迁移被覆盖或自动暂存。
仅在明确的目标范围执行 `spotless:apply`，CI 与 hook 使用 `check`。

## 安全检查

| 检查 | 目标与验收 |
| --- | --- |
| SpotBugs + Find Security Bugs | 编译后分析字节码；确认扩展确实加载且项目 class 已被扫描，覆盖范围受规则与分析能力限制 |
| PMD | 源码缺陷与编码规则；与 P3C 共用时避免重复或互斥的执行配置 |
| OWASP Dependency-Check | 第三方依赖已知漏洞；明确包含范围、失败阈值、漏洞库更新时间及报告位置 |
| 敏感信息检查 | 检查本次源码与配置变更；使用所选工具的脱敏输出，不以 grep 原样打印疑似密钥 |

构建中显式绑定检查 goal 和生命周期。只生成 HTML 报告不保证失败，
必须确认真实违规会导致非零退出码；不能仅在 pluginManagement 中声明。
多模块扫描明确逐模块与聚合职责，避免漏掉子模块或重复运行昂贵检查。

Dependency-Check 需要可用且足够新的漏洞数据。API key 用 CI secret/环境变量注入，
不放入 POM、命令回显或仓库。网络失败、限流、数据库缺失或过期都应报告为扫描未完成，
不使用 `failOnError=false` 伪造成功。
需要拆分较慢的联网扫描时，明确其 CI job、触发条件和必需状态；快速构建通过不能代表全套检查通过。
漏洞抑制项要有证据、适用版本范围、负责人或复核条件，不能宽泛忽略全部 CVE。

初始化中的检查聚焦当前代码和依赖。全历史密钥审计、渗透测试、真实外部探测或账号密钥轮换
属于单独任务，不由普通初始化自动触发。自动扫描通过也不证明应用不存在漏洞。

## 验证失败路径

在临时 fixture 或隔离副本中制造格式违规与所选静态规则违规，确认对应构建退出失败，
且正常 fixture 可通过。不要为演示扫描往当前仓库引入已知漏洞依赖。
依赖漏洞阈值可以用工具官方测试样例或固定扫描报告验证策略，
但这种策略测试不替代针对当前项目的真实依赖扫描。

交付时逐项列出工具/版本、扫描范围、运行命令、退出结果、报告位置及未覆盖内容。
保持“配置已加入”“本地已执行”“CI 已运行”“远端合并规则生效”四种状态独立。

## 实施时核对的官方来源

- [Spotless Maven](https://github.com/diffplug/spotless/tree/main/plugin-maven)
- [google-java-format](https://github.com/google/google-java-format)
- [Google Java Style](https://google.github.io/styleguide/javaguide.html)
- [阿里 Java 开发手册与 P3C](https://github.com/alibaba/p3c)
- [Maven PMD Plugin](https://maven.apache.org/plugins/maven-pmd-plugin/)
- [Checkstyle](https://checkstyle.org/)
- [SpotBugs Maven Plugin](https://github.com/spotbugs/spotbugs-maven-plugin)
- [Find Security Bugs](https://find-sec-bugs.github.io/)
- [OWASP Dependency-Check](https://dependency-check.github.io/DependencyCheck/)

版本、参数和兼容性以实施时选定版本的官方文档和本地验证为准；本 Skill 不自动追逐最新版。
