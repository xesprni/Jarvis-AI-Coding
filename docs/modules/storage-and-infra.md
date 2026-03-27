# 存储与基础设施模块

这一层主要由 `config`、`utils`、`external`、`listener` 以及部分 `resources`/Gradle 配置组成，负责会话落盘、配置持久化、本地命令执行和插件基础集成。

## 1. 配置层

### 1.1 `JarvisCoreSettings`

应用级配置，主要保存：

- 默认聊天模型 ID
- 当前模型是否支持图片
- 默认 Skill 模板
- 默认 Rules 模板
- 当前聊天模式

这是聊天主界面的基础配置源。

### 1.2 `AgentSettings`

应用级配置，主要保存：

- shell 类型
- 被禁用的 skills 名单

Skill 面板的启用/禁用开关最终会落到这里。

### 1.3 `AutoApproveSettings`

应用级配置，主要保存：

- 自动审批是否启用
- 最大自动请求数
- 文件读取/编辑权限
- 命令执行权限
- MCP / Task / Skill 开关
- 命令黑名单

这是工具执行前授权策略的核心来源。

## 2. 本地路径约定

`utils/config.kt` 定义了几组关键目录：

| 方法 | 默认位置 | 作用 |
| --- | --- | --- |
| `getUserConfigDirectory()` | `~/.jarvis` | 用户级 AI 配置根目录 |
| `getProjectConfigDirectory()` | `${project}/.jarvis` | 项目级 AI 配置根目录 |
| `getChatDirectory()` | `~/.jarvis/intellij-chat-v2` | 会话数据根目录 |
| `getPlanDirectory()` | 会话目录下 | Plan 模式写计划的目录 |
| `getJarvisBinDirectory()` | 插件目录下 `bin` | 内置二进制目录 |

## 3. 会话持久化

### 3.1 `ConversationStore`

会话元数据管理器，负责：

- 获取所有会话
- 读取单个会话
- 更新 `conversation.json`
- 删除整个会话目录

### 3.2 `JsonLineChatHistory`

负责 UI 历史消息落盘，文件名为 `chat-history.jsonl`。它存的是结构化 `ChatHistoryMessage`，面向界面重建。

### 3.3 `JsonLineChatMemory`

负责给模型使用的上下文 memory，文件名为 `chat-memory-<agentId>.jsonl`。它保存的是 LangChain4j `ChatMessage`。

### 3.4 `TodoStorage`

按 `convId + agentId` 保存未完成 todo，文件名为 `todo-agent-<agentId>.json`。当所有 todo 都完成时，文件会被自动删除。

## 4. Checkpoint 与回滚

`CheckpointStorage` 是写操作安全链路中的关键基础设施，负责：

- 为某条用户消息初始化 checkpoint 目录
- 记录受影响文件的快照
- 记录会话上下文快照
- 恢复文件和会话上下文
- 清理旧 checkpoint

它支撑了 UI 中的回滚能力，也保障了编辑型工具的安全性。

## 5. Agent / Skill 本地资产加载

虽然 `AgentLoader` 和 `SkillLoader` 在 `utils` 包中，但它们实际上是“本地扩展能力仓库”的基础设施：

- Agent 来源：内置 + `~/.jarvis/agents` + `${project}/.jarvis/agents`
- Skill 来源：`~/.jarvis/skills` + `${project}/.jarvis/skills`
- 二者都带缓存和 watcher

### 设计价值

- 不需要改插件代码，就可以通过本地文件扩展能力
- 项目级配置可以覆盖或补充用户级配置

## 6. 外部工具与命令执行

### 6.1 `RipGrepUtil`

搜索能力基于内置 `rg` 包装实现，特点是：

- 启动时自动准备平台对应二进制
- 支持 glob / grep 两种高层接口
- 如果内置二进制不可用，会回退到系统 `rg`

### 6.2 `PersistentShell` 与 `CommandUtil`

这类工具封装了命令执行、shell 生命周期和平台兼容。它们是 `BashTool` 背后的基础设施。

### 6.3 其他常用基础能力

`utils` 下还包含：

- `GitUtil`
- `FileSearchUtil`
- `PsiFileUtils`
- `WindowsJobObject`
- `diff.kt`
- `retry.kt`
- `stream_parser.kt`

这些类大多是为了 IDE 环境、文件系统和平台兼容而存在。

## 7. Listener 与插件基础集成

### 7.1 `JarvisProjectStartupActivity`

项目启动时预热：

- `AgentService`
- `RipGrepUtil`

### 7.2 `JarvisAppLifeListener`

挂在应用生命周期上，适合放置更偏全局的初始化或清理逻辑。

### 7.3 `plugin.xml`

插件描述、依赖、工具窗和 action 全部由它统一注册，是 IntelliJ 平台集成的根入口。

## 8. 资源与构建

### 8.1 `src/main/resources`

当前资源目录主要承载：

- `META-INF/plugin.xml`
- `prompts/*`
- `templates/default-skill.md`
- `templates/default-rules.md`
- `messages/*`
- `icons` / `img`
- `rg/*`

### 8.2 `build.gradle.kts`

当前构建特征：

- Kotlin 2.2.0
- IntelliJ Platform Gradle Plugin 2.9.0
- IntelliJ Platform 2024.1.2
- Java/Kotlin 目标版本来自 `jvmSupport`
- 测试框架为 `kotlin.test`，并引入 `mockk`、`kotlinx-coroutines-test`

## 9. 设计思路

### 9.1 文件即配置

模型、智能体、技能、MCP、会话和待办都尽量以“可见文件”的形式存在，这比隐藏在数据库或远端接口中更容易调试和迁移。

### 9.2 基础设施与业务分离

`utils` 负责本地能力的可复用封装，`services` 和 `agent` 才负责业务编排。这样更容易替换底层实现。

### 9.3 变更可恢复

会话级 checkpoint 的存在，使插件具备“执行前拍快照，出问题能回退”的工程保障。

## 10. 维护建议

- 新增持久化内容前，先决定它属于应用级、用户级、项目级还是会话级。
- 新增命令执行能力时，优先复用已有 `CommandUtil` / `PersistentShell` 能力。
- 改会话存储格式时，要同步考虑历史兼容、回滚和 UI 重建。
