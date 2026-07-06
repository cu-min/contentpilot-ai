# MVP 收尾检查

## 已完成能力

- 产品配置、AI 生成文章、文章库。
- 微信公众号、知乎、CSDN、掘金平台稿生成与管理。
- 平台账号配置、发布任务、MockPublisher 执行链路。
- 掘金已有草稿更新试点。
- 用户已本地验证掘金草稿真实更新成功，并已手动发布成功。

## 当前不做

- CSDN 自动填充、微信公众号真实发布、知乎发布。
- 追踪链接、真实数据看板、ECharts 图表。
- 定时调度、失败重试、自动创建掘金草稿。
- Redis、SaaS 多租户、操作日志。

## 掘金 auth_config 示例

```json
{
  "cookie": "本地填写，不提交 Git",
  "userAgent": "浏览器 User-Agent，可选",
  "csrfToken": "可选，接口失败时补充",
  "aid": "2608",
  "uuid": "",
  "draftId": "从 /editor/drafts/{draftId} 获取",
  "defaultCategoryId": "从 article_draft/update 请求 Payload 的 category_id 获取",
  "defaultTagIds": [],
  "draftOnly": true
}
```

## 本地验证流程

1. 配置 `JUEJIN + COOKIE + UNOFFICIAL_API` 平台账号。
2. `auth_config` 填写合法 JSON，至少包含 `cookie`、`draftId`、`defaultCategoryId`。
3. 创建文章并生成掘金平台稿。
4. 创建发布任务并提交为 `PENDING`。
5. 执行发布任务。
6. `draftOnly=true` 时更新草稿并写入草稿编辑链接。
7. 用户在掘金编辑器中人工确认并发布。

## 常见错误

- JSON 格式错误：检查双引号、逗号和字段格式。
- Cookie 缺失：在认证配置中填写 `cookie`。
- draftId 缺失：从 `/editor/drafts/{draftId}` 地址中获取。
- defaultCategoryId 缺失：从 `article_draft/update` 请求 Payload 的 `category_id` 获取。
- Cookie 失效：重新登录掘金并更新本地配置。

## 安全提醒

- 不提交 Cookie。
- 不提交 csrfToken。
- 不提交 DeepSeek Key。
- 不提交真实数据库密码。
- 不在日志或错误信息中输出完整 `auth_config`。
