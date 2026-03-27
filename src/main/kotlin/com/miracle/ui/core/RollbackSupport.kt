package com.miracle.ui.core

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.miracle.agent.Task
import com.miracle.utils.CheckpointStorage
import com.miracle.utils.CheckpointFileChangeSummary

/**
 * Encapsulates the checkpoint-based rollback logic for undoing file changes
 * produced by a specific user message within a conversation.
 */
internal class RollbackSupport(
    private val project: Project,
) {

    private val log = Logger.getInstance(RollbackSupport::class.java)

    /**
     * Returns `true` if there are recorded file changes for the given
     * conversation + message that can be rolled back.
     */
    fun canRollback(conversationId: String?, messageId: String?): Boolean {
        val convId = conversationId ?: return false
        val safeMessageId = messageId ?: return false
        return CheckpointStorage.hasRollbackCheckpoint(project, convId, safeMessageId)
    }

    fun getPendingFileChanges(conversationId: String?, messageId: String?): List<CheckpointFileChangeSummary> {
        val convId = conversationId ?: return emptyList()
        val safeMessageId = messageId ?: return emptyList()
        return CheckpointStorage.getPendingFileChangeSummaries(project, convId, safeMessageId)
    }

    /**
     * Prompts the user and, if confirmed, restores files and conversation
     * context to the state before [messageId].
     *
     * @param conversationId current conversation id
     * @param messageId      the user message whose changes to revert
     * @param activeTask     current [Task], must be null to allow rollback
     * @param onComplete     called on the EDT after a successful rollback
     */
    fun rollback(
        conversationId: String?,
        messageId: String,
        activeTask: Task?,
        onComplete: () -> Unit,
    ) {
        if (!ensureTaskStopped(activeTask)) {
            return
        }

        val convId = conversationId ?: return
        val changedFiles = CheckpointStorage.getChangedFiles(project, convId, messageId)
        if (changedFiles.isEmpty()) {
            Messages.showInfoMessage(project, "\u5F53\u524D\u6D88\u606F\u6CA1\u6709\u53EF\u56DE\u9000\u7684\u6587\u4EF6\u6539\u52A8\u3002", "\u56DE\u9000\u672C\u6B21\u6539\u52A8")
            return
        }

        val result = Messages.showYesNoDialog(
            project,
            "\u5C06\u56DE\u9000\u672C\u8F6E\u6539\u52A8\u5E76\u6062\u590D ${changedFiles.size} \u4E2A\u6587\u4EF6\u4E0E\u4F1A\u8BDD\u4E0A\u4E0B\u6587\uFF0C\u662F\u5426\u7EE7\u7EED\uFF1F",
            "\u56DE\u9000\u672C\u6B21\u6539\u52A8",
            Messages.getQuestionIcon(),
        )
        if (result != Messages.YES) return

        ApplicationManager.getApplication().executeOnPooledThread {
            val restoreError = runCatching {
                CheckpointStorage.restoreCheckpointAndContext(project, convId, messageId)
                CheckpointStorage.clearCheckpointsFromMessage(project, convId, messageId)
            }.exceptionOrNull()

            ApplicationManager.getApplication().invokeLater {
                if (restoreError != null) {
                    Messages.showWarningDialog(
                        project,
                        "\u56DE\u9000\u5931\u8D25\uFF1A${restoreError.message ?: "\u672A\u77E5\u9519\u8BEF"}",
                        "\u56DE\u9000\u672C\u6B21\u6539\u52A8",
                    )
                    return@invokeLater
                }
                onComplete()
            }
        }
    }

    fun restoreFile(
        conversationId: String?,
        messageId: String,
        filePath: String,
        activeTask: Task?,
        onComplete: () -> Unit,
    ) {
        if (!ensureTaskStopped(activeTask)) {
            return
        }

        val convId = conversationId ?: return
        ApplicationManager.getApplication().executeOnPooledThread {
            val restoreError = runCatching {
                CheckpointStorage.restoreSingleFile(project, convId, messageId, filePath)
            }.exceptionOrNull()

            ApplicationManager.getApplication().invokeLater {
                if (restoreError != null) {
                    log.warn("Failed to restore file checkpoint for $filePath", restoreError)
                    Messages.showWarningDialog(
                        project,
                        "\u5355\u6587\u4EF6\u8FD8\u539F\u5931\u8D25\uFF1A${restoreError.message ?: "\u672A\u77E5\u9519\u8BEF"}",
                        "\u56DE\u9000\u672C\u6B21\u6539\u52A8",
                    )
                    return@invokeLater
                }
                onComplete()
            }
        }
    }

    private fun ensureTaskStopped(activeTask: Task?): Boolean {
        if (activeTask == null) {
            return true
        }
        Messages.showInfoMessage(project, "\u8BF7\u5148\u505C\u6B62\u5F53\u524D\u4EFB\u52A1\u540E\u518D\u6267\u884C\u56DE\u9000\u3002", "\u56DE\u9000\u672C\u6B21\u6539\u52A8")
        return false
    }
}
