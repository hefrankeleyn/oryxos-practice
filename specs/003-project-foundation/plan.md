# Implementation Plan: 项目基础设施初始化

**Branch**: `main` | **Date**: 2026-11-04 | **Spec**: [spec.md](spec.md)

## Summary
复用既有Runtime与九模块，补齐Web公共契约、日志/管理端点与构建质量检查。先完成独立验证，再记录实际结果。

## Technical Context
- JDK21、Spring Boot3.5.9、Maven3.9.16；保持同步MVC、virtual thread与现有SQLite/JPA。
- Boot内置结构化日志；Actuator/Micrometer；springdoc2.x兼容版本。
- Spotless/Google格式、Checkstyle、PMD7、SpotBugs/FindSecBugs、Dependency-Check；版本见根POM与research。
- 测试采用JUnit5、MockMvc、真实回环HTTP与临时数据库；测试控制器不进入产品JAR。
- 基础设施无新增业务存储表；原Provider/Tool审计保持。

## Constitution Check
- [x] 九模块保持，core不引入Web；boot聚合依赖。
- [x] 自主管理Runtime、Provider映射、工具执行与审计不变。
- [x] SQLite显式DDL、密钥外部注入与敏感数据边界不变。
- [x] 当前用户要求在main工作；不自动创建分支，不修改constitution。
- [x] 文档与管理入口先在隔离环境验证，默认不开Swagger。

## Project Structure
- `oryxos-web/src/main/java/com/oryxlabs/oryxos/web/`: ApiResponse、错误类型、异常处理、消息字节校验和请求日志过滤器。
- `oryxos-boot/src/main/resources/`: 默认配置与local-docs/json-logs配置。
- `oryxos-web/src/test/`、`oryxos-boot/src/test/`: 公共契约、日志、管理端点及运行回归。
- `pom.xml`、`config/quality/`: 统一版本、生命周期与检查规则。
- `scripts/quality/`、`.githooks/`、`.github/workflows/`: 本地/CI执行入口；不改变网站发布。
- `.mvn/wrapper/`、`mvnw*`: 固定构建工具链。

## Execution
先验证基线；新增依赖与质量插件后进行单独格式迁移；实现可观测与Web基础契约；运行完整检查和隔离负向验证；更新状态及quickstart。
昂贵的依赖漏洞扫描使用独立Maven profile和必需CI job，默认verify的结果不得冒充联网扫描结果。
用户已于2026-11-04确认P3C的兼容规则映射分支；以PMD 7/Checkstyle覆盖选定条款，记录缺口与人工清单，不加载旧版P3C。NVD扫描按用户选择保留待配置，不降低检查失败门槛。
