# 多 Agent 文件所有权

所有权按任务动态确定。下表是默认边界，不是永久组织结构。

| 范围 | 默认负责人 | 说明 |
|---|---|---|
| `docs/agent/**`、`docs/contracts/**` | 架构 Agent | 协作规则与跨端契约 |
| `backend/**` | 后端 Agent | 后端实现与测试 |
| `frontend/**` | 前端 Agent | 前端实现与测试 |
| 独立 E2E、验证报告 | QA Agent | 不修改业务实现 |
| `README.md`、产品进展文档 | 架构 Agent或任务指定者 | 避免多人同时更新 |

## 共享热点

以下文件容易被多个任务同时影响，开始任务时必须指定唯一负责人：

- `docker-compose.yml`
- `backend/src/main/resources/schema.sql`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-prod.yml`
- `PublisherRegistry`
- 发布任务状态与公共结果类型
- 前端路由、菜单和公共类型
- 根目录构建、部署和环境配置

## 平台发布模块

微信、掘金、CSDN、知乎可以按平台分配给不同 Agent，但公共发布框架和注册入口只分配给一个负责人。真实 Cookie、Token、浏览器登录态和 API Key 不进入 Git，也不发送给其他 Agent。

## 冲突处理

- 未开始修改前发现所有权重叠：由 Team Lead 重新拆分。
- 已经出现实现重叠：保留两方结果，由架构 Agent 指定一个负责人整合。
- Git 文本冲突：退回产生冲突的开发 Agent 处理，Lead 和 QA 不直接改业务代码。
- 契约冲突：暂停受影响实现，由架构 Agent 先更新契约。

