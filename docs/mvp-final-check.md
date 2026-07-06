# MVP 收尾检查

## 已完成能力

- 产品配置、AI 生成文章、文章库。
- 微信公众号、知乎、CSDN、掘金平台稿生成与管理。
- 平台账号配置、发布任务、发布执行链路。
- 掘金新建草稿、更新内容并提交真实文章发布。
- 用户已本地验证掘金真实发布成功，文章进入审核中状态。

## 当前不做

- CSDN 自动填充、微信公众号真实发布、知乎发布。
- 追踪链接、真实数据看板、ECharts 图表。
- 定时调度、自动失败重试。
- Redis、SaaS 多租户、操作日志。

## 掘金 auth_config 示例

```json
{
  "cookie": "本地填写，不提交 Git",
  "userAgent": "浏览器 User-Agent，可选",
  "csrfToken": "可选，接口失败时补充",
  "aid": "2608",
  "uuid": "",
  "defaultCategoryId": "从 article_draft/update 请求 Payload 的 category_id 获取",
  "defaultTagIds": ["从 query_tag_list 或发布请求 Payload 获取"],
  "draftOnly": false,
  "syncToOrg": false,
  "columnIds": [],
  "themeIds": [],
  "encryptedWordCount": 1077883,
  "originWordCount": 3
}
```

## 本地验证流程

1. 配置 `JUEJIN + COOKIE + UNOFFICIAL_API` 平台账号。
2. `auth_config` 填写合法 JSON，至少包含 `cookie`、`defaultCategoryId`、`defaultTagIds`。
3. 创建文章并生成掘金平台稿。
4. 创建发布任务并提交为 `PENDING`。
5. 执行发布任务。
6. `draftOnly=false` 时系统自动新建草稿、更新内容并提交发布。
7. 发布成功后任务写入正式文章链接；如果平台进入审核中，掘金后台会显示审核状态。

## 常见错误

- JSON 格式错误：检查双引号、逗号和字段格式。
- Cookie 缺失：在认证配置中填写 `cookie`。
- defaultCategoryId 缺失：从 `article_draft/update` 请求 Payload 的 `category_id` 获取。
- defaultTagIds 缺失：从 `query_tag_list` 或发布请求 Payload 获取至少一个标签 ID。
- Cookie 失效：重新登录掘金并更新本地配置。

## 安全提醒

- 不提交 Cookie。
- 不提交 csrfToken。
- 不提交 DeepSeek Key。
- 不提交真实数据库密码。
- 不在日志或错误信息中输出完整 `auth_config`。
