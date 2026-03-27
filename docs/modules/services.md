# Services 模块

`services` 目录承载了 Agent 运行时依赖的“中层服务”。这些类大多不直接负责 UI，也不直接负责持久化，而是负责提示词、模型、提醒、上下文和加载器装配。

## 1. 模块定位

从职责上看，`services` 是 UI 和 Agent Runtime 之间的业务中间层，主要解决以下问题：

- 如何组装 system prompt
- 如何读取本地模型配置并创建模型实例
- 如何在会话中注入系统提醒
- 如何跟踪文件读取/变更状态
- 如何在项目启动后加载 Agent 和 Skill

## 2. 关键服务

### 2.1 `AgentService`

这是一个项目级 service，主要负责：

- 持有 `AgentLoader`
- 持有 `SkillLoader`
- 在项目索引完成后加载现有 Agent/Skill
- 维持 watcher 生命周期

它本身逻辑不复杂，但它是整个“本地可扩展能力”体系的启动入口。

### 2.2 `PromptService`

这是最关键的服务之一，负责：

- 用运行环境变量替换 prompt 模板中的 `{{CWD}}`、`{{DATE}}` 等占位符
- 组装主 system prompt
- 在 `PLAN` 模式下注入额外约束
- 注入 MCP server instructions
- 生成会话标题判定 prompt
- 生成 `Task` 工具描述

### 设计特点

- Prompt 不是静态常量，而是与项目、模型、模式绑定的动态结果。
- Prompt 里显式纳入了项目规则文档、MCP 提示、计划模式限制、工具策略等上下文。

### 2.3 `model.kt`

模型相关能力目前是“OpenAI Compatible” 优先的本地配置模式，核心职责包括：

- 定义 `ModelConfig`
- 从 `~/.jarvis/models.json` 读取模型列表
- 构建 `OpenAiChatModel` / `OpenAiStreamingChatModel`
- 维护当前选中模型
- 支持增删改模型配置

### 当前设计特点

- provider 目前只有 `OPENAI_COMPATIBLE`
- 严格开启 `strictTools` 和 `strictJsonSchema`
- 某些模型别名会走定制参数分支

### 2.4 `context.kt`

这里专门负责读取项目规则文档：

- 根目录 `AGENTS.md`
- 根目录 `JARVIS.md`

读取后的内容会作为“项目级指令”进入提示词体系。

### 2.5 `SystemReminderService`

这是会话内的系统提醒注入器。它不是一次性 prompt，而是动态提醒流，负责：

- todo 变更提醒
- mention 类提醒
- Plan 模式提醒
- 事件派发与去重

### 设计价值

- 把“提醒”作为增量上下文处理，而不是每轮重建完整巨型 prompt
- 能根据用户行为和会话状态动态改变模型感知

### 2.6 `FileFreshnessService`

文件新鲜度服务用于跟踪：

- 某个文件何时被读取
- 文件是否被外部修改
- 本轮会话触及了哪些文件
- 哪些文件适合作为上下文恢复对象

### 典型用途

- 在模型准备编辑前检查文件是否已过期
- 在会话压缩或恢复时挑选重要文件
- 给系统提醒注入“文件已在外部变化”的提示

## 3. `services` 内的设计思路

### 3.1 把“提示词系统”独立成服务

Prompt 在该项目中不是一个简单字符串，而是一种运行时组合结果。把它放在 `PromptService` 能避免 UI、Task、工具层各自拼接 prompt。

### 3.2 把“模型配置”视为本地资产

模型管理没有走远端配置中心，而是直接把 `models.json` 当作本地资产文件处理。这与整个项目的“本地优先”思路一致。

### 3.3 会话中台化

`SystemReminderService` 和 `FileFreshnessService` 本质上都是会话中台服务：

- 一个管系统状态提醒
- 一个管文件状态感知

它们不属于 UI，也不应该散落在工具实现中。

## 4. 与其他模块的关系

| 调用方 | 调用方式 | 说明 |
| --- | --- | --- |
| `Task` | 调用 `PromptService`、`model.kt` | 发起真正的模型请求 |
| `TaskState` | 持有 `SystemReminderService`、`FileFreshnessService` | 管理会话级上下文 |
| `JarvisChatTabPanel` | 通过模型与模式选择影响服务行为 | UI 只做参数输入 |
| `AgentService` | 被 `JarvisProjectStartupActivity` 预加载 | 保证 watcher 及时启动 |

## 5. 维护建议

- 改 prompt 前，先区分是“静态约束”还是“会话内动态提醒”。
- 改模型层时，注意同时检查模型列表持久化、UI 展示和默认模型校验。
- 新增会话级状态时，优先考虑放入 `TaskState` 持有的 service，而不是直接塞进 UI。
