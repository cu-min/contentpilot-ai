# 项目进展汇总

> 更新时间：2026-07-16。本文用于快速了解 AI 内容营销系统的当前能力、人工发布交接边界和待实现项。

## 总体状态

系统已完成从产品信息到平台发布稿的主链路：

```text
产品配置 → AI 生成文章 → 多平台内容适配 → 平台账号 → 发布任务 → 草稿/编辑器准备 → 人工发布
```

项目仍定位为内部工具。为避免误发布、平台风控和非公开接口变更风险，系统的自动化终点固定为“草稿或编辑器已准备”，最终发布必须由用户人工点击。

## 阶段简表

| 阶段 | 状态 | 简要说明 |
|---|---|---|
| Phase 1 项目基础框架 | 已完成 | Spring Boot、React、登录鉴权和角色基础能力 |
| Phase 2 产品配置与文章库 | 已完成 | 产品上下文、文章列表、详情和编辑 |
| Phase 3 AI 文章生成 | 已完成 | DeepSeek 接入、结构化内容生成 |
| Phase 4 平台稿适配 | 已完成 | 微信、知乎、CSDN、掘金内容改写 |
| Phase 5 AI 生成增强 | 已完成 | JSON 解析、联网调研、错误提示和入库 |
| Phase 6 多平台内容管理 | 已完成 | 平台稿保存、编辑、归档和恢复 |
| Phase 7 平台账号与发布任务 | 已完成 | 账号配置与 DRAFT/PENDING/CANCELLED 流转 |
| Phase 8 发布准备框架 | 重构中 | 统一 `/prepare`、`WAITING_MANUAL_CONFIRM` 和人工交接 |
| Phase 9 掘金草稿 | 重构中 | 保留草稿创建/更新，移除正式发布 |
| Phase 10 微信公众号草稿 | 重构中 | 保留官方 API 草稿创建，移除正式发布 |
| Phase 11 CSDN 编辑器准备 | 重构中 | 自动填充并停在最终确认前 |
| Phase 12 知乎编辑器准备 | 重构中 | 自动填充并停在最终发布按钮前 |
| Phase 13 文档与部署安全 | 重构中 | 清理 Mock 成功语义，收紧 noVNC 访问 |

“重构中”表示本文已冻结新契约，代码实现和 QA 验收尚需完成，不应把历史真实发布试点当作新边界。

## 平台准备能力

| 平台 | 保留能力 | 正常结果 | 不再包含 |
|---|---|---|---|
| 微信公众号 | 官方 API 创建草稿 | media_id、草稿交接信息 | 提交正式发布、发布状态轮询 |
| 掘金 | 创建或更新草稿 | 草稿 ID、草稿 URL | 提交正式文章 |
| CSDN | 复用登录态，填标题正文及弹窗字段 | 发布弹窗展示且等待人工确认 | 最终发布点击和结果猜测 |
| 知乎 | 复用登录态，填标题正文、话题和专栏 | 编辑器展示且等待人工发布 | 发布点击和创作中心结果猜测 |

## 任务契约

新任务使用：

```text
DRAFT → PENDING → RUNNING → WAITING_MANUAL_CONFIRM
                                  └→ FAILED
DRAFT/PENDING → CANCELLED
```

正式准备接口为：

```http
POST /api/publish/tasks/{id}/prepare
```

`/execute` 暂作兼容别名。`SUCCESS` 仅用于历史数据展示，新流程不自动写入。详细契约见 `docs/contracts/manual-publish-handoff.md`。

## 安全边界

- 不提交 Cookie、Token、API Key、数据库密码、JWT Secret 或浏览器用户目录。
- 不绕过登录、验证码或平台风控。
- 生产 Publisher Registry 缺少实现时直接失败，不返回 Mock 成功。
- noVNC 只绑定宿主机 `127.0.0.1:6080`，通过 SSH 隧道访问。
- `VNC_PASSWORD` 必填，为空时后端容器必须拒绝启动。
- 公网登录与 JWT 传输必须使用 HTTPS。

## 验证入口

```bash
./scripts/verify-backend.sh
./scripts/verify-frontend.sh
./scripts/verify-all.sh
docker compose config
```

QA 不得在自动验收中点击最终发布。真实平台验收只能证明草稿或编辑器已准备，不能声称正式文章已发布。

## 后续顺序

1. 完成后端状态、四平台准备路径和 Mock 清理。
2. 完成前端新状态、准备按钮和交接入口。
3. 完成 noVNC、密码、CORS 与 HTTPS 配置。
4. 由 QA 执行静态边界检查、后端测试、前端构建和人工交接冒烟。
