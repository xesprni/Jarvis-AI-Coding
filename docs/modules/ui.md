# UI 模块

UI 模块主要位于 `ui/core`、`ui/settings` 和少量 `ui/smartconversation` 目录中，负责把 Agent 运行时的状态组织成 IntelliJ 工具窗交互。

## 1. 分层概览

| 目录 | 职责 |
| --- | --- |
| `ui/core` | 工具窗主体、聊天 tab、输入区、消息渲染、历史弹层 |
| `ui/settings` | 工具窗内 overlay 设置页 |
| `ui/smartconversation` | 目前主要保留 `ChatMode`、协程作用域和少量复用组件 |

## 2. 主链路组件

| 类 | 作用 |
| --- | --- |
| `JarvisToolWindowFactory` | 创建工具窗内容并绑定 service |
| `JarvisToolWindowService` | 工具窗外部入口桥接，缓存待注入上下文 |
| `JarvisToolWindowPanel` | 主视图和设置 overlay 的卡片切换容器 |
| `JarvisToolWindowTabbedPane` | 管理多个会话 tab |
| `JarvisChatTabPanel` | 单个聊天 tab 的控制器 |
| `ChatPromptPanel` / `ChatComposerField` | 输入区、模型选择、模式选择、按钮区 |
| `AssociatedContextState` / `AssociatedContextHeaderPanel` | 顶部关联文件和代码选区状态 |
| `AssistantMessageCard` / `SegmentRendererFactory` | assistant 消息卡片和 segment 渲染 |
| `HistoryPopupBuilder` | 会话历史列表 |
| `RollbackSupport` | 变更回滚能力 |

## 3. `JarvisToolWindowService` 的价值

这不是一个简单的“show tool window” 工具类，而是一个很重要的缓冲层：

- 接收来自编辑器动作、设置入口或其他服务的输入
- 当工具窗还未构建时，先把内容缓存下来
- 待 panel 绑定后再统一注入当前激活 tab

这保证了像“右键发送选区到 Jarvis”这样的动作不依赖工具窗是否已打开。

## 4. 单个聊天 tab 的 UI 结构

`JarvisChatTabPanel` 由三大区域组成：

- 顶部会话信息区：标题、模型、模式
- 中间内容区：欢迎页或消息滚动区
- 底部输入区：关联上下文 + composer + Ask 面板

### 4.1 输入区

输入区支持：

- 普通文本输入
- slash 命令
- `@` 路径引用
- 顶部关联文件
- 顶部关联代码选区
- 发送/停止状态切换

### 4.2 Ask 面板

Ask 面板被统一用于两类交互：

- 工具执行审批
- `AskUserQuestion` 主动追问

因此它是聊天循环的一部分，而不是一个单独的弹窗。

## 5. 消息渲染设计

消息不是按纯文本渲染，而是按 segment 渲染。这样可以对不同内容采用不同展示方式：

- 文本
- 代码块
- 搜索替换预览
- 工具卡片
- 错误块

`partialCards` 的存在让 UI 能够流式更新某条 assistant 消息，而不用每次新建一张卡片。

## 6. 关联上下文设计

顶部关联上下文采用显式可视化设计，而不是把所有引用偷偷塞进 prompt：

- 用户可以看到当前附带了哪些文件/代码
- 可以逐个删除
- 代码选区会保留文件路径和 1-based 行号

这个设计明显降低了“上下文带了什么”这一类可解释性问题。

## 7. 编辑器到工具窗的交互

`AddToJarvisChatAction` 注册在 `EditorPopupMenu` 上，行为如下：

1. 只有在选区非空时才显示
2. 获取选区所在文件、起止行号和完整行内容
3. 通过 `JarvisToolWindowService.appendAssociatedCodeSelection()` 注入到工具窗

这说明编辑器和工具窗之间是弱耦合的，靠 service 做消息中转。

## 8. 设置页设计

设置页没有做成单独对话框，而是作为工具窗内部的 overlay 页面存在：

- `JarvisSettingsSection` 定义所有 section
- `SettingsMenuPopupBuilder` 负责展示设置菜单
- `JarvisSettingsOverlayPanel` 负责 section 切换和组件缓存

目前的 section 包括：

- 模型
- MCP
- 智能体
- Skills
- Rules
- 自动审批

### 设计收益

- 用户不需要离开聊天上下文
- 设置页和聊天页共享同一个工具窗生命周期
- 设置修改后更容易直接回到当前会话继续验证

## 9. `ui/settings` 各子模块定位

| 子模块 | 说明 |
| --- | --- |
| `models` | 维护 `models.json`，增删改本地模型 |
| `mcp` | 展示 MCP 连接状态与配置入口 |
| `agent` | 展示内置/用户/项目智能体，并支持编辑 |
| `skills` | 展示 Skill 卡片、启用/禁用、定位源码、AI 生成 |
| `rules` | 直接打开项目 `AGENTS.md` |
| `autoapprove` | 管理工具自动审批范围 |

## 10. `ui/smartconversation` 的现状

当前这个目录已经不是主 UI 实现承载区，只保留了少量仍被复用的基础设施：

- `ChatMode`
- `SmartCoroutineScope`
- `DiffWindowHolder`

也就是说，新功能优先加到 `ui/core` 或 `ui/settings`，不要继续把主逻辑堆回旧目录。

## 11. UI 层的设计思路

### 11.1 一个 tab 对应一个活跃会话视图

这样历史打开、标题更新、回滚、Ask 状态都可以围绕 tab 内局部状态处理。

### 11.2 工具窗内闭环

从聊天到设置再回聊天，都在同一个工具窗内完成，减少上下文切换成本。

### 11.3 渲染优先于抽象

当前 UI 代码带有比较强的 Swing 组件拼装风格。它的优点是：

- 局部修改快
- 直观
- 与 IntelliJ 组件系统兼容好

代价是组件类较多，布局和行为耦合也比较明显。

## 12. 维护建议

- 加新设置项时，不要跳过 `JarvisSettingsSection` 和 `SettingsMenuPopupBuilder`。
- 改输入区行为时，注意同时检查 `ChatComposerField`、`ChatComposerSupport`、`SlashCommandRegistry` 和 `AssociatedContextState`。
- 改消息渲染时，先确认 segment 类型是否足够表达，而不是直接往文本里拼 UI 标记。

