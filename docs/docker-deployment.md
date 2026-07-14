# Docker 部署指南

本文说明如何用 Docker Compose 在腾讯云服务器部署 AI 内容营销系统，并支持 CSDN、知乎的 Playwright 可视化浏览器自动化。

## 1. 腾讯云服务器准备

推荐使用 Ubuntu 22.04 LTS 或更新的 Ubuntu 发行版。2 核 2G 可以运行基础系统，但 Playwright Chromium、Xvfb、VNC/noVNC 会消耗较多内存；如果同时执行多个浏览器发布任务，建议至少 2 核 4G。

安全组建议开放：

- `22`：SSH 登录
- `80`：前端访问
- `6080`：noVNC 访问，用于登录和观察浏览器

不建议开放：

- `3306`：MySQL 仅在 Docker Compose 内部访问
- `8080`：后端由前端 Nginx 通过 `/api` 反向代理访问，生产环境通常不需要公网开放

`6080` 建议限制来源 IP，或至少使用强 `VNC_PASSWORD`。

## 2. 安装 Docker 和 Docker Compose

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

## 3. 克隆项目

```bash
git clone <你的仓库地址>
cd ai-content-marketing-system
```

## 4. 配置环境变量

```bash
cp .env.example .env
vim .env
```

至少修改以下值：

- `MYSQL_ROOT_PASSWORD`
- `MYSQL_PASSWORD`
- `DB_PASSWORD`：需与 `MYSQL_PASSWORD` 一致
- `JWT_SECRET`：使用足够长的随机字符串
- `DEEPSEEK_API_KEY`
- `EXA_API_KEY`
- `VNC_PASSWORD`

不要提交 `.env`。

## 5. 启动服务

```bash
docker compose up -d --build
```

查看状态：

```bash
docker compose ps
```

查看日志：

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql
```

## 6. 访问系统

前端访问：

```text
http://服务器公网IP
```

前端 Nginx 会把 `/api/` 代理到 `backend:8080`，SPA 页面刷新会回退到 `index.html`。

noVNC 访问：

```text
http://服务器公网IP:6080
```

输入 `.env` 中配置的 `VNC_PASSWORD`。noVNC 用于在后端容器内查看 Playwright Chromium 窗口，完成 CSDN、知乎首次登录并观察自动化发布过程。

## 7. CSDN / 知乎登录态准备

Docker 部署后，浏览器登录态必须保存在容器内路径，不能继续使用本机 Mac 路径。请在平台管理页面的 `auth_config` 中配置。

CSDN 示例：

```json
{
  "browserUserDataDir": "/app/browser-data/csdn",
  "editorUrl": "https://editor.csdn.net/md/?not_checkout=1",
  "manageUrl": "https://mp.csdn.net/mp_blog/manage/article",
  "defaultTags": ["Java", "前端", "编程语言"],
  "defaultCategory": "后端",
  "defaultColumn": "",
  "defaultSummary": "",
  "manualConfirm": false,
  "autoPublish": true
}
```

知乎示例：

```json
{
  "browserUserDataDir": "/app/browser-data/zhihu",
  "editorUrl": "https://zhuanlan.zhihu.com/write",
  "manageUrl": "https://www.zhihu.com/creator",
  "defaultTopics": ["科学", "生活", "互联网"],
  "defaultColumn": "",
  "manualConfirm": false,
  "autoPublish": true,
  "waitAfterFillMs": 1000
}
```

操作顺序：

1. 打开 noVNC。
2. 在容器浏览器中登录 CSDN / 知乎。
3. 平台账号 `auth_config` 使用 `/app/browser-data/csdn` 和 `/app/browser-data/zhihu`。
4. 登录成功后再执行发布任务。

## 8. 常用命令

重启后端：

```bash
docker compose restart backend
```

重启全部服务：

```bash
docker compose restart
```

停止服务：

```bash
docker compose down
```

重新构建：

```bash
docker compose up -d --build
```

清理容器但保留数据卷：

```bash
docker compose down --remove-orphans
```

查看 MySQL 数据卷：

```bash
docker volume ls | grep mysql
docker volume inspect ai-content-marketing-system_mysql-data
```

## 9. 常见问题

页面打不开：检查腾讯云安全组是否开放 `80`，并确认 `frontend` 容器正常。

接口 404：检查 `frontend/nginx.conf` 的 `/api/` 代理配置，以及后端是否在 `backend:8080` 正常启动。

后端连不上数据库：检查 `.env` 中 `DB_HOST=mysql`、`DB_PASSWORD` 和 `MYSQL_PASSWORD` 是否一致，查看 `docker compose logs -f mysql`。

Playwright 下载慢：Chromium 已在后端镜像构建阶段安装到 `/ms-playwright`，运行时不应再临时下载浏览器。

noVNC 打不开：检查安全组是否开放 `6080`，确认 `VNC_PASSWORD` 已配置，并查看 `docker compose logs -f backend`。

CSDN / 知乎提示登录：需要通过 noVNC 在容器浏览器内登录，且平台账号 `auth_config.browserUserDataDir` 必须使用 `/app/browser-data/csdn` 或 `/app/browser-data/zhihu`。

2G 内存浏览器卡顿：减少同时执行的发布任务，必要时升级服务器配置。
