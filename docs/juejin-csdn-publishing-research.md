# 掘金 / CSDN 发布方案调研与试点计划

## 1. 调研目的

当前 MVP 已经完成：

```text
产品配置
AI 生成文章
文章库
多平台适配稿
平台账号配置
发布任务
MockPublisher 发布执行框架
```

下一步目标不是同时打通所有平台，而是先选择一个真实平台，验证从平台稿到发布结果的最小闭环。

本阶段只做调研和试点计划，不实现真实发布代码，不调用掘金接口，不接入 CSDN，不引入 Playwright / Selenium。

## 2. 当前发布架构基础

当前代码结构已经具备真实平台 Publisher 的基础：

1. `platform_account` 用于保存平台账号配置，包括平台、认证方式、敏感配置、默认发布方式和启用状态。
2. `article_platform_content` 用于保存不同平台的发布稿，包括标题、摘要、正文、标签和关键词。
3. `publish_task` 用于保存发布任务，包括平台、平台稿、平台账号、计划类型、发布时间、发布方式、状态、发布链接和错误信息。
4. `PlatformPublisher` 是统一发布接口，后续真实发布器应实现该接口。
5. `MockPublisher` 已用于验证 `PENDING -> RUNNING -> SUCCESS / FAILED` 的内部任务执行流程。
6. 后续真实发布只需要新增具体 Publisher，并注册到 `PublisherRegistry`，避免在 `PublishTaskService` 中写死平台逻辑。

## 3. 掘金发布方案调研

### 3.1 推荐发布方式

优先方案：Cookie + 非官方草稿接口试点。

兜底方案：浏览器自动化。

公开资料和社区经验中经常提到掘金创作端存在草稿相关接口，例如创建草稿、更新草稿、发布草稿等。但这些接口不是公开稳定的官方开放 API，本项目不能把它们当作长期可靠能力。阶段 9 如使用该方向，需要由人工在浏览器中抓包确认当前接口、参数、Header 和返回结构。

### 3.2 是否官方

标记为非官方接口，不稳定。

阶段 9 不应承诺“自动发布文章”，只建议做“创建草稿”。发布草稿、获取正式文章链接、处理审核和失败重试全部后置。

### 3.3 可能涉及的接口

以下接口类型仅作为待确认方向，不在本阶段实现：

- 创建草稿。
- 更新草稿。
- 发布草稿。
- 查询草稿或文章。

常见待人工确认路径包括：

- `/content_api/v1/article_draft/create`
- `/content_api/v1/article_draft/update`
- 草稿发布或文章查询相关接口。

这些路径、参数和鉴权方式都需要以当前浏览器抓包结果为准。

### 3.4 需要账号配置

建议在 `platform_account.auth_config` 中保存 JSON，不新增字段：

```json
{
  "cookie": "手动填写，禁止提交到 Git",
  "userAgent": "可选",
  "csrfToken": "如抓包发现需要则填写",
  "defaultCategoryId": "默认分类 ID",
  "defaultTagIds": ["标签 ID 1", "标签 ID 2"],
  "draftOnly": true
}
```

`platform_account` 建议配置：

- `platform = JUEJIN`
- `auth_type = COOKIE`
- `default_publish_mode = UNOFFICIAL_API`
- `enabled = 1`

### 3.5 内容字段映射

| 系统字段 | 掘金草稿字段建议 |
|---|---|
| `article_platform_content.title` | 草稿标题 |
| `article_platform_content.content` | Markdown 正文 |
| `article_platform_content.summary` | 摘要或 brief_content |
| `article_platform_content.tags` | 标签名称，实际提交可能需要映射为标签 ID |
| `auth_config.defaultTagIds` | 默认标签 ID |
| `auth_config.defaultCategoryId` | 默认分类 ID |

阶段 9 不建议做复杂标签搜索或自动匹配。先用账号配置中的默认分类 ID 和默认标签 ID，降低试点成本。

### 3.6 主要风险

- Cookie 过期。
- 非官方接口变化。
- 需要额外 Header、csrf、aid、uuid 或设备参数。
- 分类 ID、标签 ID 需要维护。
- 发布审核或内容风控。
- 非官方接口不稳定，可能随时失效。
- 如果直接正式发布，失败处理和风险边界都会扩大。

### 3.7 MVP 建议

- 阶段 9 只做创建草稿。
- 不做正式发布。
- 不做自动重试。
- 失败直接记录 `error_message`。
- 成功记录 `draft_id` 或草稿链接。
- 优先把草稿结果放入 `publish_task.publish_url`，例如草稿编辑页 URL。
- 保留 `MockPublisher`，用于本地验证和非掘金平台兜底。

## 4. CSDN 发布方案调研

### 4.1 推荐发布方式

推荐 Playwright 浏览器自动化，且优先自动填充 + 人工确认。

CSDN 写作页面是浏览器编辑器场景，公开稳定的文章发布 API 不明确。对于 MVP 来说，与其直接调用未确认接口，不如使用浏览器登录态打开 CSDN Markdown 编辑器，自动填充标题、正文、摘要、标签、分类，然后停在人工确认页面。

### 4.2 是否官方

不依赖公开官方 API。

如果后续发现 CSDN 有合规稳定的开放能力，可以独立评估；当前试点方案先按浏览器自动化处理。

### 4.3 需要账号配置

建议在 `platform_account.auth_config` 中保存 JSON，不新增字段：

```json
{
  "browserUserDataDir": "本机浏览器登录态目录，仅本地保存",
  "cookie": "可选，不建议长期依赖",
  "defaultCategory": "默认分类",
  "defaultColumn": "默认专栏，可选",
  "defaultTags": ["默认标签 1", "默认标签 2"],
  "manualConfirm": true
}
```

`platform_account` 建议配置：

- `platform = CSDN`
- `auth_type = BROWSER_PROFILE`，或本地试验时使用 `COOKIE`
- `default_publish_mode = BROWSER_AUTOMATION` 或 `MANUAL_CONFIRM`
- `enabled = 1`

### 4.4 内容字段映射

| 系统字段 | CSDN 编辑器字段建议 |
|---|---|
| `article_platform_content.title` | 标题 |
| `article_platform_content.content` | Markdown 正文 |
| `article_platform_content.summary` | 摘要 |
| `article_platform_content.tags` | 标签 |
| `auth_config.defaultCategory` | 默认分类 |
| `auth_config.defaultColumn` | 默认专栏，可选 |

### 4.5 主要风险

- 页面结构变化。
- 编辑器填充难度高。
- Markdown 编辑器可能基于 CodeMirror 或动态 DOM，不能只依赖普通 textarea。
- 登录态失效。
- 标签、分类、专栏控件变化。
- 发布按钮、草稿按钮或弹窗变化。
- 验证码、风控或内容审核。

### 4.6 MVP 建议

- 阶段 10 只做自动打开页面并填充内容。
- 优先人工确认。
- 不强制自动点击发布。
- 不做验证码处理。
- 不做风控绕过。
- 可将任务状态置为 `NEED_MANUAL_CONFIRM`，或在人工确认边界下先记为 `SUCCESS` 并写入说明，具体阶段 10 再确定。

## 5. 掘金 vs CSDN 试点对比

| 维度 | 掘金 | CSDN |
|---|---|---|
| 实现成本 | 中。需要抓包确认非官方草稿接口和参数 | 中高。需要浏览器自动化和编辑器填充 |
| 稳定性 | 中低。非官方接口可能变化 | 中。页面会变化，但人工确认边界更稳 |
| 是否需要浏览器自动化 | 阶段 9 可先不需要 | 基本需要 |
| 是否适合 Markdown | 适合 | 适合 |
| 账号配置复杂度 | Cookie、可选 csrf、默认分类 ID、默认标签 ID | 浏览器用户目录或 Cookie、默认分类、默认专栏、默认标签 |
| 是否适合当前 MVP | 更适合先做草稿闭环 | 更适合作为第二试点或可选阶段 |
| 主要风险 | 非官方接口、Cookie 过期、分类标签 ID 维护 | 页面结构、编辑器填充、登录态和验证码 |
| 推荐优先级 | 第一优先级 | 第二优先级，可选 |

结论：优先先做掘金草稿发布试点。CSDN 作为第二试点或可选阶段。

原因是掘金试点可以继续复用当前后端 Publisher 架构，不必立刻引入 Playwright / Selenium，也不需要前端或后端新增复杂自动化运行环境。CSDN 更适合在掘金草稿闭环跑通后，再作为浏览器自动化试点。

## 6. 阶段 9 建议方案：掘金草稿发布试点

阶段 9 最小开发范围：

1. 只做 `JuejinPublisher`。
2. 只做创建草稿。
3. 不做正式发布。
4. 不做 CSDN。
5. 不做 Playwright。
6. 不做追踪链接。
7. 不做数据看板。
8. 不做定时调度。
9. 不做重试。
10. 不提交 Cookie、Token、API Key。

阶段 9 执行流程：

```text
读取 publish_task
↓
读取 article_platform_content
↓
读取 platform_account
↓
解析 auth_config 中的掘金 Cookie、默认分类 ID、默认标签 ID
↓
调用掘金草稿创建接口
↓
成功：任务 SUCCESS，记录 draft_id 或 draft_url
失败：任务 FAILED，记录 error_message
```

建议实现细节：

- 新增 `JuejinPublisher implements PlatformPublisher`。
- `platform()` 返回 `JUEJIN`。
- `mode()` 返回 `UNOFFICIAL_API`。
- `PublisherRegistry` 继续按平台和发布方式选择 Publisher。
- 保留 `MockPublisher`，不删除。
- 只在服务端读取 `auth_config`，不要将 Cookie 返回给前端。
- 日志中不要打印完整 Cookie、Token 或请求 Header。
- 失败时只记录可读错误摘要。

成功结果建议：

- 若接口返回 `draft_id`，可先组装草稿编辑页 URL 写入 `publish_task.publish_url`。
- 若草稿 URL 不确定，可临时写入 `draft:{draftId}` 这种内部标记，但阶段 9 文档需要说明这不是公开访问链接。

是否需要改表：

- 阶段 9 不建议改表。
- `publish_task.publish_url` 可承载草稿编辑链接。
- `publish_task.error_message` 可承载失败原因。
- 若后续需要同时保存 `draft_id`、正式文章 URL、平台响应 ID，再考虑新增 `platform_publish_id` 或 `publish_result` 字段。

## 7. 阶段 10 建议方案：CSDN 自动填充试点，可选

阶段 10 建议作为可选阶段，不建议和阶段 9 合并。

建议范围：

1. 新增 `CsdnPublisher`。
2. 接入 Playwright。
3. 使用浏览器登录态打开 CSDN 编辑器。
4. 自动填充标题、正文、摘要、标签、分类。
5. 停在人工确认页面。
6. 状态可设为 `NEED_MANUAL_CONFIRM` 或 `SUCCESS`，阶段 10 再明确。
7. 不强制点击发布。

阶段 10 边界：

- 不处理验证码。
- 不绕过登录。
- 不规避风控。
- 不做自动正式发布。
- 不自动读取正式文章链接。
- 不做复杂重试。

## 8. 当前数据库和代码是否需要调整

### 8.1 platform_account

当前字段够用：

- `platform` 区分 `JUEJIN` / `CSDN`。
- `auth_type` 区分 `COOKIE` / `BROWSER_PROFILE`。
- `auth_config` 可保存 JSON 格式账号配置。
- `default_publish_mode` 可区分 `UNOFFICIAL_API` / `BROWSER_AUTOMATION` / `MANUAL_CONFIRM`。
- `enabled` 可控制账号是否启用。

阶段 9 不需要新增平台账号表字段。

### 8.2 article_platform_content

当前字段够用：

- `title` 对应平台标题。
- `summary` 对应摘要。
- `content` 对应 Markdown 正文。
- `tags` 和 `keywords` 可作为标签或关键词来源。
- `platform` 用于校验平台一致性。

阶段 9 不需要新增平台稿字段。

### 8.3 publish_task

当前字段基本够用：

- `platform_content_id` 关联平台稿。
- `account_id` 关联平台账号。
- `platform` 用于平台校验。
- `publish_mode` 用于选择 Publisher。
- `publish_url` 可先保存草稿链接或草稿标记。
- `error_message` 可保存失败原因。
- `status` 已支持 `RUNNING`、`SUCCESS`、`FAILED`。

阶段 9 不建议改表。若阶段 9 后发现必须长期保存平台草稿 ID，再考虑最小新增字段：

- `platform_publish_id`：保存平台草稿 ID 或平台文章 ID。

但这不是阶段 9 的前置条件，本阶段不修改表结构。

## 9. 安全边界

必须遵守：

1. 不绕过验证码。
2. 不绕过登录。
3. 不规避平台风控。
4. 不刷量。
5. 不自动评论。
6. 不自动浏览。
7. 不在代码或 Git 中保存真实 Cookie / Token。
8. 不在日志中打印完整 Cookie。
9. 非官方接口只作为 MVP 试点，可能失效。
10. 本地调试配置只能保存在不提交 Git 的本地配置或数据库中。

## 10. 最终建议

明确建议：

- 阶段 9 优先做掘金草稿发布试点。
- 阶段 10 暂不需要立即做，可作为 CSDN 自动填充可选阶段。
- 当前 MVP 可以只打通掘金草稿发布后进入联调与演示优化。

## 11. 参考资料与待确认项

本阶段调研参考了已有项目文档 `docs/platform-publishing-strategy-reference.md`、当前代码中的发布执行框架，以及公开页面/资料方向：

- Playwright 官方输入能力文档：https://playwright.dev/docs/input
- CSDN Markdown 编辑器入口：https://editor.csdn.net/md/
- 掘金创作端草稿接口方向：公开搜索可见社区提到 `content_api/v1/article_draft/create`、`article_draft/update` 等路径，但未找到稳定官方开放文档，必须在阶段 9 由人工抓包确认。

待人工确认：

- 掘金当前草稿创建接口路径、请求方法、Header、参数和返回结构。
- 掘金是否需要 csrf、aid、uuid、device_id 等额外参数。
- 掘金分类 ID、标签 ID 的获取方式。
- CSDN 当前编辑器 DOM、标题/正文/摘要/标签/分类控件选择器。
- CSDN 自动填充后应选择 `NEED_MANUAL_CONFIRM` 还是 `SUCCESS`。
