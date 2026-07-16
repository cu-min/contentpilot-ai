# 人工发布交接契约

> 版本：2026-07-16。本契约冻结发布任务的后端状态、API 和前端交互，修改前必须由架构 Agent 评估影响。

## 业务边界

系统只做发布前准备，最终发布必须由用户在平台页面人工确认：

- 微信公众号：调用官方 API 创建草稿，不提交正式发布。
- 掘金：创建或更新草稿，不调用文章发布接口。
- CSDN：打开编辑器、填充标题正文和发布弹窗字段，停在最终确认按钮前。
- 知乎：打开编辑器、填充标题正文、话题和专栏等设置，停在最终发布按钮前。

新流程不自动进入 `SUCCESS`，不写入 `PUBLISHED`，不伪造正式文章链接。

## 任务状态

```text
DRAFT --submit--> PENDING --prepare--> RUNNING
RUNNING --> WAITING_MANUAL_CONFIRM
RUNNING --> FAILED
DRAFT/PENDING --> CANCELLED
```

- `RUNNING` 只表示程序正在准备草稿或编辑器。
- `WAITING_MANUAL_CONFIRM` 表示系统的工作已完成，正等待用户在外部平台检查并点击。
- `SUCCESS` 仅用于展示历史数据，新流程不再写入。
- 登录失效、验证码、编辑器填充失败或外部接口失败写入 `FAILED`，并保留可读 `errorMessage`。

## API

```http
POST /api/publish/tasks/{id}/prepare
```

响应保持现有结构：

```text
Result<PublishTaskVO>
```

`POST /api/publish/tasks/{id}/execute` 暂时作为兼容别名，内部委托同一准备服务。新前端只调用 `/prepare`。

## 结果字段

准备成功时：

- `status = WAITING_MANUAL_CONFIRM`
- `externalDraftId` 保存微信或掘金草稿 ID，浏览器平台可为空。
- `draftUrl` 保存草稿或当前编辑器交接地址。
- `publishUrl` 保持为空。
- `articleStatus` 保持为空。
- `errorMessage` 保持为空。

服务层必须先保存发布适配器返回的草稿 ID 和 URL，再根据结果状态完成任务流转。

## 前端交互

- `PENDING` 显示“准备发布”，定时任务未到时禁用。
- `RUNNING` 显示“准备中”。
- `WAITING_MANUAL_CONFIRM` 显示“等待人工发布”。
- 微信、掘金提供“打开草稿”；CSDN、知乎提供“打开编辑器”。
- 不显示“自动发布成功”或“已发布”等系统无法证明的结论。
- 本版本不提供“标记已发布”接口。

## 生产安全

- Publisher Registry 只允许精确平台和方式匹配；缺少实现时直接失败。
- 生产代码不得包含会返回虚假成功的 Mock Publisher。
- noVNC 只绑定宿主机 `127.0.0.1:6080`，通过 SSH 隧道访问。
- `VNC_PASSWORD` 必须非空；空密码时容器必须拒绝启动，不得使用 `-nopw`。
- 测试使用 Mockito 或 `src/test` 中的替身，不依赖生产 Mock 回退。
