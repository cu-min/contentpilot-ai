# AI 文章生成模块说明

## DeepSeek 配置方式

AI 文章生成由后端调用 DeepSeek V4 Pro。前端不会直接接触 DeepSeek API Key。

本项目通过环境变量读取 AI 配置，不应把真实 API Key 写入 `application.yml`、代码或 Git 提交。

## 环境变量

| 变量 | 说明 | 默认值 |
|---|---|---|
| `DEEPSEEK_BASE_URL` | DeepSeek API 基础地址 | `https://api.deepseek.com` |
| `DEEPSEEK_API_KEY` | DeepSeek API Key | 空 |
| `DEEPSEEK_MODEL` | 模型名称 | `deepseek-v4-pro` |
| `DEEPSEEK_TIMEOUT_SECONDS` | 请求超时时间，单位秒 | `60` |

如果 `DEEPSEEK_API_KEY` 为空，调用生成接口会返回：`DeepSeek API Key 未配置`。

## AI 生成流程

1. 前端在 `/ai-generate` 输入文章主题、文章类型、语言和额外要求。
2. 后端校验登录态和请求参数。
3. 后端读取单产品配置 `product_config`。
4. 如果产品配置未完成，返回：`请先完成产品配置`。
5. 后端基于产品配置和写作要求构建提示词。
6. `AiModelService` 调用当前模型实现 `DeepSeekAiModelService`。
7. DeepSeek 返回 JSON 格式文章内容。
8. 后端解析 `title`、`summary`、`content`、`tags`、`keywords`。
9. 后端保存到 `article` 表，状态为 `DRAFT`。
10. 前端跳转到文章编辑页查看和继续编辑。

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
- 不要在前端保存或调用 DeepSeek API Key。
- 不要在日志中打印完整 Authorization Header。
- 本地密钥应通过环境变量或未提交的本地配置管理。
- AI 生成内容不得包含平台 Cookie、Token、浏览器用户目录或其他本地敏感认证信息。
- AI 只负责内容生产和平台风格适配，不负责绕过登录、验证码、审核或平台风控。

## 常见错误

| 错误 | 原因 | 处理 |
|---|---|---|
| `DeepSeek API Key 未配置` | 未设置 `DEEPSEEK_API_KEY` | 设置环境变量后重启后端 |
| `请先完成产品配置` | `product_config` 中产品名称为空 | 先进入产品配置页保存产品名称 |
| `AI 返回格式解析失败，请重试` | AI 未按 JSON 格式返回 | 重新生成，或调整提示词 |
| `AI 调用失败，请检查 DeepSeek 配置或稍后重试` | 网络、模型、Key 或服务端异常 | 检查配置和 DeepSeek 服务状态 |
