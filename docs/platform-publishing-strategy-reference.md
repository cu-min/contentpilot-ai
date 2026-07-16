# AI 内容营销系统：平台发布准备策略

> 更新时间：2026-07-16。用途：记录平台草稿、编辑器填充、人工发布交接与安全边界。跨端细节以 `docs/contracts/manual-publish-handoff.md` 为准。

## 1. 核心结论

系统不再执行最终发布动作。统一边界是：

```text
准备平台草稿或编辑器
↓
验证内容已写入
↓
返回 WAITING_MANUAL_CONFIRM
↓
用户在外部平台检查并人工点击发布
```

| 平台 | 准备方式 | 系统终点 | 禁止动作 |
|---|---|---|---|
| 微信公众号 | 官方 API | 草稿创建完成 | 提交正式发布 |
| 掘金 | 草稿接口 | 草稿创建或更新完成 | 调用文章发布接口 |
| CSDN | Playwright | 发布弹窗字段已填充 | 点击最终确认发布 |
| 知乎 | Playwright | 标题、正文和发布设置已填充 | 点击发布或最终确认 |

这个边界降低非官方接口、DOM 变化、账号异常和平台风控带来的误发布风险。

## 2. 统一设计

### 2.1 任务状态

```text
DRAFT
PENDING
RUNNING
WAITING_MANUAL_CONFIRM
FAILED
CANCELLED
```

- `RUNNING` 只用于程序正在准备的短暂过程。
- `WAITING_MANUAL_CONFIRM` 是新流程的正常交付终点。
- `SUCCESS` 只保留给历史数据展示，新流程不写入。
- 登录、验证码、接口或页面填充失败时记录 `FAILED` 及可读原因。

### 2.2 准备接口

```http
POST /api/publish/tasks/{id}/prepare
```

旧的 `POST /api/publish/tasks/{id}/execute` 暂作兼容别名，新页面只使用 `/prepare`。

### 2.3 结果记录

准备成功时：

- 保存平台返回的 `externalDraftId`。
- 保存草稿或当前编辑器的 `draftUrl`。
- `publishUrl` 保持为空。
- `articleStatus` 不写入 `PUBLISHED`。
- 系统不猜测用户是否已在外部页面完成发布。

Publisher Registry 只允许精确平台和方式匹配。缺少实现时必须失败，生产代码不得回退到会返回虚假成功的 Mock Publisher。

## 3. 微信公众号

### 准备链路

```text
获取 access_token
↓
准备封面素材
↓
Markdown 转公众号 HTML
↓
创建公众号草稿
↓
记录 media_id
↓
WAITING_MANUAL_CONFIRM
```

不调用微信正式发布接口，不轮询正式文章链接。旧配置中的 `draftOnly` 可暂时保留以兼容数据，但不再改变行为：系统始终只创建草稿。

常见失败包括 AppID/AppSecret 错误、IP 白名单、封面素材缺失和账号权限不足。

## 4. 掘金

### 准备链路

```text
读取本地 Cookie 配置
↓
新建或复用草稿
↓
更新标题、正文、摘要、分类和标签
↓
记录草稿 ID 和草稿 URL
↓
WAITING_MANUAL_CONFIRM
```

不调用文章发布接口。旧配置中的 `draftOnly` 不再改变行为。由于草稿接口属于非公开能力，Cookie、分类和标签 ID 必须只保存在本地配置中。

## 5. CSDN

### 准备链路

```text
加载 Chrome for Testing 登录态
↓
打开 Markdown 编辑器
↓
填写标题和正文
↓
点击第一层“发布文章”打开设置弹窗
↓
填写标签、摘要、分类和原创声明
↓
确认最终按钮存在，但不点击
↓
WAITING_MANUAL_CONFIRM
```

系统必须保留编辑器窗口，供用户检查并人工完成最后一步。登录失效、验证码或 DOM 变化时不尝试绕过。

## 6. 知乎

### 准备链路

```text
加载 Chrome for Testing 登录态
↓
打开知乎编辑器
↓
填写标题和正文
↓
处理草稿加载和 Markdown 解析提示
↓
填写话题、专栏等发布设置
↓
确认发布按钮存在，但不点击
↓
WAITING_MANUAL_CONFIRM
```

不实现知乎 HTTP 非官方发布接口，不自动点击发布或最终确认。

## 7. 平台账号配置

CSDN 示例：

```json
{
  "browserUserDataDir": "/app/browser-data/csdn",
  "editorUrl": "https://editor.csdn.net/md/?not_checkout=1",
  "defaultTags": ["Java", "Spring Boot", "AI"],
  "defaultCategory": "后端",
  "defaultColumn": "",
  "defaultSummary": ""
}
```

知乎示例：

```json
{
  "browserUserDataDir": "/app/browser-data/zhihu",
  "editorUrl": "https://zhuanlan.zhihu.com/write",
  "manageUrl": "https://www.zhihu.com/creator",
  "defaultTopics": ["项目管理", "效率工具"],
  "defaultColumn": "",
  "waitAfterFillMs": 1000
}
```

`manualConfirm`、`autoPublish`、`draftOnly` 不再是可以改变最终发布边界的开关。旧 JSON 可被兼容读取，但新页面和文档不再引导配置自动发布。

## 8. 验收边界

自动验收只检查：

- 微信、掘金草稿创建或更新成功。
- CSDN、知乎编辑器完整填充，并停在最终发布按钮前。
- 任务进入 `WAITING_MANUAL_CONFIRM`，交接 URL 可用。
- 缺少平台实现时直接失败，不返回 Mock 成功。

自动验收不点击最终发布，不访问真实正式文章链接，不评判外部平台是否已完成人工发布。

## 9. 安全边界

系统不实现：

- 验证码、登录或平台风控绕过。
- 自动点击最终发布。
- 自动点赞、评论、刷量或批量垃圾内容发布。

敏感信息必须仅保存在后端本地配置中，前端脱敏展示，日志不明文输出，Git 不提交 Cookie、Token、API Key 或浏览器用户目录。
