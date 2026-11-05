# Tasks: 项目基础设施初始化

## Phase 1: Setup
- [x] T001 保存原有工作并确认主分支，检查 `AGENTS.md` 和 `.specify/memory/constitution.md`。
- [x] T002 建立 `specs/003-project-foundation/` 规格、设计、契约与任务；保留002验收状态。

## Phase 2: Foundation
- [x] T003 在 `pom.xml` 固定依赖与质量插件版本，配置 `config/quality/` 与 `.editorconfig`。
- [x] T004 生成 `.mvn/wrapper/maven-wrapper.properties`、`mvnw`、`mvnw.cmd` 并验证工具链。
- [x] T005 对九模块 `src/main/java`、`src/test/java` 执行可审查的Google格式迁移，确认幂等。

## Phase 3: US1 可诊断运行
- [x] T006 [US1] 在 `oryxos-boot/src/main/resources/` 接入受限管理入口和JSON日志配置。
- [x] T007 [US1] 在 `oryxos-web/src/main/java/com/oryxlabs/oryxos/web/` 实现请求MDC关联、清理和安全访问日志。
- [x] T008 [US1] 在 `oryxos-boot/src/test/` 验证管理端点、JSON日志、CLI及审计回归。

## Phase 4: US2 API基础契约
- [x] T009 [US2] 在 `oryxos-web/src/main/java/com/oryxlabs/oryxos/web/` 实现统一响应与异常映射。
- [x] T010 [US2] 在 `oryxos-web/src/main/java/com/oryxlabs/oryxos/web/` 实现32KiB UTF-8消息校验；配置本地OpenAPI。
- [x] T011 [US2] 在 `oryxos-web/src/test/` 添加测试专用Controller，验证响应、敏感值和多字节边界。

## Phase 5: US3 质量检查
- [x] T012 [US3] 执行 `pom.xml` 的格式、Checkstyle、PMD、SpotBugs/FindSecBugs，修复或精确解释发现。
- [x] T013 [US3] 按用户选择记录 `config/quality/` 的阿里规则映射与覆盖缺口；不兼容P3C不报通过。
- [x] T014 [US3] 在 `scripts/quality/` 和 `.githooks/pre-commit` 实现只读暂存检查与脱敏密钥扫描。
- [x] T015 [US3] 新增 `.github/workflows/java-ci.yml`，保留网站发布工作流；激活本地hook时保护已有hook。
- [ ] T016 [US3] 执行 `pom.xml` dependency-audit及隔离负向fixture，记录漏洞库/网络限制。

## Phase 6: Integration
- [x] T017 完成构建、真实回环JAR验证和文档入口验证，记录到 `specs/003-project-foundation/quickstart.md`。
- [x] T018 更新 `docs/IMPLEMENTATION_STATUS.md` 与使用指引，核对代码差异和实际覆盖。

## Dependencies & Strategy
T001→T002→T003/T004→T005；US1与US2共享Web基础代码，顺序实施；US3配置可独立推进，T013所需用户选择已获得。
独立检查可并行执行，修改同一POM或源码时顺序处理。每个故事先完成其测试，再做全量构建与可丢弃JAR验收。

## 执行结果（2026-11-04）
T013 已按用户确认的 PMD 7 + Checkstyle 兼容映射完成：覆盖说明、缺口及人工清单见 `config/quality/alibaba-mapping.md`；九模块检查与隔离正负向样例通过，不代表完整 P3C 合规。T016 的格式、PMD、FindSecBugs 与 Gitleaks 负向样例已完成，真实依赖扫描因缺少 NVD_API_KEY 和可用漏洞数据失败，按用户选择保留待配置、保持未勾选。其余任务证据见 quickstart.md。

2026-11-05 用户确认将本轮初始化提交到 main，NVD_API_KEY 暂不配置，T016 继续保留待办；未推送或部署。
