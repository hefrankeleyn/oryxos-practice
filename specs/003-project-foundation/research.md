# Research: 基础设施工具链

## 兼容工具链
Decision：复用实际Maven缓存中可由Java21加载的版本：Spotless3.10.4/Google1.36.1、Checkstyle3.6.0/14.3.0、PMD插件3.28.0/引擎7.27.0、SpotBugs插件4.10.5.0/引擎4.10.5、FindSecBugs1.14.0、Dependency-Check13.0.0；Maven Wrapper锁定3.9.16。
Rationale：检查过实际JAR和字节码版本；插件及分析引擎分开固定，仍需在本项目运行正负向fixture。
Alternatives：默认插件引擎过旧；未选择未经核实的任意最新版。

## 日志与接口文档
Decision：Boot3.5内置JSON日志，springdoc2.8.17；保持Boot3.5.9，不升级业务框架。
Rationale：与现有JDK21/MVC兼容，springdoc2.8.17已缓存；JSON无需额外encoder。
Alternatives：不引入响应式、独立日志平台或产品管理页面。
Sources：[Boot日志](https://docs.spring.io/spring-boot/3.5/reference/features/logging.html)、[springdoc兼容表](https://springdoc.org/v2/faq.html)。

## P3C兼容性
官方P3C2.1.1依赖PMD6.15.0；Java21源码支持从PMD7开始；旧扩展存在BaseLanguageModule缺类。
[官方POM](https://github.com/alibaba/p3c/blob/master/p3c-pmd/pom.xml)、[上游问题](https://github.com/alibaba/p3c/issues/1002)、[语言支持](https://docs.pmd-code.org/latest/pmd_languages_java.html)。
用户于2026-11-04确认采用PMD 7 + Checkstyle兼容规则映射，覆盖与缺口在[alibaba-mapping.md](../../config/quality/alibaba-mapping.md)逐项记录，未覆盖条款保留人工清单。官方P3C不被宣称已接入。

## 检查执行
格式/Checkstyle在validate，PMD/SpotBugs在verify；高成本依赖扫描使用dependency-audit profile的aggregate命令及独立CI job。
Dependency-Check阈值为CVSS>=7，扫描错误必须失败；API key来自NVD_API_KEY环境变量。实际扫描因空凭证和无有效漏洞数据失败；用户选择先完成其他验证，将漏洞扫描保留待配置，门槛保持启用。
[PMD版本覆盖](https://maven.apache.org/plugins/maven-pmd-plugin/examples/upgrading-PMD-at-runtime.html)、[Checkstyle check](https://maven.apache.org/plugins/maven-checkstyle-plugin/check-mojo.html)、[SpotBugs](https://spotbugs.github.io/spotbugs-maven-plugin/examples/violationChecking.html)、[Dependency-Check配置](https://dependency-check.github.io/DependencyCheck/dependency-check-maven/configuration.html)。
