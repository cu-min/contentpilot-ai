# ContentPilot AI

> 一个从“产品信息”到“多平台营销稿”的 AI 内容生产与发布管理平台。

ContentPilot AI 是一个面向内容运营、增长团队和产品营销人员的内部工具。它的目标不是简单生成一篇文章，而是把一篇营销内容从 **产品资料整理、AI 初稿生成、多平台改写、发布任务创建、平台草稿准备、人工发布交接** 串成一条完整工作流。

如果你正在推广一个产品，通常会遇到这些问题：

- 🧠 每次写文章都要重复整理产品介绍、优势、目标用户和品牌语气。
- ✍️ 同一篇内容不能直接复制到微信公众号、知乎、CSDN、掘金等平台。
- 🧩 不同平台的标题、结构、语气、技术细节和营销浓度都不一样。
- 📌 内容生成后，还需要管理文章状态、平台稿版本、发布任务和结果链接。
- 🔐 平台集成涉及登录态、Cookie、验证码和风控；系统只准备草稿或编辑器，最终发布由用户人工确认。

ContentPilot AI 解决的就是这条链路：让运营人员先维护一份产品上下文，再围绕推广主题生成文章，并自动改写成不同平台适合阅读和发布的版本。

## 适合谁使用

| 用户 | 典型诉求 |
|---|---|
| 内容运营 | 快速生成产品介绍、教程、解决方案、SEO 文章 |
| 增长团队 | 批量准备不同渠道的推广内容 |
| 产品营销 | 保持品牌语气、卖点和禁用词一致 |
| 技术内容团队 | 把同一主题改写成 CSDN、掘金等技术社区风格 |
| 内部工具开发者 | 参考 AI 内容生成、平台适配、发布任务的完整 MVP 架构 |

## 它能做什么

### 1. 维护产品上下文

在「产品配置」里填写当前要推广的产品信息：

- 产品名称
- 产品简介
- 官网链接
- 核心功能
- 目标用户
- 产品优势
- 品牌语气
- 禁用词/敏感词

这些信息会作为 AI 生成和平台改写的基础上下文，避免每次生成内容都从零开始描述产品。

### 2. 生成营销文章

运营人员输入一个推广主题，选择文章类型和语言，系统会生成：

- 标题
- 摘要
- Markdown 正文
- 标签
- 推荐关键词

支持的文章类型包括：

- 产品介绍
- 使用教程
- 行业科普
- 竞品对比
- 解决方案
- SEO 文章

### 3. 适配多个内容平台

同一个主题，在不同平台上的写法应该不同。ContentPilot AI 会根据平台特点改写内容：

| 平台 | 内容风格 |
|---|---|
| 微信公众号 | 更重场景、痛点、传播感和轻营销引导 |
| 知乎 | 更重问题背景、逻辑推导、客观观点 |
| CSDN | 更重技术步骤、配置说明、实践价值 |
| 掘金 | 更重开发者经验、效率工具、轻量技术分享 |

### 4. 管理文章和平台稿

系统会保存原始文章和不同平台的发布稿。运营人员可以查看、编辑、归档文章，也可以单独维护某个平台的标题、正文、标签和关键词。

### 5. 创建发布准备任务

内容准备好后，可以创建发布任务：

- 选择文章
- 选择平台稿
- 选择平台账号
- 设置立即发布或定时发布
- 提交为待发布状态
- 提交任务后按平台准备草稿或自动填充编辑器
- 检查准备结果后，在外部平台人工点击最终发布

当前产品边界是“自动准备，人工发布”：

| 平台 | 当前发布能力 |
|---|---|
| 微信公众号 | 官方 API 创建草稿，记录 media_id，不提交正式发布 |
| 掘金 | 草稿接口新建或更新草稿，不调用文章发布接口 |
| CSDN | Playwright 复用登录态，填充编辑器和发布弹窗，停在最终确认前 |
| 知乎 | Playwright 填标题正文、话题和专栏等设置，停在最终发布按钮前 |

所有浏览器自动化都遵守安全边界：不点击最终发布，不绕过登录或验证码，不规避平台风控。

## 一个简单例子

假设你要推广一个叫 **CodeReview Pro** 的代码审查工具。

### 第一步：配置产品信息

在「产品配置」中填写：

```text
产品名称：CodeReview Pro
产品简介：一个帮助研发团队自动发现代码风险、生成审查建议的 AI 工具
目标用户：研发团队、技术负责人、代码审查负责人
核心功能：AI 代码审查、风险提示、审查报告、团队协作
产品优势：减少人工 review 压力，提高代码质量，沉淀团队规范
品牌语气：专业、清晰、技术向、不过度营销
禁用词：绝对安全、100% 准确、替代程序员
```

### 第二步：生成一篇文章

在「AI 生成」里输入主题：

```text
研发团队为什么需要 AI 代码审查工具？
```

选择：

```text
文章类型：解决方案
语言：中文
```

系统会生成一篇完整文章，例如包含：

- 研发团队 review 压力越来越大的背景
- 人工审查容易遗漏的问题
- AI 代码审查适合承担的辅助角色
- CodeReview Pro 的使用场景
- 面向技术团队的温和转化引导

### 第三步：生成平台稿

进入文章详情，选择生成平台适配稿：

```text
微信公众号：偏场景化和传播感
知乎：偏问题分析和理性建议
CSDN：偏工具实践和使用步骤
掘金：偏开发者效率和经验分享
```

系统不会简单复制原文，而是按平台阅读习惯重组内容。

### 第四步：创建发布任务

进入「发布任务」：

```text
文章：研发团队为什么需要 AI 代码审查工具？
平台稿：掘金版本
平台账号：公司掘金账号
```

点击提交时选择“立即准备”或“定时准备”。两类任务都会进入 `PENDING`，由运营人员在计划时间到达后点击“准备发布”；当前 MVP 不扫描或自动执行定时任务。草稿或编辑器准备好后，任务进入 `WAITING_MANUAL_CONFIRM`，由运营人员在平台中人工检查并发布。

## 工作流

```mermaid
flowchart LR
    A[产品配置] --> B[AI 生成文章]
    B --> C[文章库]
    C --> D[生成平台适配稿]
    D --> E[平台账号配置]
    E --> F[创建发布任务]
    F --> G[按平台准备草稿或编辑器]
    G --> H[等待人工检查并发布]
```

## 当前完成度

✅ 已完成：

- 产品配置
- AI 文章生成
- 文章库
- Markdown 编辑与预览
- 微信公众号、知乎、CSDN、掘金平台稿生成
- 平台账号配置
- 发布任务创建、提交、取消和准备框架
- 微信、掘金草稿创建/更新试点
- CSDN、知乎 Chrome for Testing 登录态复用和编辑器填充试点
- 人工发布交接契约：`WAITING_MANUAL_CONFIRM`、`POST /prepare`、旧 `/execute` 兼容

🚧 当前重构：

- 新流程禁止自动点击最终发布，不再自动写入 `SUCCESS`
- 生产 Publisher Registry 删除 Mock 成功回退
- noVNC 改为本机绑定、SSH 隧道和强密码

🚧 暂不包含：

- SaaS 多租户和计费
- 复杂权限系统
- 真实数据看板和追踪链接
- 失败自动重试
- 平台验证码、登录验证或风控绕过
- 知乎 HTTP 非官方接口
- CSDN / 知乎失败自动重试
- 绕过验证码、登录验证或平台风控的能力

## 技术栈

### 后端

- Java 17
- Spring Boot 3.3.5
- Spring Security + JWT
- MyBatis-Plus
- MySQL
- Maven

### 前端

- React 18
- TypeScript
- Vite
- Ant Design
- MobX
- Axios
- React Router

## 项目结构

```text
.
├── backend/                 # Spring Boot 后端
│   ├── src/main/java/       # 业务代码
│   └── src/main/resources/  # 配置、schema.sql、data.sql
├── frontend/                # React 前端
│   └── src/
├── docs/                    # 产品设计、平台策略和阶段说明文档
└── README.md
```

## 多 Agent 开发

本项目采用轻量的 Team Lead、架构、前端、后端和 QA 协作模型。前后端在独立 Git Worktree 中并行实现，架构 Agent 负责必要契约和集成审查，QA 在集成结果上验证。

- 项目规则：`AGENTS.md`
- 文件所有权：`docs/agent/ownership.md`
- Worktree 使用：`docs/agent/worktrees.md`

## 本地运行

### 1. 准备数据库

本地创建 MySQL 数据库：

```sql
CREATE DATABASE ai_content_marketing DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

后端启动时会根据 `schema.sql` 和 `data.sql` 初始化基础表和默认管理员用户。

### 2. 配置后端

默认配置在：

```text
backend/src/main/resources/application.yml
```

数据库、JWT、DeepSeek 和 Exa 配置全部通过环境变量读取：

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=ai_content_marketing
export DB_USERNAME=root
export DB_PASSWORD=your_mysql_password
export JWT_SECRET=use_a_random_secret_with_at_least_32_characters
export DEEPSEEK_API_KEY=your_deepseek_api_key
export DEEPSEEK_BASE_URL=https://api.deepseek.com
export DEEPSEEK_MODEL=deepseek-v4-pro
export DEEPSEEK_TIMEOUT_SECONDS=60
export EXA_API_KEY=your_exa_api_key
export EXA_BASE_URL=https://api.exa.ai
export EXA_TIMEOUT_SECONDS=15
```

请不要将真实数据库密码、JWT 密钥、DeepSeek/Exa Key、Cookie、csrfToken 或其他平台认证信息写入 YAML 或提交到仓库。

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认后端地址：

```text
http://localhost:8080
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认前端地址：

```text
http://localhost:5173
```

前端开发代理会把 `/api` 请求转发到 `http://localhost:8080`。

## 发布安全边界

发布平台的自动化能力必须尊重平台规则。ContentPilot AI 的设计原则是：

- 不绕过验证码。
- 不绕过登录验证。
- 不规避平台风控。
- 不做刷量、自动评论或模拟真人行为。
- 遇到登录失效、验证码、风控或审核失败时，系统记录可读失败原因，不绕过平台限制。

微信和掘金只准备草稿；CSDN 和知乎通过 Chrome for Testing 复用登录态并填充到最终发布按钮前。涉及 Cookie、csrfToken、categoryId、tagIds、browserUserDataDir 等本地配置时，请只保存在平台账号的本地认证配置中，不要提交到仓库。noVNC 不向公网开放，必须通过 SSH 隧道和强密码访问。

## 相关文档

- `docs/project-progress-summary.md` — 项目进展总览与当前状态
- `docs/product-strategy-notes.md` — 产品策略反思与后续方向
- `docs/platform-content-adaptation.md` — 多平台内容改写规则
- `docs/platform-publishing-strategy-reference.md` — 平台发布策略与安全边界
- `docs/docker-deployment.md` — Docker 部署与运维指南
- `docs/frontend-design-system.md` — 前端设计规范
- `docs/contracts/manual-publish-handoff.md` — 人工发布交接契约

## License

当前未指定开源许可证。正式公开使用前请先补充 License。
