# Claude Code 项目入口

开始任何任务前完整阅读根目录 `AGENTS.md`，并遵循其中的协作、所有权、交接和验证规则。

使用 Agent Teams 或 Subagents 时：

- Lead 只负责任务编排、消息转发和汇总，不实现业务代码。
- Architect 先完成必要设计和契约冻结，再允许前后端并行。
- 前端和后端分别使用独立 Worktree。
- QA 在集成结果上验证，缺陷退回原实现 Agent。

详细流程见 `docs/agent/multi-agent-workflow.md`。

