# 发布任务草稿流转设计

## 阶段目标

阶段 7 只完成平台账号配置、发布任务管理和发布任务草稿流转，为后续自动发布做准备。本阶段不执行真实自动发布，不调用微信公众号、知乎、CSDN、掘金的真实发布接口，不接入 Playwright / Selenium，不生成追踪链接，也不自动读取发布链接。

## 平台发布策略引用

发布策略参考 `docs/platform-publishing-strategy-reference.md`：

| 平台 | 后续推荐发布方式 | 阶段 7 设计含义 |
|---|---|---|
| 微信公众号 | 官方 API 优先，先做草稿 | 平台账号支持 `APP_SECRET`，发布方式支持 `OFFICIAL_API` |
| 知乎 | 浏览器自动化 + 人工确认 | 平台账号支持 `COOKIE` / `BROWSER_PROFILE`，发布方式支持 `BROWSER_AUTOMATION` / `MANUAL_CONFIRM` |
| CSDN | 浏览器自动化，草稿或自动填充 | 平台账号支持浏览器登录态，发布任务保留草稿流转 |
| 掘金 | 非官方草稿接口试点 + 浏览器兜底 | 发布方式支持 `UNOFFICIAL_API` 和 `BROWSER_AUTOMATION` |

后续自动发布阶段将基于 `platform_account`、`article_platform_content`、`publish_task` 实现平台 Publisher。

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

阶段 7 实际使用状态：

| 状态 | 说明 |
|---|---|
| `DRAFT` | 草稿，可编辑 |
| `PENDING` | 已提交，等待后续自动发布阶段处理 |
| `CANCELLED` | 已取消 |

后续预留状态：

```text
RUNNING
SUCCESS
FAILED
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
- 只有 `DRAFT` 可以编辑和提交。
- 取消操作只改变状态，不执行发布。

## 本阶段边界

本阶段不做：

- 真实自动发布
- 真实平台账号验证
- 微信公众号 API 调用
- 知乎 / CSDN / 掘金真实发布
- Playwright / Selenium
- 追踪链接生成
- 自动读取发布链接
- 操作日志
- Redis
- SaaS 多租户

## 后续扩展点

后续自动发布阶段建议新增统一 Publisher 抽象：

```java
public interface PlatformPublisher {
    String platform();
    String publishMode();
    PublishResult publish(PublishContext context);
}
```

`PublishContext` 可从以下表组装：

- `platform_account`：认证方式、认证配置、默认发布方式
- `article_platform_content`：平台标题、正文、标签、关键词
- `publish_task`：计划类型、计划时间、任务状态、发布方式

不同平台的 Publisher 可以独立演进，避免用一套发布逻辑硬套所有平台。
