# ContentPilot AI 多 Agent 开发规则

本文件是所有 AI 编程工具共用的项目级规则。开始任务前，先阅读：

1. `README.md`
2. `docs/project-progress-summary.md`
3. 与任务直接相关的产品或技术文档
4. `docs/agent/multi-agent-workflow.md`
5. `docs/agent/ownership.md`

## 协作模型

- Team Lead 是用户的唯一任务入口，负责澄清、拆分、分配、跟踪和汇总，不写业务代码。
- 架构 Agent 负责技术设计、契约冻结、文件所有权和集成审查；契约冻结后不直接修改前后端业务实现。
- 后端 Agent 只在后端 Worktree 工作，负责后端实现、测试和自验。
- 前端 Agent 只在前端 Worktree 工作，负责前端实现、测试和自验。
- QA Agent 在集成结果上验证，发现问题后退回原负责人，不直接修补业务代码。

角色按任务需要启用，不要求每个任务都同时启动全部 Agent。

## 简化流程

1. Team Lead 明确任务、验收目标和不做事项。
2. 架构 Agent 给出必要设计并冻结前后端契约。
3. 前端、后端从同一契约版本并行开发。
4. 开发 Agent 自验后提交结果，架构 Agent 审查并集成。
5. QA Agent 对集成结果做完整验证。
6. Team Lead 汇总完成内容、验证结果和剩余风险，交付用户。

状态名称不固定，只要能够清楚表达当前处于规划、实现、验证、修复或完成中的哪一步。

## 执行约束

- 一个文件同一时间只能有一个可写负责人。
- 不得为了实现方便静默修改已冻结的 API、字段、枚举或业务流程。
- 契约需要调整时，先说明原因和影响，由架构 Agent 更新后再继续。
- 前后端 Agent 必须先完成本模块验证，不能把基础构建错误留给 QA。
- QA 只报告、补充独立测试和回归验证；业务缺陷退回原开发 Agent。
- Team Lead 不替代开发 Agent 修复代码，也不替代 QA 宣布验证通过。
- 不提交密码、Cookie、Token、API Key、浏览器用户目录或真实账号数据。
- 微信、掘金、CSDN、知乎真实发布属于人工批准环节；自动验证默认使用 Mock。

## 最小交接信息

不强制固定表格，但每次交接至少说清楚：

- 任务和当前状态
- 实现了什么
- 修改了哪些文件或模块
- 做了什么验证以及结果
- 未完成事项、阻塞或重要风险（没有可省略）
- 契约是否发生偏差（没有可省略）
- 对应分支或提交

## 验证入口

```bash
./scripts/verify-backend.sh
./scripts/verify-frontend.sh
./scripts/verify-changed.sh
./scripts/verify-all.sh
```

测试必须以完整命令的退出码为准，不得通过 `grep`、`head` 或截断输出掩盖失败。

