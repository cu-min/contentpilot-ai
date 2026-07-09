# AI内容营销系统：平台发布策略参考文档

> 更新时间：2026-07-09。用途：记录平台账号配置、发布任务、发布器策略、真实发布试点结果和安全边界。

## 1. 核心结论

自动发布模块不能用一套逻辑硬套所有平台。不同平台在账号认证、内容格式、发布链路、链接获取、失败处理上差异很大，因此应采用“统一发布任务 + 平台发布器”的设计。

当前总体策略：

| 平台 | 推荐发布方式 | 当前状态 | 风险等级 |
|---|---|---|---|
| 微信公众号 | 官方 API 优先 | 已支持创建草稿、可选提交发布、刷新发布状态 | 中 |
| 知乎 | 浏览器自动化 | 已支持自动填充、发布设置处理、点击发布、创作中心确认 | 高 |
| CSDN | 浏览器自动化 | 已支持自动填充、发布弹窗字段处理、自动发布试点、结果检测 | 中高 |
| 掘金 | 非官方接口试点 + 浏览器兜底 | 已支持新建草稿、更新内容、提交发布、查询文章状态 | 中高 |

## 2. 总体设计原则

### 2.1 官方 API 优先

如果平台提供稳定、公开、合规的发布 API，应优先走官方 API。官方 API 更适合后端定时任务，也更容易记录发布结果和错误状态。

适用平台：微信公众号。

### 2.2 浏览器自动化兜底

如果平台没有稳定的公开发布 API，可以使用 Playwright 做浏览器自动化辅助发布。

边界：

- 不绕过验证码。
- 不绕过登录验证。
- 不规避平台风控。
- 不刷浏览量、点赞、评论。
- 遇到验证码、登录失效、页面结构变化时，直接失败或转人工确认。

适用平台：知乎、CSDN、掘金兜底。

### 2.3 非官方接口谨慎试点

部分平台可以通过浏览器抓包发现草稿或发布接口，但这类接口通常没有公开稳定文档，可能随时变化。

使用原则：

- 只能作为试点能力。
- 必须封装到独立 Publisher。
- 必须标记为 UNOFFICIAL_API。
- 失败后不要影响其他平台。
- 不在代码、文档、日志中写死 Cookie / Token。

适用平台：掘金。

### 2.4 人工确认兜底

如果平台发布链路不稳定，可以采用：

```text
系统打开发布页面
↓
自动填充标题、正文、标签等内容
↓
运营人员人工确认发布
```

这不是失败，而是更稳的自动化边界。

## 3. 推荐技术架构

### 3.1 发布器统一抽象

```java
public interface PlatformPublisher {
    PlatformType platform();
    PublishMode mode();
    PublishResult publish(PublishContext context);
}
```

### 3.2 发布方式枚举

```text
OFFICIAL_API
UNOFFICIAL_API
BROWSER_AUTOMATION
MANUAL_CONFIRM
```

### 3.3 发布结果状态

```text
SUCCESS
FAILED
NEED_LOGIN
NEED_CAPTCHA
NEED_MANUAL_CONFIRM
CONTENT_REJECTED
LINK_FETCH_FAILED
```

### 3.4 平台发布器建议

```text
WechatOfficialApiPublisher
ZhihuBrowserPublisher
CsdnBrowserPublisher
JuejinDraftApiPublisher
JuejinBrowserPublisher
```

## 4. 微信公众号发布策略

### 4.1 推荐方式

优先官方 API。

### 4.2 推荐链路

```text
获取 access_token
↓
上传封面素材
↓
上传正文图片素材，可选
↓
Markdown 转微信公众号兼容 HTML
↓
创建草稿
↓
提交发布，可后置
↓
查询发布状态
↓
获取文章链接
```

### 4.3 账号配置字段

- AppID
- AppSecret
- 默认封面 media_id，可选
- 作者，可选
- 是否启用评论，可选
- 是否仅创建草稿
- 备注

### 4.4 内容格式要求

- 公众号更适合 HTML，而不是原始 Markdown。
- 段落要短，适合移动端阅读。
- 标题要有吸引力，但不能夸张。
- 正文图片需要处理成微信素材或可用链接。
- 外部链接能力受公众号规则限制，当前 MVP 不处理追踪链接，后续数据归因阶段再统一设计链接策略。

### 4.5 常见失败原因

- access_token 过期。
- AppID / AppSecret 错误。
- IP 白名单未配置。
- 封面素材缺失。
- 内容审核失败。
- 认证主体权限不足。
- 发布接口异步，需要查询状态。

### 4.6 初版建议

第一步优先保证草稿创建和内容写入稳定；正式发布能力接入后必须保留失败原因记录和人工处理入口。

## 5. 知乎发布策略

### 5.1 推荐方式

Playwright 浏览器自动化，必要时人工确认。

### 5.2 推荐链路

```text
加载登录态
↓
打开知乎写文章页或编辑页
↓
填写标题
↓
填写正文
↓
处理草稿加载和 Markdown 解析提示
↓
滚动到底部并检测发布设置
↓
检查或补充文章话题
↓
点击发布并处理二次确认
↓
检测文章详情页、成功文案或创作中心标题匹配
```

### 5.3 账号配置字段

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

字段说明：

- `browserUserDataDir` 必填，用于复用 Chrome for Testing 登录态。
- `editorUrl` 缺省为 `https://zhuanlan.zhihu.com/write`。
- `manageUrl` 缺省为 `https://www.zhihu.com/creator`。
- `defaultTopics` 可选，页面无话题 chip 时尝试填 1-2 个。
- `manualConfirm=true` 时只填充并返回 `NEED_MANUAL_CONFIRM`。
- `autoPublish=true` 且 `manualConfirm=false` 时自动点击发布。
- `autoPublish` 缺省 false，避免误发布。

### 5.4 内容格式要求

- 更像问题回答、经验分享或观点分析。
- 减少硬广。
- 产品只能自然出现，不能强推。
- 标题适合“为什么 / 怎么做 / 是否值得”结构。
- 结尾可以给建议，但不要强销售。

### 5.5 常见失败原因

- 没有稳定公开发文 API。
- 登录态失效。
- 验证码。
- 页面结构变化。
- 内容审核或限流。
- 发布后链接读取不稳定。

### 5.6 初版建议

当前已经完成自动填充和自动发布试点。后续重点是根据实机日志持续增强 DOM 选择器稳定性，仍不实现知乎 HTTP 非官方接口。

## 6. CSDN 发布策略

### 6.1 推荐方式

浏览器自动化。

### 6.2 推荐链路

```text
加载登录态
↓
打开 CSDN Markdown 编辑器
↓
填写标题
↓
填写 Markdown 正文
↓
填写标签
↓
选择分类/专栏
↓
填写摘要
↓
点击发布并处理弹窗
↓
尝试获取文章链接
```

### 6.3 账号配置字段

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

### 6.4 内容格式要求

- Markdown 技术教程风格。
- 结构清晰，步骤明确。
- 可以保留代码块和配置示例。
- 标签、分类、摘要比较重要。
- 标题偏实用，适合搜索。

### 6.5 常见失败原因

- 登录态失效。
- 编辑器元素变化。
- CodeMirror / Markdown 编辑器填充复杂。
- 分类、标签字段变化。
- 发布按钮或弹窗变化。
- 内容审核。

### 6.6 初版建议

当前已经完成 CSDN 浏览器自动化发布试点。页面结构变化、验证码、登录失效或发布结果无法确认时，系统应返回明确失败状态或人工确认，不做自动重试。

## 7. 掘金发布策略

### 7.1 推荐方式

非官方草稿接口试点 + 浏览器自动化兜底。

### 7.2 推荐链路

当前非官方接口试点：

```text
读取 Cookie
↓
创建文章草稿
↓
更新草稿内容
↓
设置分类、标签、摘要
↓
提交发布
↓
查询文章状态或正式链接
```

浏览器兜底：

```text
打开掘金写文章页面
↓
填写标题
↓
填写 Markdown 正文
↓
选择分类和标签
↓
保存草稿或人工确认发布
```

### 7.3 账号配置字段

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

### 7.4 内容格式要求

- 开发者经验总结。
- 偏实践、踩坑、方法论。
- 语气比 CSDN 轻一点。
- 避免太官方的品牌宣传。
- 分类和标签重要。

### 7.5 常见失败原因

- 非官方接口变化。
- Cookie 过期。
- aid / uuid 等参数变化。
- 分类和标签 ID 维护成本。
- 内容审核。

### 7.6 初版建议

当前已经支持新建草稿、更新内容、提交发布和状态查询。由于该链路依赖非官方接口，后续维护时要把接口变化、Cookie 过期、分类和标签 ID 失效作为主要排查方向。

## 8. 发布任务状态建议

阶段 7 草稿流转使用：

```text
DRAFT
PENDING
CANCELLED
```

阶段 8 发布执行框架启用：

```text
RUNNING
SUCCESS
FAILED
```

真实平台发布阶段再按平台情况启用：

```text
NEED_LOGIN
NEED_CAPTCHA
NEED_MANUAL_CONFIRM
LINK_FETCH_FAILED
CONTENT_REJECTED
```

## 9. 当前验证记录

- 掘金：已验证发布任务可新建草稿、更新内容、提交发布，并同步审核中或正式链接状态。
- 微信公众号：已验证官方 API 草稿创建、可选提交发布和发布状态刷新链路。
- CSDN：已验证 Chrome for Testing 登录态复用、编辑器填充、发布弹窗必填项处理和结果检测。
- 知乎：已验证标题正文填充、草稿加载弹窗处理、Markdown 解析提示处理、发布设置检测、话题 chip 检测、自动点击发布和创作中心结果确认。

后续优化顺序建议：

1. 继续根据实机日志增强 CSDN / 知乎 DOM 选择器稳定性。
2. 增加发布结果人工校正入口。
3. 评估是否需要轻量发布记录导出。
4. 追踪链接、真实数据看板、自动失败重试仍放到后续扩展。

## 10. 安全边界

系统不实现：

- 验证码绕过。
- 登录绕过。
- 平台风控规避。
- 批量刷量。
- 自动点赞。
- 自动评论。
- 自动浏览刷量。
- 垃圾内容批量发布。

敏感信息要求：

- 后端存储。
- 前端脱敏展示。
- 日志不明文输出。
- Git 不提交 Cookie、Token、API Key。
