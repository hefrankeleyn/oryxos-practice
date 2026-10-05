# CLAUDE.md

本文件是 Claude Code 在本仓库中的入口说明。跨 Agent 共用的项目规则只维护在根目录 `AGENTS.md`，不要在这里复制一份。

## 开始任务

1. 先完整读取根目录 `AGENTS.md` 并遵守其项目边界、架构原则、验证和 Git 规则。
2. 查看 `docs/IMPLEMENTATION_STATUS.md`，确认相关能力是已实现、部分实现还是目标设计。
3. 只按任务需要读取 `docs/02-DemandAnalysis.md`、`docs/03-TechnicalSolution.md`、`docs/04-AiProgrammingGuide.md` 或网站文档；小改动不需要重读全部六份资料。

## Claude Code 补充约定

- `docs/prompt/` 是用户与 Claude 的执行记录，不是需求来源；除非用户明确要求，否则不要修改。
- 主体能力开发遵循 `docs/04-AiProgrammingGuide.md` 的 User Story 顺序。当前 `.specify/` artifacts 是否已经建立，以 `docs/IMPLEMENTATION_STATUS.md` 和实际目录为准，不得假定存在。
- 使用 Spec-Kit 时，constitution 一经用户确认不得自行改写；每个 User Story 完成后进行一致性检查，并用测试和 Demo 证明完成。
- 不要因为 README、官网、POM 描述或 `package-info.java` 写有某个类名，就生成依赖于尚不存在实现的代码。
- 能力状态变化时同步更新 `docs/IMPLEMENTATION_STATUS.md`；仅修改注释、文案或配置时，不要虚增实现进度。

构建、运行命令及详细工程规范统一以 `AGENTS.md` 为准。
