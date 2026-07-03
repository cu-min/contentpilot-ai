# 发布任务草稿流转与执行框架设计

## 阶段目标

阶段 7 完成平台账号配置、发布任务管理和发布任务草稿流转，为后续自动发布做准备。阶段 8 在此基础上启用最小版发布执行框架，通过 `MockPublisher` 验证系统内部执行链路。

当前阶段仍不执行真实自动发布，不调用微信公众号、知乎、CSDN、掘金的真实发布接口，不接入 Playwright / Selenium，不生成追踪链接，也不自动读取真实发布链接。

## 平台发布策略引用

发布策略参考 `docs/platform-publishing-strategy-reference.md`：

| 平台 | 后续推荐发布方式 | 阶段 7 设计含义 |
|---|---|---|
| 微信公众号 | 官方 API 优先，先做草稿 | 平台账号支持 `APP_SECRET`，发布方式支持 `OFFICIAL_API` |
| 知乎 | 浏览器自动化 + 人工确认 | 平台账号支持 `COOKIE` / `BROWSER_PROFILE`，发布方式支持 `BROWSER_AUTOMATION` / `MANUAL_CONFIRM` |
| CSDN | 浏览器自动化，草稿或自动填充 | 平台账号支持浏览器登录态，发布任务保留草稿流转 |
| 掘金 | 非官方草稿接口试点 + 浏览器兜底 | 发布方式支持 `UNOFFICIAL_API` 和 `BROWSER_AUTOMATION` |

后续真实平台发布阶段将基于 `platform_account`、`article_platform_content`、`publish_task` 实现具体平台 Publisher。

## 平台账号配置设计

表：`platform_account`

核心字段：

- `platform`：平台，支持 `WECHAT_OFFICIAL`、`ZHIHU`、`CSDN`、`JUEJIN`
- `account_name`：公司统一平台账号名称
- `auth_type`：认证方式，支持 `APP_SECRET`、`COOKIE`、`BROWSER_PROFILE`、`API_KEY`、`MANUAL`
- `auth_config`：认证配置，JSON 字符串或本地配置描述
- `default_publish_mode`：默认发布方式，支持 `OFFICIAL_API`、`UNOFFICIAL_API`、`BROWSER_AUTOMATION`、`MANUAL_CONFIRM`
- `enabled`：是否启用
- `remark`：备注

安全要求：

- `auth_config` 属于敏感信息。
- 后端列表和详情接口不返回明文 `auth_config`，只返回是否已配置和脱敏标记。
- 前端列表不展示认证配置明文。
- 编辑时认证配置留空表示保留旧值。
- 不在代码、日志或 Git 中提交真实 Cookie、Token、API Key。

## 发布任务状态设计

表：`publish_task`

当前实际使用状态：

| 状态 | 说明 |
|---|---|
| `DRAFT` | 草稿，可编辑 |
| `PENDING` | 已提交，等待手动执行 |
| `RUNNING` | 执行中，由发布执行框架写入 |
| `SUCCESS` | 执行成功，记录 `publish_url` |
| `FAILED` | 执行失败，记录 `error_message` |
| `CANCELLED` | 已取消 |

后续预留状态：

```text
NEED_LOGIN
NEED_CAPTCHA
NEED_MANUAL_CONFIRM
LINK_FETCH_FAILED
CONTENT_REJECTED
```

发布类型：

| 类型 | 说明 |
|---|---|
| `IMMEDIATE` | 立即发布计划，阶段 7 只表示提交后可进入待发布 |
| `SCHEDULED` | 定时发布计划，必须填写 `schedule_time` |

## 发布任务创建流程

```text
选择文章
↓
选择该文章下的平台发布稿
↓
选择同平台的平台账号
↓
选择立即发布或定时发布
↓
保存为 DRAFT
↓
提交为 PENDING 或取消为 CANCELLED
```

校验规则：

- 必须选择 `platform_content_id`。
- 必须选择 `account_id`。
- 平台稿平台必须和平台账号平台一致。
- `IMMEDIATE` 可以不填 `schedule_time`。
- `SCHEDULED` 必须填写 `schedule_time`。
- 创建后默认 `DRAFT`。
- 只有 `DRAFT` 可以编辑和提交为 `PENDING`。
- 只有 `PENDING` 可以手动执行。
- `DRAFT` / `PENDING` 可以取消为 `CANCELLED`。
- `RUNNING` 后只允许进入 `SUCCESS` 或 `FAILED`。

## 发布执行接口

```text
POST /api/publish/tasks/{id}/execute
```

执行逻辑：

```text
查询发布任务
↓
校验任务状态必须为 PENDING
↓
查询平台发布稿和平台账号
↓
校验任务、平台稿、平台账号的平台一致
↓
任务状态更新为 RUNNING
↓
调用 PublisherRegistry 获取发布器
↓
当前阶段调用 MockPublisher
↓
成功：状态更新为 SUCCESS，并写入 mock publish_url
失败：状态更新为 FAILED，并写入 error_message
```

状态约束：

- `DRAFT` 不能直接执行。
- `CANCELLED` 不能执行。
- `SUCCESS` 不能重复执行。
- `FAILED` 暂不做重试。
- 定时发布任务本阶段不自动扫描，到达 `schedule_time` 后也只允许人工点击执行。

## MockPublisher 说明

`MockPublisher` 只用于阶段 8 验证发布任务执行链路：

- 不调用任何真实平台接口。
- 不读取真实 Cookie、Token、API Key。
- 默认模拟发布成功。
- 成功时返回形如 `https://mock.publish/{platform}/{taskId}` 的模拟链接。
- 当任务标题、摘要、正文、标签、关键词或账号备注中包含 `mock-fail` 时，模拟发布失败，方便验证失败状态和错误提示。

## 本阶段边界

本阶段不做：

- 真实自动发布
- 真实平台账号验证
- 微信公众号 API 调用
- 知乎 / CSDN / 掘金真实发布
- Playwright / Selenium
- 追踪链接生成
- 自动读取发布链接
- 定时任务自动扫描
- 失败自动重试
- 操作日志
- Redis
- SaaS 多租户

## 后续扩展点

发布执行框架采用统一 Publisher 抽象：

```java
public interface PlatformPublisher {
    String platform();
    String mode();
    PublishResult publish(PublishContext context);
}
```

`PublishContext` 可从以下表组装：

- `platform_account`：认证方式、认证配置、默认发布方式
- `article_platform_content`：平台标题、正文、标签、关键词
- `publish_task`：计划类型、计划时间、任务状态、发布方式

不同平台的 Publisher 可以独立演进，避免用一套发布逻辑硬套所有平台。

## 试点路线补充

阶段 8.5 只做掘金 / CSDN 发布方案调研与试点计划，不实现真实发布代码。

阶段 9 新增 `JuejinPublisher`，实际采用“已有草稿更新 + 可选正式发布”：

- 使用 `platform_account.auth_config` 保存 Cookie、userAgent、csrfToken、draftId、默认分类 ID、默认标签 ID 等本地配置。
- 使用 `article_platform_content` 提供标题、摘要、Markdown 正文和标签。
- 调用掘金 `article_draft/update` 更新已有草稿。
- `draftOnly=true` 时只更新草稿，任务成功后 `publish_url` 写入 `https://juejin.cn/editor/drafts/{draftId}`。
- `draftOnly=false` 时 update 成功后再调用 `article/publish`，阶段 9 暂按 HTTP 2xx 且无异常认为成功。
- 使用 `publish_task.publish_url` 暂存草稿编辑链接。
- 使用 `publish_task.error_message` 记录失败原因。
- 保留 `MockPublisher`，不删除。
- 不自动创建新草稿，不做 CSDN、追踪链接、数据看板、定时调度或自动重试。

阶段 10 如启动 CSDN，建议作为可选阶段新增 `CsdnPublisher`，通过 Playwright 打开 CSDN 编辑器并自动填充内容，停在人工确认页面，不强制点击发布。
