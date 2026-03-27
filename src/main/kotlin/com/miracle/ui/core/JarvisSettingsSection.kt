package com.miracle.ui.core

import com.intellij.icons.AllIcons
import javax.swing.Icon

internal enum class JarvisSettingsSection(
    val title: String,
    val description: String,
    val icon: Icon,
) {
    MODELS(
        title = "模型",
        description = "管理本地模型与默认聊天模型选择。",
        icon = AllIcons.Nodes.Plugin,
    ),
    MCP(
        title = "MCP",
        description = "查看 MCP 服务状态并维护本地 MCP 配置。",
        icon = AllIcons.Nodes.PpLib,
    ),
    AGENT(
        title = "智能体",
        description = "管理智能体配置与执行能力。",
        icon = AllIcons.Nodes.Class,
    ),
    SKILLS(
        title = "Skills",
        description = "启用、禁用并查看本地 Skills 能力。",
        icon = AllIcons.Nodes.Tag,
    ),
    RULES(
        title = "Rules",
        description = "维护会话提示词与项目规则。",
        icon = AllIcons.Actions.EditScheme,
    ),
    AUTO_APPROVE(
        title = "自动审批",
        description = "配置工具自动审批范围、限额与命令黑名单。",
        icon = AllIcons.Actions.Checked,
    ),
}
