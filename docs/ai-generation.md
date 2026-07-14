# AI 文章生成模块说明

## DeepSeek 写作 + Exa 联网取材

AI 文章生成由后端按固定流程执行：先调用 Exa Search API 收集受限的资料包，再将产品配置和资料包交给 DeepSeek 写作。DeepSeek 仍是唯一写作模型；Exa 只负责检索，不使用 Exa MCP、Answer 或 Agent。

前端不会直接接触 Exa 或 DeepSeek API Key。所有密钥都通过环境变量读取，不应写入 `application.yml`、代码或 Git 提交。

## 环境变量

| 变量 | 说明 | 默认值 |
|---|---|---|
| `DEEPSEEK_BASE_URL` | DeepSeek API 基础地址 | `https://api.deepseek.com` |
| `DEEPSEEK_API_KEY` | DeepSeek API Key | 空 |
| `DEEPSEEK_MODEL` | 模型名称 | `deepseek-v4-pro` |
| `DEEPSEEK_TIMEOUT_SECONDS` | 请求超时时间，单位秒 | `60` |
| `EXA_API_KEY` | Exa Search API Key | 空 |
| `EXA_BASE_URL` | Exa API 基础地址 | `https://api.exa.ai` |
| `EXA_TIMEOUT_SECONDS` | 单次 Exa 请求超时时间，单位秒 | `15` |

如果 `EXA_API_KEY` 或 `DEEPSEEK_API_KEY` 为空，生成任务会失败，不会创建无资料的半成品文章。

## AI 生成流程

1. 前端可选择产品，向 `POST /api/ai/articles/generate` 提交可选的 `productConfigId`、主题、类型、语言和额外要求，立即收到 `{ taskId, status }`。
2. 后端创建 `ai_generation_task`，以有界后台线程池执行 `PENDING → SEARCHING → WRITING → SAVING → SUCCESS/FAILED`。
3. `SEARCHING` 阶段仅在传入 `productConfigId` 时读取所选产品配置；产品不存在时任务失败，不会改用其他产品。未选择产品时直接按主题、类型和备注进入通用检索与写作模式。
4. Exa Search API 检索 10 条候选资料，清洗、按 URL 去重后最多保留 5 条。产品官网可读取时优先占一个来源位置；官网失败但其余资料有效时仍可继续。
5. 检索词始终包含主题、类型和备注；选择产品后才附加产品名称、核心功能、目标用户和产品优势。主题或备注明确要求“最新、今天、本周、新闻、政策、价格、活动”等强时效信息时，才限制近 7 天；其他主题不加固定时间下限。
6. `WRITING` 阶段将资料包和可选的产品配置交给 `DeepSeekAiModelService`。未选择产品时，提示词禁止虚构品牌、官网、功能或案例；提示词也明确要求忽略网页内的任何指令，只提取与主题有关的事实。
7. `SAVING` 阶段解析并在同一事务中保存草稿文章和 `article_research_source` 来源记录；失败不会创建半成品文章。
8. 前端轮询 `GET /api/ai/generation-tasks/{taskId}`，成功后跳转文章详情；离开页面不会取消后台任务。

服务启动时，遗留的执行中任务会标记为失败，并提示重新生成。

## 联网资料规则与费用

- 默认只请求和存储标题、链接、域名、发布日期和清洗后的高相关片段；产品官网也仅请求片段，不下载整篇外部网页正文。这能降低延迟、Token 消耗和提示注入风险。
- 产品配置始终是可信上下文；外部网页属于不可信数据。来源链接仅在文章详情的“联网资料来源”卡片展示，不自动写入营销正文。
- Exa 无密钥、超时、限流、接口错误或无有效来源时任务会明确失败，不回退到旧的无资料生成流程。
- 该实现每次生成只发起一轮最多 10 条结果的 Exa Search。请以 [Exa API 价格页](https://exa.ai/pricing?tab=api) 的当前价格为准，并在 Exa 控制台设置预算和用量告警。

## 接口

### 提交生成任务

`POST /api/ai/articles/generate`

请求体保持原有字段不变。响应示例：

```json
{
  "code": 200,
  "data": { "taskId": 42, "status": "PENDING" }
}
```

### 查询任务进度

`GET /api/ai/generation-tasks/{taskId}`

仅创建该任务的用户可以查询。成功时返回 `articleId`；失败时返回可读的 `errorMessage`。

生成后的文章可以继续进入多平台适配和发布任务链路。当前项目已支持微信公众号、知乎、CSDN、掘金平台稿生成，并在发布执行层分别接入微信公众号官方 API、掘金非官方接口试点、CSDN 浏览器自动化和知乎浏览器自动化。

## 提示词构建逻辑

`PromptBuilder` 会组合以下信息：

- 产品名称
- 产品简介
- 官网链接
- 核心功能
- 目标用户
- 产品优势
- 品牌语气
- 禁用词/敏感词
- 文章主题
- 文章类型
- 语言
- 额外要求
- 经清洗和截断的联网资料包（最多 5 条）

提示词要求 AI 以内容营销运营人员身份写作，围绕产品配置但避免过度硬广，并只返回 JSON，不返回代码块或额外解释。

期望返回格式：

```json
{
  "title": "...",
  "summary": "...",
  "content": "...",
  "tags": ["标签1", "标签2"],
  "keywords": ["关键词1", "关键词2"]
}
```

## 安全要求

- 不要提交真实 DeepSeek API Key。
- 不要提交真实 Exa API Key。
- 不要在前端保存或调用 DeepSeek API Key。
- 不要在前端保存或调用 Exa API Key。
- 不要在日志中打印完整 Authorization Header。
- 本地密钥应通过环境变量或未提交的本地配置管理。
- AI 生成内容不得包含平台 Cookie、Token、浏览器用户目录或其他本地敏感认证信息。
- AI 只负责内容生产和平台风格适配，不负责绕过登录、验证码、审核或平台风控。

## 常见错误

| 错误 | 原因 | 处理 |
|---|---|---|
| `DeepSeek API Key 未配置` | 未设置 `DEEPSEEK_API_KEY` | 设置环境变量后重启后端 |
| `Exa API Key 未配置` | 未设置 `EXA_API_KEY` | 设置环境变量后重启后端 |
| `Exa 请求过于频繁，请稍后重试` | Exa 返回 429 | 降低并发或稍后重试，并检查 Exa 用量限制 |
| `Exa 未返回可用资料来源，请调整主题后重试` | 搜索结果无有效链接或片段 | 调整主题、补全产品配置或稍后重试 |
| `请先完成产品配置` | `product_config` 中产品名称为空 | 先进入产品配置页保存产品名称 |
| `AI 返回格式解析失败，请重试` | AI 未按 JSON 格式返回 | 重新生成，或调整提示词 |
| `AI 调用失败，请检查 DeepSeek 配置或稍后重试` | 网络、模型、Key 或服务端异常 | 检查配置和 DeepSeek 服务状态 |
