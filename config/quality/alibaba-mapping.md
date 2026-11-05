# 阿里编码规约：兼容规则映射与人工清单

2026-11-04 用户确认采用 **PMD 7 + Checkstyle 的兼容规则映射**，其余条款保留人工清单。
本项目没有加载官方 P3C 插件，也不宣称完整覆盖 P3C 或阿里 Java 开发手册。
格式统一采用 Google Java Style 的 2 空格缩进；与阿里格式条款冲突时不重复启用。

实际配置是 [checkstyle.xml](checkstyle.xml) 与 [pmd.xml](pmd.xml)，版本由根 POM 固定为
Checkstyle 14.3.0、PMD 7.27.0。这里的映射表示检查意图有交集，不表示替代工具与 P3C
具有完全相同的 AST、数据流分析能力、例外或违规判定。

## 自动覆盖及边界

| 规约关注点 | 实际启用规则 | 覆盖边界与缺口 |
| --- | --- | --- |
| 类、方法及包名风格 | Checkstyle `TypeName`、`MethodName`、`PackageName` | 类名首字母大写，方法名首字母小写，名称不含下划线或美元符号；包名各段以小写字母开头，仅含小写字母和数字。无法判断英文语义、缩写是否合理；字段、参数、常量、异常/测试类后缀等未自动覆盖。 |
| 捕获异常后必须处理 | PMD `EmptyCatchBlock` | 注释不能代替处理；`ignored`/`expected` 变量名也不能绕过。不能判断非空 catch 的恢复逻辑、审计和错误映射是否正确。 |
| 释放自身拥有的资源 | PMD `CloseResource` | 检查可解析的 AutoCloseable 等资源类型及其控制流；跨方法所有权、容器托管资源仍需审查。已有两处精确例外见下文。 |
| equals 与 hashCode 契约 | PMD `OverrideBothEqualsAndHashcode` | 检查是否配套重写，不证明实现语义一致；该规则不处理实现 Comparable 的类型，其 equals/hashCode/compareTo 契约由人工检查。 |
| 空值判断 | PMD `EqualsNull`、`BrokenNullCheck` | 分别检查 `equals(null)` 和特定错误短路条件。**不等同于** P3C 的 `EqualsAvoidNullRule`，不能证明所有 nullable 接收者、自动拆箱均安全。 |
| 字符串值比较 | PMD `UseEqualsToCompareStrings` | 检查 String 两侧的 `==`/`!=`；不是对全部包装类型的 `WrapperTypeEqualityRule` 实现。包装类型值比较仍需人工检查。 |
| 正确启动线程 | PMD `DontCallThreadRun` | 拦截直接调用 Thread.run；不禁止已采纳的虚拟线程，也不证明线程生命周期和取消机制正确。 |
| 双重检查锁的安全性 | PMD `DoubleCheckedLocking` | 检查可识别的 DCL 风险；其他共享状态、可见性和锁边界仍需人工检查。 |
| 使用日志框架记录异常 | PMD `AvoidPrintStackTrace` | 当前规则匹配无参 printStackTrace；带输出流的重载、System.out 日志及敏感信息仍需审查。CLI 正常结果的 stdout 是保留的产品契约。 |
| 避免无用局部变量 | PMD `UnusedLocalVariable` | 作为补充缺陷检查，不计为完整阿里规则；PMD 默认忽略 ignored/unused 前缀，不证明变量使用符合业务语义。 |

Checkstyle 的 `FileTabCharacter`、`NewlineAtEndOfFile`、`OuterTypeFilename` 是工程基础检查，
不额外算作阿里语义规则覆盖。当前没有声称自动覆盖完整集合、事务、SQL 或注释规约。

## 执行与例外

- Checkstyle 在 Maven `validate` 检查九模块生产及测试源码；PMD 在 `verify` 检查生产源码。
  两者都以 0 未处理违规为门槛；CI 的 `quality` 作业运行同一套配置。
- 本地提交 hook 只检查暂存快照的格式与密钥；不把 hook 通过当作完整规约检查通过。
- PMD `CloseResource` 仅在 `OryxOsCommand.call` 和 `BootRuntimeStarter.start` 按方法排除：
  前者借用 Picocli 输出流，后者将 Spring 上下文所有权移交 RuntimeHandle，失败时立即关闭。
  增加资源或改变所有权时必须重新审查，不扩大为全局排除。
- 不通过重命名异常为 ignored、添加空注释或全局忽略来消除违规。新增必要例外应注明
  具体方法、原因、关闭/处理责任和复核条件，并在代码审查时确认。
- [spotbugs-exclude.xml](spotbugs-exclude.xml) 中的字节码分析例外有独立理由；
  不把 SpotBugs/FindSecBugs 的检查结果计算为 P3C 已执行。

## 人工审查清单

以下清单供修改相关代码时填写；列出清单不代表已经对全部仓库完成人工手册审计。
审查者应逐项记录“通过 / 不适用 / 待修复”及理由或证据。

- [ ] 命名是否表达准确含义；字段、局部变量、参数、常量命名及必要后缀是否符合项目约定。
  不为命名检查机械改写已采纳的接口、RuntimeFailure 等公共契约。
- [ ] nullable 调用、包装类型比较与自动拆箱是否安全；equals/hashCode/compareTo 是否一致。
- [ ] 集合泛型、遍历期间修改、不可变视图、数组转换及容量选择是否正确。
- [ ] 资源所有权和所有失败路径是否清晰；借用的流不会提前关闭，自建资源不会泄漏。
- [ ] 异常是否有正确的恢复或安全错误映射；失败、拒绝仍落库审计，不记录原始敏感 cause。
- [ ] 共享状态、锁、可见性、任务取消和上下文清理是否正确；继续使用同步模型与虚拟线程，
  不为传统线程池条款改成平台线程池或响应式栈。
- [ ] SQLite/JPA 的事务边界、回滚、约束和显式 DDL 是否正确；不套用 MySQL 专有规则，
  不为规约检查引入 MySQL、Flyway 或破坏独立审计提交。
- [ ] 公共契约、复杂业务和安全逻辑是否有中文说明；不机械补作者/日期或复述代码。
- [ ] 日志是否参数化并去除密钥、Prompt、敏感 Tool 参数与模型原文；CLI 结果和日志通道分离。
- [ ] DTO、金额/时间、序列化及 Lombok 生成行为是否适合业务；不强制生成可能泄漏敏感字段的 toString。

## 来源与复核

命名、对象比较和异常处理的上游范围分别参照
[阿里命名规则](https://github.com/alibaba/p3c/blob/master/p3c-pmd/src/main/resources/rulesets/java/ali-naming.xml)、
[阿里面向对象规则](https://github.com/alibaba/p3c/blob/master/p3c-pmd/src/main/resources/rulesets/java/ali-oop.xml)、
[阿里异常规则](https://github.com/alibaba/p3c/blob/master/p3c-pmd/src/main/resources/rulesets/java/ali-exception.xml)。
其余条款以[阿里 Java 开发手册仓库](https://github.com/alibaba/p3c)为来源；本映射仅选取与项目兼容的检查意图。

具体检测边界已核对本机 PMD 7.27.0 JAR 内的 category/java 规则定义。
变更工具版本、规则配置或项目架构时，重新核对映射、例外及正负向样例，不能沿用旧的通过结论。
执行证据见 [003 验收记录](../../specs/003-project-foundation/quickstart.md)。
