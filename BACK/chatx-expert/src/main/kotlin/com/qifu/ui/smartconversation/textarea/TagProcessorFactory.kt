package com.qifu.ui.smartconversation.textarea

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.qifu.ui.smartconversation.textarea.header.*
import com.qifu.ui.utils.CompletionRequestUtil
import com.qifu.utils.GitUtil
import com.qihoo.finance.lowcode.smartconversation.conversations.Conversation
import com.qihoo.finance.lowcode.smartconversation.conversations.ConversationsState
import com.qihoo.finance.lowcode.smartconversation.conversations.Message
import com.qihoo.finance.lowcode.smartconversation.utils.EncodingManager
import git4idea.GitCommit
import java.util.*

object TagProcessorFactory {

    fun getProcessor(project: Project, tagDetails: TagDetails): TagProcessor {
        return when (tagDetails) {
            is FileTagDetails -> FileTagProcessor(tagDetails)
            is SelectionTagDetails -> SelectionTagProcessor(tagDetails)
            is HistoryTagDetails -> ConversationTagProcessor(tagDetails)
            is DocumentationTagDetails -> DocumentationTagProcessor(tagDetails)
            is PersonaTagDetails -> PersonaTagProcessor(tagDetails)
            is FolderTagDetails -> FolderTagProcessor(tagDetails)
            is WebTagDetails -> WebTagProcessor()
            is GitCommitTagDetails -> GitCommitTagProcessor(project, tagDetails)
            is CurrentGitChangesTagDetails -> CurrentGitChangesTagProcessor(project)
            is EditorSelectionTagDetails -> EditorSelectionTagProcessor(tagDetails)
            is EditorTagDetails -> EditorTagProcessor(project, tagDetails)
            is ImageTagDetails -> ImageTagProcessor(tagDetails)
            is AgentTagDetails -> TagProcessor { _, _ -> }
            is EmptyTagDetails -> TagProcessor { _, _ -> }
            is CodeAnalyzeTagDetails -> TagProcessor { _, _ -> }
        }
    }
}

class FileTagProcessor(
    private val tagDetails: FileTagDetails,
) : TagProcessor {
    override fun process(message: Message, promptBuilder: StringBuilder) {
        if (message.referencedFilePaths == null) {
            message.referencedFilePaths = mutableListOf()
        }
        message.referencedFilePaths?.add(tagDetails.virtualFile.path)
    }
}

class EditorTagProcessor(
    private val project: Project,
    private val tagDetails: EditorTagDetails,
) : TagProcessor {

    override fun process(message: Message, promptBuilder: StringBuilder) {
        val filePath = tagDetails.virtualFile.path
        val projectBasePath = project.basePath ?: ""
        promptBuilder.append("\n**File:** `${com.qifu.utils.toRelativePath(filePath, projectBasePath)}`\n")
    }
}

class SelectionTagProcessor(
    private val tagDetails: SelectionTagDetails,
) : TagProcessor {

    override fun process(message: Message, promptBuilder: StringBuilder) {
        if (tagDetails.selectedText.isNullOrEmpty()) {
            return
        }

        promptBuilder.append(
            CompletionRequestUtil.formatCode(
                tagDetails.selectedText ?: "",
                tagDetails.virtualFile.path
            )
        )

        tagDetails.selectionModel.let {
            if (it.hasSelection()) {
                it.removeSelection()
            }
        }
    }
}

class EditorSelectionTagProcessor(
    private val tagDetails: EditorSelectionTagDetails,
) : TagProcessor {
    override fun process(message: Message, promptBuilder: StringBuilder) {
        invokeAndWaitIfNeeded {
            runReadAction {
                val selectionModel = tagDetails.selectionModel
                if (!selectionModel.hasSelection()) {
                    return@runReadAction
                }
                // 获取选中内容所在的完整行
                val document = tagDetails.selectionModel.editor.document
                // 使用 offset 方式获取行号（0-based），与 TagPanel.kt 保持一致
                val startLine = document.getLineNumber(selectionModel.selectionStart)
                val endLine = document.getLineNumber(selectionModel.selectionEnd)

                val startOffset = document.getLineStartOffset(startLine)
                val endOffset = document.getLineEndOffset(endLine)
                val fullLineText = document.getText(TextRange(startOffset, endOffset))
                val projectBasePath = selectionModel.editor.project?.basePath ?: ""
                promptBuilder.append(
                    CompletionRequestUtil.formatCodeBlock(
                        tagDetails.virtualFile.path,
                        projectBasePath,
                        fullLineText,
                        startLine + 1,  // 转换为 1-based 用于显示
                        endLine + 1,    // 转换为 1-based 用于显示
                    )
                )
            }
        }

        tagDetails.selectionModel.let {
            invokeLater {
                if (it.hasSelection()) {
                    it.removeSelection()
                }
            }
        }
    }
}

class DocumentationTagProcessor(
    private val tagDetails: DocumentationTagDetails,
) : TagProcessor {
    override fun process(message: Message, promptBuilder: StringBuilder) {
        message.documentationDetails = tagDetails.documentationDetails
    }
}

class PersonaTagProcessor(
    private val tagDetails: PersonaTagDetails,
) : TagProcessor {
    override fun process(message: Message, promptBuilder: StringBuilder) {
        message.personaName = tagDetails.personaDetails.name
    }
}

class FolderTagProcessor(
    private val tagDetails: FolderTagDetails,
) : TagProcessor {
    override fun process(
        message: Message,
        promptBuilder: StringBuilder
    ) {
        if (message.referencedFilePaths == null) {
            message.referencedFilePaths = mutableListOf()
        }

        processFolder(tagDetails.folder, message.referencedFilePaths ?: mutableListOf())
    }

    private fun processFolder(folder: VirtualFile, referencedFilePaths: MutableList<String>) {
        folder.children.forEach { child ->
            when {
                child.isDirectory -> processFolder(child, referencedFilePaths)
                else -> referencedFilePaths.add(child.path)
            }
        }
    }
}

class WebTagProcessor : TagProcessor {
    override fun process(
        message: Message,
        promptBuilder: StringBuilder
    ) {
        message.isWebSearchIncluded = true
    }
}

class GitCommitTagProcessor(
    private val project: Project,
    private val tagDetails: GitCommitTagDetails,
) : TagProcessor {
    override fun process(message: Message, promptBuilder: StringBuilder) {
        promptBuilder
            .append("\n```shell\n")
            .append(getDiffString(project, tagDetails.gitCommit))
            .append("\n```\n")
    }

    private fun getDiffString(project: Project, gitCommit: GitCommit): String {
        return ProgressManager.getInstance().runProcessWithProgressSynchronously<String, Exception>(
            {
                val repository = GitUtil.getProjectRepository(project)
                    ?: return@runProcessWithProgressSynchronously ""

                val commitId = gitCommit.id.asString()
                val diff = GitUtil.getCommitDiffs(project, repository, commitId)
                    .joinToString("\n")

                service<EncodingManager>().truncateText(diff, 8192, true)
            },
            "Getting Commit Diff",
            true,
            project
        )
    }
}

class CurrentGitChangesTagProcessor(
    private val project: Project,
) : TagProcessor {

    override fun process(
        message: Message,
        promptBuilder: StringBuilder
    ) {
        ProgressManager.getInstance().runProcessWithProgressSynchronously<Unit, Exception>(
            {
                GitUtil.getCurrentChanges(project)?.let {
                    promptBuilder
                        .append("\n```shell\n")
                        .append(it)
                        .append("\n```\n")
                }
            },
            "Getting Current Changes",
            true,
            project
        )
    }
}

class ImageTagProcessor(
    private val tagDetails: ImageTagDetails
) : TagProcessor {
    override fun process(message: Message, promptBuilder: StringBuilder) {
        message.imageFilePaths = listOf(tagDetails.imagePath)
    }
}

class ConversationTagProcessor(
    private val tagDetails: HistoryTagDetails
) : TagProcessor {

    companion object {
        fun getConversation(conversationId: UUID) =
            ConversationsState.getCurrentConversation()?.takeIf {
                it.id.equals(conversationId)
            } ?: ConversationsState.getInstance().conversations.find {
                it.id.equals(conversationId)
            }

        fun formatConversation(conversation: Conversation): String {
            val stringBuilder = StringBuilder()
            stringBuilder.append(
                "# History\n\n"
            )
//            stringBuilder.append(
//                "## Conversation: ${HistoryActionItem.getConversationTitle(conversation)}\n\n"
//            )

            conversation.messages.forEachIndexed { index, msg ->
                stringBuilder.append("**User**: ${msg.prompt}\n\n")
                stringBuilder.append("**Assistant**: ${msg.response}\n\n")
                stringBuilder.append("\n")
            }
            return stringBuilder.toString()
        }
    }

    override fun process(message: Message, stringBuilder: StringBuilder) {
        if (message.conversationsHistoryIds == null) {
            message.conversationsHistoryIds = mutableListOf()
        }
        message.conversationsHistoryIds?.add(tagDetails.conversationId)
    }
}