# ContentPilot AI

ContentPilot AI 是一个面向内部运营团队的 AI 内容营销平台。它围绕单一产品完成内容生产、平台适配、发布任务管理和发布结果记录，帮助运营人员更快地生成、改写并分发营销内容。

当前版本是 MVP，重点验证从“产品配置 -> AI 生成文章 -> 多平台适配 -> 创建发布任务 -> 执行发布链路”的完整流程。

## 核心能力

- 产品配置：维护产品名称、简介、官网、核心功能、目标用户、产品优势、品牌语气和禁用词。
- AI 文章生成：根据主题、文章类型、语言和产品上下文生成营销文章。
- 多平台适配：支持微信公众号、知乎、CSDN、掘金的平台化改写。
- 文章库管理：查看文章列表、文章详情、平台稿和发布状态。
- 平台账号管理：配置不同平台账号、认证方式和默认发布模式。
- 发布任务：支持创建发布任务、提交待发布状态并手动执行。
- 掘金草稿试点：已接入掘金草稿更新链路，真实发布仍建议人工确认。

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

建议把本地敏感配置放到下面这个文件中：

```text
backend/config/application-local.yml
```

该文件已被 `.gitignore` 忽略，不会提交到 Git。

示例：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_content_marketing?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: your_mysql_password
```

DeepSeek 相关配置当前通过环境变量读取：

```bash
export DEEPSEEK_API_KEY=your_deepseek_api_key
export DEEPSEEK_BASE_URL=https://api.deepseek.com
export DEEPSEEK_MODEL=deepseek-v4-pro
export DEEPSEEK_TIMEOUT_SECONDS=60
```

请不要提交真实数据库密码、DeepSeek Key、Cookie、csrfToken 或其他平台认证信息。

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

## 发布链路说明

当前 MVP 的发布链路以安全可控为主：

1. 先生成原始文章。
2. 再生成不同平台的适配稿。
3. 选择平台账号创建发布任务。
4. 提交任务为待发布状态。
5. 由运营人员手动执行任务。
6. 系统记录发布链接或失败原因。

掘金平台目前支持草稿更新试点。涉及 Cookie、draftId、categoryId 等本地配置时，请只保存在平台账号的本地认证配置中，不要提交到仓库。

## 当前边界

- 不做 SaaS 多租户、计费和复杂权限。
- 不绕过验证码、登录验证或平台风控。
- 不做自动评论、刷量或模拟真人行为。
- 暂不支持微信公众号、知乎、CSDN 的真实自动发布。
- 暂不包含真实数据看板、追踪链接、定时调度和失败重试。

## 相关文档

- `docs/AI_Content_Marketing_System_Product_Design_MVP.md`
- `docs/ai-generation.md`
- `docs/platform-content-adaptation.md`
- `docs/platform-publishing-strategy-reference.md`
- `docs/juejin-csdn-publishing-research.md`
- `docs/mvp-final-check.md`

## License

当前未指定开源许可证。正式公开使用前请先补充 License。
