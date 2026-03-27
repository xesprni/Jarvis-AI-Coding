<!-- Plugin description -->

# Jarvis AI Core

Jarvis AI Core 是一个面向 IntelliJ IDEA 的本地 AI Agent 插件，聚焦于工具窗内的 Agent 对话式开发能力。当前版本保留了聊天、模型管理、MCP、智能体、Skills、Rules 与自动审批等核心功能，并移除了登录鉴权、远程内网接口、默认模型回退、MCP 市场、编辑器代码补全与低代码模块。

主要能力：

- Agent 模式多轮会话开发
- 本地模型管理与默认聊天模型选择
- MCP 配置、连接状态查看与工具接入
- 智能体、Skills、Rules 的本地管理
- 自动审批策略配置
- 支持 slash 命令、`@` 路径引用、顶部关联文件与代码选区上下文

<!-- Plugin description end -->

## 概览

本项目是一个 IntelliJ Platform 插件，主要提供右侧 `Jarvis` 工具窗能力。插件以 Kotlin 为主，兼容部分历史 Java 代码。当前定位不是“全家桶式 AI 平台”，而是一个更聚焦、更本地化的 Jarvis Core。

## 当前功能

### 1. Agent 会话式开发

- 在 IDE 右侧 `Jarvis` 工具窗中进行多轮 AI 对话。
- 支持欢迎页快捷提问、普通问答、工具调用、Ask/Approve 交互。
- 会话支持历史记录查看、重新打开和标题更新。

### 2. 增强输入框能力

- `slash` 命令：
  - 内建 `/clear`
  - 内建 `/compact`
  - Skills slash 命令
- `@` 引用：
  - 最近文件
  - 项目内文件
  - 项目内文件夹
- 顶部关联区：
  - 主动添加关联文件
  - 展示文件 tag / 代码 tag
  - 支持单个移除
- 代码选区：
  - 编辑器右键 `Send Selection To Jarvis`
  - 发送时自动附带绝对路径、1-based 行号范围和完整行内容
- 发送/停止状态：
  - 按钮状态显式区分
  - 停止按钮仅在 loop 中可用

### 3. 本地配置管理

顶部设置菜单按类型拆分为独立入口：

- 模型
- MCP
- 智能体
- Skills
- Rules
- 自动审批

设置展示为工具窗内单页 overlay，不再使用多 tab 对话框。

### 4. 自动审批

支持本地自动审批策略：

- 启用/关闭自动审批
- 最大自动批准数
- 文件读取/编辑权限
- 终端命令执行范围
- MCP / Task / Skill 开关
- 命令黑名单

### 5. MCP 集成

- 支持读取本地 MCP 配置
- 展示连接状态
- 将已连接 MCP 工具注入 Agent 工具集

## 交互入口

### 工具窗

- 右侧边栏 `Jarvis` 图标打开工具窗
- 顶部固定操作区包含：
  - 新会话
  - 历史会话
  - 设置

### 编辑器右键菜单

- `Send Selection To Jarvis`
  - 将当前编辑器选区作为代码上下文添加到聊天输入区顶部

## 构建与运行

### 环境要求

- JDK 17
- IntelliJ Platform 2024.1.2
- Gradle Wrapper

### 常用命令

```bash
./gradlew compileKotlin
./gradlew build -x test
./gradlew buildPlugin
./gradlew runIde
./gradlew test
```

### 定向测试

```bash
./gradlew test --tests com.miracle.ui.core.composer.ChatComposerSupportTest
```

## 项目结构

```text
src/
├── main/
│   ├── java/                      # 历史 Java 代码
│   ├── kotlin/com/miracle/
│   │   ├── agent/                # Agent、工具、MCP、流式解析
│   │   ├── config/               # 持久化配置与插件设置
│   │   ├── listener/             # 启动与生命周期监听
│   │   ├── services/             # 模型、技能、系统提示、辅助服务
│   │   ├── ui/
│   │   │   ├── core/             # Jarvis 工具窗与聊天主 UI
│   │   │   ├── settings/         # 模型/MCP/智能体/Skills/Rules/自动审批设置
│   │   │   └── smartconversation/# 历史 UI 模块与部分复用组件
│   │   └── utils/                # 通用工具与本地存储
│   └── resources/
│       ├── META-INF/plugin.xml
│       └── img/                  # 插件与工具窗图标
└── test/
    └── kotlin/                   # Kotlin 单元测试
```

## 本地数据与配置

当前版本中的部分本地配置/数据会落盘到用户目录，例如：

- `~/.jarvis/models.json`
- IntelliJ PersistentState XML
- 本地会话 history / memory 文件
- 本地 MCP 配置目录

具体读写位置可在 `config`、`utils` 与 MCP 相关实现中查看。

## 开发说明

- 推荐优先使用 Kotlin 编写新代码。
- UI 主要基于 Swing / IntelliJ UI DSL 风格组件。
- 修改聊天输入区相关能力时，优先关注：
  - `com.miracle.ui.core.JarvisChatTabPanel`
  - `com.miracle.ui.core.ChatPromptPanel`
  - `com.miracle.ui.core.ChatComposerPanel`
  - `com.miracle.ui.core.composer.*`
- 修改设置页时，优先关注：
  - `com.miracle.ui.core.JarvisSettingsOverlayPanel`
  - `com.miracle.ui.core.SettingsMenuPopupBuilder`
  - `com.miracle.ui.settings.*`


## 备注

- 插件内日志不要打印 `ERROR` 级别，否则可能触发 IDE 用户弹窗。
- `README.md` 中 `<!-- Plugin description -->` 到 `<!-- Plugin description end -->` 之间的内容会被用于插件描述生成。
