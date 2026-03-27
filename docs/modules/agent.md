# Agent 模块

`agent` 目录是插件的执行引擎，负责把“聊天请求”变成“模型调用 + 工具调用 + UI 事件流”。

## 1. 目录结构

| 子目录 | 作用 |
| --- | --- |
| `agent/agent.kt` | `Task`、`TaskState`、主循环 |
| `agent/ChatResponseHandler.kt` | 流式响应处理 |
| `agent/parser/*` | SSE 文本、完整消息、segment 解析 |
| `agent/tool/*` | 工具协议、内置工具、执行器、注册表 |
| `agent/mcp/*` | MCP 客户端、动态工具、MCP prompt 集成 |

## 2. 主体职责

### 2.1 `Task`

`Task` 表示一次“会话轮次”的执行上下文，负责：

- 建立或恢复会话元数据
- 把用户输入、文件、图片写入 memory
- 选择模型、模式和工具集合
- 循环请求模型，直到得到最终文本结果或完成工具调用链

`Task.startTaskLoop()` 是整个 Agent 模块最核心的方法。

### 2.2 `TaskState`

`TaskState` 是运行时共享状态容器，主要包含：

- `chatMemory` / `chatHistory`
- `tools`
- `modelId` / `chatMode`
- `askFutures` / `toolCallFutures`
- `systemReminderService` / `fileFreshnessService`
- `shell`
- `emit`

它把“请求级状态”和“会话级缓存服务”放在同一个对象里，方便工具和 UI 回调共享。

## 3. 执行模型

### 3.1 请求前准备

`Task` 初始化时会：

- 将引用文件内容包进 `<file_content path="...">...</file_content>`
- 为每个读取过的文件记录 freshness
- 把用户文本、文件、图片统一塞进 `UserMessage`
- 对主会话写入 `ChatHistoryUserMessage`

### 3.2 主循环

主循环大致分为以下阶段：

1. 根据模式刷新工具规格
2. 获取 system prompt
3. 调用模型并等待完整 `AiMessage`
4. 如果有 tool requests，则逐个执行
5. 如果没有工具，则把本轮视为结束

这种设计是“模型驱动的工具循环”，而不是 UI 直接调用工具。

## 4. 流式响应处理

`ChatResponseHandler` 负责把模型输出翻译为前端可消费的事件：

- `onPartialResponse()`：处理普通文本流
- `onPartialToolCall()`：处理工具参数流
- `onCompleteResponse()`：收尾、写入 token usage
- `onError()`：结束流并回传异常

这里有两个重要细节：

- 文本渲染有节流，避免 UI 高频刷新
- partial tool call 只用于 UI 预览，不会提前写入 tool result

## 5. 解析器设计

`agent/parser` 的作用不是把模型输出原样展示，而是把它转成结构化 segment。这样上层 UI 可以按类型渲染：

- `TextSegment`
- `Code`
- `SearchReplace`
- `ToolSegment`
- `ErrorSegment`

这也是为什么聊天卡片可以在一条消息中混排普通文本、代码块、工具展示。

## 6. 工具体系

### 6.1 `ToolRegistry`

`ToolRegistry` 统一管理三类工具：

- 内置静态工具：如 `Bash`、`Read`、`Edit`、`Write`、`TodoWrite`
- 动态 IDE 工具：如 `ReadClass`、`ResolveClassName`、`ListImplementations`
- MCP 工具：由 `McpClientHub.instantiate()` 动态生成

同时它还按模式裁剪工具集：

- `getAll()`
- `getAskTools()`
- `getPlanTools()`

### 6.2 `ToolExecutor`

执行器负责把一个 `ToolExecutionRequest` 安全地落地执行。流程包括：

- 校验工具是否存在
- 尝试修复非法 JSON 参数
- 调用工具级参数校验
- Ask/AutoApprove 授权
- 记录 checkpoint
- 反射调用工具执行函数
- 把 `ToolExecutionResultMessage` 回写到 memory

### 6.3 内置工具大类

从当前代码看，工具大体分为这些类别：

- 搜索读取：`Glob`、`Grep`、`Read`、`ReadClass`、`ResolveClassName`、`ListImplementations`
- 代码变更：`Edit`、`Write`、`TodoWrite`
- 执行与代理：`Bash`、`Task`、`Skill`
- 交互控制：`AskUserQuestion`、`EnterPlanMode`、`ExitPlanMode`
- 特殊输入：`ExcelRead`

扩展新工具时，优先参考已有 `Read`、`Edit`、`Write`、`Task` 这几类实现。

## 7. MCP 子系统

### 7.1 `McpClientHub`

它是项目级 MCP 连接中心，负责：

- 读取启用的 MCP 配置
- 建立/关闭连接
- 维护连接状态和错误信息
- 监听配置文件变化
- 对外提供已连接的 client 和工具定义

### 7.2 `McpPromptIntegration`

MCP 不只是动态工具。系统还会把服务端提供的 instructions 拼进 system prompt，让模型知道这些工具应如何使用。

### 7.3 工具注入方式

已连接 MCP server 的 tool definitions 会被封装成 `McpServerToolInstance`，并在 `ToolRegistry.getMcpTools()` 阶段动态并入总工具表。

## 8. Agent 模块的设计思路

### 8.1 UI 无感知工具执行细节

UI 只关心 `AgentMessage` 事件，不关心模型 SDK、参数修复、授权逻辑和反射执行细节。这让 UI 层可以保持相对稳定。

### 8.2 执行安全优先于吞吐

`ToolExecutor` 在真正执行前做了很多前置步骤。它会牺牲一些吞吐和简洁性，但能保证：

- 变更型操作可回滚
- 参数错误不会直接打崩循环
- 用户拥有最终授权权

### 8.3 运行时能力可热插拔

MCP 工具、Agent、Skill 都不是写死在编译期的。Agent 模块天然支持“本地文件改一下，运行态就看到新能力”。

## 9. 维护建议

- 改 `Task` 前，先明确是“请求级状态”还是“会话级状态”。
- 改工具协议前，注意 `ToolRegistry`、`ToolExecutor`、UI 工具卡片三方是否同步。
- 改 MCP 逻辑时，同时检查“动态工具注入”和“prompt 注入”两条链路。
