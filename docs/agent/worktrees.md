# 前后端 Worktree 使用说明

## 固定工作区

| 用途 | 分支 | 目录 |
|---|---|---|
| 主控、架构、集成、QA | `main` 或任务集成分支 | `ai-content-marketing-system` |
| 后端实现 | `agent/backend` | `worktrees/backend` |
| 前端实现 | `agent/frontend` | `worktrees/frontend` |

实际绝对路径以 `git worktree list` 输出为准。

## 使用规则

- 前后端 Agent 只能在自己的 Worktree 修改代码。
- 每次开始新任务前先确认 Worktree 没有未提交修改。
- 架构契约提交后，前后端分支再同步该提交并开始实现。
- 每个 Agent 完成后提交自己的修改，再通知 Team Lead 和架构 Agent。
- 主工作区负责审查和集成，不在前后端 Worktree 直接修复另一端代码。
- 不执行会丢弃未提交内容的重置或清理命令。

## 常用检查

```bash
git worktree list
git -C ../worktrees/backend status --short
git -C ../worktrees/frontend status --short
```

当角色分支长期复用时，每个任务完成并集成后，应先确认其与主分支没有遗留差异，再开始下一项任务。

