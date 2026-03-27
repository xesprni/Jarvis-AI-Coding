package com.qifu.ui.smartconversation.panels

import com.intellij.openapi.project.Project
import com.qifu.agent.parser.ToolHeader
import com.qihoo.finance.lowcode.common.util.Icons
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull


/**
 * @author weiyichao
 * @date 2025-10-03
 **/
class FileReadPanel(project: Project) : ToolPanel(project) {
    /** 添加一个可展开的结果分组 */
    fun addResultGroup(title: String, basePath: String) {
        val toggleAction: () -> Unit = {
            openFileInEditor(title, basePath)
        }
        addTitleActionListener { toggleAction() }
    }

    override fun setContent(
        filePath: String,
        content: String,
        params: Map<String, JsonElement>,
        toolHeader: ToolHeader,
        isPartial: Boolean
    ) {
        // 更新描述区域状态
        setTipsContent(toolHeader)

        setTitleIcon(Icons.AI_OPEN)

        val currentTitle = filePath.substringAfterLast("/")
        val filePrefix = filePath.substringBeforeLast("/")

        // 处理 offset 和 limit 参数
        val start = (params["start"] as JsonPrimitive).intOrNull ?: 0
        val end = (params["end"] as JsonPrimitive).intOrNull ?: 0

        // 构建带行数信息的标题
        val titleWithLineInfo = "$currentTitle (行: ${start}-$end)"
        setTitleContent(titleWithLineInfo)
        addResultGroup(currentTitle, filePrefix)
        contentPanel.revalidate()
        contentPanel.repaint()
    }

}