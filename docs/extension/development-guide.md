# 扩展开发指南

本文档面向需要在本项目中新增功能的开发者，重点说明从哪里接入、优先改哪些类、有哪些工程约束。

## 1. 开发基本原则

- 新代码优先使用 Kotlin。
- 不要打印 `ERROR` 级别日志，否则可能触发 IDE 用户弹窗。
- 先确认改动属于哪个层次：UI、Agent Runtime、Service、存储/基础设施。
- 尽量复用现有机制，不要为单点需求重新造一套状态管理或持久化方案。

## 2. 常见扩展场景

### 2.1 新增一个 Tool

### 推荐接入点

1. 在 `src/main/kotlin/com/miracle/agent/tool/` 下新增工具实现。
2. 参考已有工具实现输入校验、执行函数和 partial block 渲染。
3. 在 `ToolRegistry.autoRegisterTools()` 中注册，或按需加入 `getDynamicTools()`。
4. 如果工具只应在 `ASK` 或 `PLAN` 模式可用，需要同步修改 `ASK_TOOL_NAMES` / `PLAN_TOOL_NAMES`。
5. 为工具补单元测试，参考 `src/test/kotlin/com/miracle/agent/tool/*Test.kt`。

### 额外注意

- 如果工具会改文件，优先参考 `EditTool` / `WriteTool` 的实现方式。
- 变更型工具必须纳入授权与 checkpoint 体系，不要绕过 `ToolExecutor`。
- 如果工具适合流式预览，实现 `handlePartialBlock()`，这样 UI 才能在工具参数尚未完整时展示预期操作。

### 2.2 新增一个设置页 section

### 推荐接入点

1. 在 `src/main/kotlin/com/miracle/ui/settings/` 下新增对应 panel。
2. 在 `JarvisSettingsSection` 中新增枚举项。
3. 在 `JarvisSettingsOverlayPanel.createSectionComponent()` 中挂接新 panel。
4. 设置入口会自动出现在 `SettingsMenuPopupBuilder` 菜单中。

### 设计建议

- 设置页优先做成工具窗内 overlay，不要再开新的复杂对话框。
- 如需持久化，优先评估是否应写入 PersistentState，还是用户/项目 `.jarvis` 目录。

### 2.3 新增一个聊天入口或上下文注入方式

### 推荐接入点

- 编辑器动作：参考 `AddToJarvisChatAction`
- 工具窗外部注入：使用 `JarvisToolWindowService`
- 输入区展示：修改 `AssociatedContextState`、`AssociatedContextHeaderPanel`
- prompt 拼装：必要时修改 `ChatComposerSupport` 或 `Task` 的输入构造逻辑

### 典型例子

- 新增“发送当前文件到 Jarvis”
- 新增“附带 diff 片段到输入区”
- 新增新的上下文 tag 类型

### 2.4 新增 slash 命令

### 推荐接入点

1. 在 `SlashCommandRegistry` 中新增内建命令或 skill 命令转换规则。
2. 在 `JarvisChatTabPanel.handlePrimaryAction()` 或相关命令处理逻辑中补执行分支。
3. 如命令会改变会话状态，优先复用 `ConversationCommandService`。

### 当前内建命令

- `/clear`
- `/compact`

### 2.5 新增模型能力或模型提供商

### 推荐接入点

1. 扩展 `ModelProvider` 和 `ModelConfig`。
2. 修改 `loadModelConfigs()` / `getChatModel()` / `getStreamChatModel()`。
3. 检查 `ModelsListPanel` 是否需要新的字段展示和编辑项。
4. 保证 `JarvisCoreSettings.ensureValidSelectedModel()` 仍然成立。

### 当前约束

- 模型配置来源是 `~/.jarvis/models.json`
- 当前实现以 OpenAI Compatible 协议为主

### 2.6 扩展 Agent / Skill / Rules

### Agent

- 用户级目录：`~/.jarvis/agents`
- 项目级目录：`${project}/.jarvis/agents`
- 文件格式：Markdown + frontmatter
- 运行时加载：`AgentLoader`

### Skill

- 用户级目录：`~/.jarvis/skills`
- 项目级目录：`${project}/.jarvis/skills`
- 入口文件：`SKILL.md`
- 运行时加载：`SkillLoader`

### Rules

- 项目规则文件：`${project}/AGENTS.md`
- UI 入口：`RulesManagerPanel`

### 设计建议

- 如果能力只影响 prompt，不一定需要新写工具，优先考虑 Skill 或 Agent。
- 如果能力需要 UI 配置、授权和执行反馈，才考虑做 Tool。

### 2.7 扩展 MCP 能力

### 推荐接入点

1. 确认配置能被 `McpClientHub` 正确读到。
2. 检查 MCP server instructions 是否应进入 prompt。
3. 如需新增 UI 展示，修改 `ui/settings/mcp/*`。
4. 如需影响工具审批，注意 `McpPromptIntegration` 和自动审批逻辑。

### 关键点

- MCP 不只是连接状态问题，还涉及动态工具注入。
- 修改 MCP 行为时，要同时检查“配置文件 -> 连接状态 -> ToolRegistry -> PromptService”整条链路。

### 2.8 扩展会话级状态

如果你要新增一种“本轮或本会话都要记住”的状态，优先考虑以下位置：

- 请求级：放进 `Task` 或 `TaskState`
- 会话级：作为 `TaskState` 持有的 service
- 长期持久化：放到 `config` 或 `utils` 存储层

不要直接把这类状态塞进 Swing 组件字段里，否则会让恢复、回滚和多 tab 行为变得混乱。

## 3. 推荐的开发路径

### 3.1 改 UI 之前

- 先确认对应能力有没有现成 service 或 storage。
- 确认状态最终由谁持有，不要先写界面再补状态模型。

### 3.2 改 Agent Runtime 之前

- 先确认是 prompt 问题、工具问题，还是事件渲染问题。
- 尤其不要把授权、checkpoint、文件新鲜度绕开。

### 3.3 改持久化之前

- 先确认数据应该按应用、用户、项目、会话哪个粒度保存。
- 再决定使用 PersistentState、`.jarvis` 文件还是会话目录。

## 4. 测试建议

项目当前测试基础已经覆盖了不少关键点，建议遵循现有模式：

- 单元测试目录：`src/test/kotlin`
- 基础断言：`kotlin.test.*`
- 协程测试：`kotlinx-coroutines-test`
- mock：`mockk`

### 常用命令

```bash
./gradlew test
./gradlew test --tests com.miracle.agent.tool.ToolRegistryTest
./gradlew build -x test
./gradlew runIde
```

### 推荐补测的场景

- 新工具的参数校验和执行结果
- 会话命令对 memory/history 的影响
- 新增 settings 面板后的持久化读写
- 新增存储结构后的兼容与恢复

## 5. 修改时最容易遗漏的点

- 只改了 UI，没改真正的执行链路
- 新增工具后忘了注册到 `ToolRegistry`
- 改了配置文件格式，没同步 UI 和默认值
- 改了文件写入逻辑，没同步 checkpoint/回滚
- 新增日志用了 `ERROR` 级别

## 6. 建议的落地顺序

1. 先明确需求属于哪个模块。
2. 先改状态和执行链路，再补 UI。
3. 最后补测试、文档和必要的资源文件。

如果改动覆盖了工具、会话存储和 UI 三层，建议先以 [架构总览](../architecture/overview.md) 和 [核心流程](../architecture/core-flows.md) 为基准自查一遍，确保没有破坏主链路。
