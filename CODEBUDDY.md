# CodeBuddy 项目入口

开始任何任务前完整阅读根目录 `AGENTS.md`，并遵循其中的协作、所有权、交接和验证规则。

使用 Agent Team 时：

- Team Lead 使用主工作区，只做调度、状态跟踪和结果汇总。
- 架构 Agent 使用主工作区维护设计与契约，并负责审查。
- backend-agent 使用后端 Worktree。
- frontend-agent 使用前端 Worktree。
- qa-agent 在前后端结果集成后启动，默认只验证和报告问题。
- 前后端开始前必须由架构 Agent 明确契约和文件范围。

详细流程见 `docs/agent/multi-agent-workflow.md`。

