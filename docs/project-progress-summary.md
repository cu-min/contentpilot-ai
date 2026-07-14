# 项目进展汇总

> 更新时间：2026-07-13。本文用于快速了解 AI 内容营销系统当前阶段、已完成能力、平台发布边界和后续建议。

## 总体状态

当前项目已从内容生成 MVP 进入“多平台真实发布试点”阶段。主链路已经跑通：

```text
产品配置 → AI 生成文章 → 多平台内容适配 → 平台账号配置 → 发布任务 → 平台发布器执行 → 发布结果记录
```

系统仍定位为内部工具，不做 SaaS 多租户、计费、复杂权限、追踪链接、数据看板、自动失败重试，也不绕过登录、验证码或平台风控。

## 阶段简表

| 阶段 | 状态 | 简要说明 |
|---|---|---|
| Phase 1 项目基础框架 | 已完成 | Spring Boot、React、登录鉴权、角色基础能力 |
| Phase 2 产品配置与文章库 | 已完成 | 产品上下文维护、文章列表、详情、编辑 |
| Phase 3 AI 文章生成 | 已完成 | DeepSeek 接入，生成标题、摘要、正文、标签、关键词 |
| Phase 4 平台稿适配 | 已完成 | 微信公众号、知乎、CSDN、掘金平台稿生成 |
| Phase 5 AI 生成增强 | 已完成 | JSON 解析、错误提示、文章入库和编辑流程 |
| Phase 6 多平台内容管理 | 已完成 | 平台稿保存、编辑、归档、恢复 |
| Phase 7 平台账号与发布任务 | 已完成 | 账号 auth_config、发布任务 DRAFT/PENDING/CANCELLED |
| Phase 8 发布执行框架 | 已完成 | PlatformPublisher 抽象、状态流转、失败原因记录 |
| Phase 9 掘金真实发布 | 已完成 | 非官方接口试点，新建草稿、更新内容、提交发布、取链 |
| Phase 10 微信公众号官方 API | 已完成 | 创建草稿、可选提交发布、状态刷新、发布结果同步 |
| Phase 11 CSDN 浏览器自动化 | 已完成 | Chrome for Testing 登录态复用、自动填充、发布弹窗处理、结果检测 |
| Phase 12 知乎浏览器自动化 | 已完成 | 自动填充、发布设置检测、自动点击发布、创作中心结果确认 |
| Phase 13 文档与演示收尾 | 当前完成 | 更新项目进展、平台策略、README 和设计文档 |

## 平台发布能力

| 平台 | 当前能力 | 成功结果 | 主要边界 |
|---|---|---|---|
| 微信公众号 | 官方 API 创建草稿；配置允许时提交正式发布并刷新状态 | 草稿 media_id、publish_id、文章链接或处理状态 | 不绕过平台权限；封面、素材、IP 白名单和公众号权限需平台侧满足 |
| 掘金 | Cookie + 非官方接口试点；新建草稿、更新内容、提交发布、查询文章状态 | 正式文章链接、审核中状态或可读失败原因 | 非官方接口可能变化；Cookie、分类、标签需本地配置 |
| CSDN | Playwright 浏览器自动化；填标题、正文、标签、摘要、分类/专栏并自动发布试点 | 正式文章链接或内容管理页确认 | 依赖页面 DOM 和登录态；验证码/风控转人工 |
| 知乎 | Playwright 浏览器自动化；填标题、正文、处理发布设置、点击发布并检测结果 | 正式文章链接或创作中心匹配确认 | 依赖 Chrome for Testing 登录态；验证码、登录失效、页面结构变化不绕过 |

## 关键配置示例

### 掘金

```json
{
  "cookie": "本地填写，不提交 Git",
  "userAgent": "浏览器 User-Agent，可选",
  "csrfToken": "可选，接口失败时补充",
  "aid": "2608",
  "uuid": "",
  "defaultCategoryId": "默认分类 ID",
  "defaultTagIds": ["默认标签 ID"],
  "draftOnly": false,
  "syncToOrg": false,
  "columnIds": [],
  "themeIds": []
}
```

### CSDN

```json
{
  "browserUserDataDir": "/Users/yourname/ai-content-marketing-browser/csdn",
  "editorUrl": "https://editor.csdn.net/md/?not_checkout=1",
  "manageUrl": "https://mp.csdn.net/mp_blog/manage/article",
  "defaultTags": ["Java", "Spring Boot", "AI"],
  "defaultCategory": "后端",
  "defaultColumn": "",
  "defaultSummary": "",
  "manualConfirm": false,
  "autoPublish": true
}
```

### 知乎

```json
{
  "browserUserDataDir": "/Users/yourname/ai-content-marketing-browser/zhihu",
  "editorUrl": "https://zhuanlan.zhihu.com/write",
  "manageUrl": "https://www.zhihu.com/creator",
  "defaultTopics": ["台风", "科学"],
  "defaultColumn": "",
  "manualConfirm": false,
  "autoPublish": true,
  "waitAfterFillMs": 1000
}
```

## 历史阶段文档合并说明

早期两个历史阶段文档的有效内容已经合并到本文和 `platform-publishing-strategy-reference.md`。后续以这两个文档作为项目进展和发布策略入口，避免旧计划与当前实现状态混淆。

## 知乎最新实机验证

2026-07-09 实机确认：

- 可以打开 `https://zhuanlan.zhihu.com/write` 或编辑页。
- 标题填充成功，策略为 `direct-input-fill`。
- 正文粘贴后可通过归一化文本校验。
- 自动处理“草稿加载中”与 Markdown 解析提示。
- 能识别底部“发布设置”区域和已有文章话题 chip。
- `autoPublish=true` 时可以点击底部“发布”按钮。
- 发布后任务可进入 `SUCCESS`，前端展示“知乎发布成功”并提供查看入口。

## 安全边界

- 不提交 Cookie、Token、DeepSeek Key、数据库密码或本机浏览器目录。
- 不实现知乎 HTTP 非官方接口。
- 不绕过验证码、登录验证或平台安全验证。
- 不规避平台风控，不做刷量、自动评论、自动互动。
- 自动发布失败时记录明确原因，必要时进入人工确认。

## 验证命令

```bash
cd backend
mvn -s maven-central-settings.xml test

cd ../frontend
npm run build
```

最近一次验证结果：

- 后端测试通过：26 tests passed。
- 前端构建通过：Vite 仅提示既有大 chunk 警告。

## 上线后待办

- 域名 `contentpilot-ai.cloud` 和 `www.contentpilot-ai.cloud` 已解析到公网 IP，并可通过 HTTP 访问系统。
- 后续配置 HTTPS/SSL：为 `contentpilot-ai.cloud` 和 `www.contentpilot-ai.cloud` 申请证书，配置 Nginx 监听 `443`，并将 `80` 自动重定向到 `443`。
- HTTPS 完成前，浏览器会显示“不安全”；登录密码和 JWT 在公网环境下建议尽快切换到 HTTPS。
- 如需长期使用国内服务器域名访问，继续完成 ICP 备案并确认腾讯云安全组仅开放必要端口。
