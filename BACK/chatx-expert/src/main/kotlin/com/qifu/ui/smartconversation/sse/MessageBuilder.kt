package com.qifu.ui.smartconversation.sse

import com.intellij.openapi.project.Project
import com.qifu.ui.smartconversation.textarea.TagProcessorFactory
import com.qifu.ui.smartconversation.textarea.header.TagDetails
import com.qihoo.finance.lowcode.smartconversation.conversations.Message
import com.qihoo.finance.lowcode.smartconversation.service.ReferencedFile
import java.util.*

class MessageBuilder(private val project: Project, private val text: String) {
    private val message = Message("")
    private var inlayContent: String = ""

    fun withInlays(appliedTags: List<TagDetails>): MessageBuilder {
        if (appliedTags.isNotEmpty()) {
            inlayContent = processTags(message, appliedTags)
        }
        return this
    }

    fun withReferencedFiles(referencedFiles: List<ReferencedFile>): MessageBuilder {
        if (referencedFiles.isNotEmpty()) {
            message.referencedFilePaths = referencedFiles.map { it.filePath() }
        }
        return this
    }

    fun withConversationHistoryIds(conversationHistoryIds: List<UUID>): MessageBuilder {
        if (conversationHistoryIds.isNotEmpty()) {
            message.conversationsHistoryIds = conversationHistoryIds
        }
        return this
    }

    fun withImage(attachedImagePaths: List<String>): MessageBuilder {
        message.imageFilePaths = attachedImagePaths
        return this
    }

    fun build(): Message {
        message.prompt = buildString {
            append(text)
            if (inlayContent.isNotBlank()) {
                append("\n")
                append(inlayContent)
            }
        }.trim()
        return message
    }

    private fun processTags(
        message: Message,
        tags: List<TagDetails>
    ): String = buildString {
        if (tags.isEmpty()) {
            return@buildString
        }
        append("\n\n")
        append("引用的代码上下文：\n")
        tags
            .map {
                TagProcessorFactory.getProcessor(project, it)
            }
            .forEach { it.process(message, this) }
    }
}
