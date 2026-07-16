# Docker 部署指南

本文说明如何用 Docker Compose 部署 AI 内容营销系统，并通过受保护的 noVNC 访问 CSDN、知乎编辑器。系统只自动填充到最终发布按钮前，不自动完成发布。

## 1. 服务器与端口

推荐 Ubuntu 22.04 LTS 或更新版本。基础系统可以使用 2 核 2G，运行 Playwright Chromium、Xvfb 和 noVNC 时建议至少 2 核 4G。

公网安全组只开放：

- `22`：SSH，建议限制来源 IP 并使用密钥登录。
- `80`：仅用于跳转 HTTPS。
- `443`：系统 HTTPS 访问。

不向公网开放：

- `3306`：MySQL 只在 Compose 内部访问。
- `8080`：后端由前端 Nginx 通过 `/api` 反向代理。
- `5900`：VNC 仅在容器内使用。
- `6080`：noVNC 必须只绑定宿主机 `127.0.0.1`，不得在安全组中开放。

## 2. 安装 Docker

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list >/dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker "$USER"
```

重新登录 SSH 后验证：

```bash
docker version
docker compose version
```

## 3. 获取项目和环境变量

```bash
git clone <你的仓库地址>
cd ai-content-marketing-system
cp .env.example .env
vim .env
```

生产环境至少需要：

- `MYSQL_ROOT_PASSWORD`
- `MYSQL_PASSWORD`
- `DB_PASSWORD`，与 `MYSQL_PASSWORD` 保持一致
- `JWT_SECRET`，使用足够长的随机字符串
- `VNC_PASSWORD`，使用强密码，为空时后端容器必须拒绝启动
- `DEEPSEEK_API_KEY`
- `EXA_API_KEY`，如需联网调研

不要提交 `.env`。不要把密码、Cookie、Token 或浏览器用户目录写入镜像或 Git。

## 4. 启动和验证

```bash
docker compose config
docker compose up -d --build
docker compose ps
```

查看日志：

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql
```

前端 Nginx 将 `/api/` 代理到 `backend:8080`，SPA 页面刷新回退到 `index.html`。公网使用前必须配置 HTTPS，并将 HTTP 重定向到 HTTPS。

## 5. 安全访问 noVNC

Compose 中 noVNC 的主机绑定必须是：

```yaml
ports:
  - "127.0.0.1:6080:6080"
```

禁止使用 `0.0.0.0:6080`或直接 `6080:6080`对公网发布。后端启动脚本必须在 `VNC_PASSWORD` 为空时退出，禁止使用 x11vnc `-nopw`。

在本机建立 SSH 隧道：

```bash
ssh -N -L 6080:127.0.0.1:6080 <user>@<server>
```

然后仅在本机浏览器打开：

```text
http://127.0.0.1:6080
```

输入 `.env` 中的强 `VNC_PASSWORD`。noVNC 只用于首次登录、检查编辑器填充结果和人工点击最终发布。

## 6. CSDN / 知乎登录态

容器内浏览器登录态必须保存到持久化路径。

CSDN 示例：

```json
{
  "browserUserDataDir": "/app/browser-data/csdn",
  "editorUrl": "https://editor.csdn.net/md/?not_checkout=1",
  "manageUrl": "https://mp.csdn.net/mp_blog/manage/article",
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

不配置 `autoPublish=true`。旧配置中即使存在 `autoPublish`、`manualConfirm` 或 `draftOnly`，新流程也必须停在人工发布前。

操作顺序：

1. 通过 SSH 隧道打开 noVNC。
2. 在容器浏览器中登录 CSDN 和知乎。
3. 平台账号使用 `/app/browser-data/csdn` 或 `/app/browser-data/zhihu`。
4. 执行“准备发布”。
5. 任务进入 `WAITING_MANUAL_CONFIRM` 后，在 noVNC 中检查内容并人工点击。

## 7. 常用命令

```bash
docker compose restart backend
docker compose restart
docker compose down
docker compose up -d --build
docker compose down --remove-orphans
```

查看 MySQL 数据卷：

```bash
docker volume ls | grep mysql
docker volume inspect ai-content-marketing-system_mysql-data
```

## 8. 常见问题

- 页面打不开：检查 `443`、TLS 证书和 `frontend` 容器。
- 接口 404：检查 `frontend/nginx.conf` 的 `/api/` 代理和 `backend:8080`。
- 后端连不上数据库：检查 `DB_HOST=mysql`、`DB_PASSWORD` 和 `MYSQL_PASSWORD`。
- noVNC 打不开：确认 SSH 隧道存在、Compose 只绑定 `127.0.0.1:6080` 且 `VNC_PASSWORD` 非空；不要开放公网 6080。
- CSDN / 知乎提示登录：通过 noVNC 在容器浏览器内重新登录，不绕过验证。
- 2G 内存浏览器卡顿：减少同时准备的任务，必要时升级配置。

## 9. 交付前安全检查

- `docker compose config` 不包含空的 DB、JWT 或 VNC 密钥。
- `docker compose ps` 不向公网发布 3306、8080、5900 或 6080。
- 公网 HTTP 自动跳转 HTTPS。
- Git 和容器日志中没有 Cookie、Token、API Key 或密码。
- CSDN、知乎自动化只做内容填充，不点击最终发布。
