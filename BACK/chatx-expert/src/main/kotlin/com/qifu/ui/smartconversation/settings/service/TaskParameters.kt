package com.qifu.ui.smartconversation.settings.service

import com.qifu.ui.smartconversation.psistructure.ClassStructure
import com.qifu.ui.smartconversation.settings.configuration.ChatMode
import com.qifu.utils.image.ImageUtil
import com.qihoo.finance.lowcode.smartconversation.conversations.Conversation
import com.qihoo.finance.lowcode.smartconversation.conversations.Message
import com.qihoo.finance.lowcode.smartconversation.service.ReferencedFile

interface CompletionParameters

class TaskCompletionParameters private constructor(
    val taskId: String,
    val message: Message,
    var retry: Boolean,
    var imageDetailsList: List<String>?,
    var history: List<Conversation>?,
    var referencedFiles: List<ReferencedFile>?,
    var psiStructure: Set<ClassStructure>?,
    var chatMode: ChatMode = ChatMode.AGENT,
    var modelId: String = "",
    var hasCustomInput : Boolean = false,
) : CompletionParameters {

    fun toBuilder(): Builder {
        return Builder(taskId, message).apply {
            retry(this@TaskCompletionParameters.retry)
            imageDetailsList(this@TaskCompletionParameters.imageDetailsList)
            referencedFiles(this@TaskCompletionParameters.referencedFiles)
            psiStructure(this@TaskCompletionParameters.psiStructure)
            chatMode(this@TaskCompletionParameters.chatMode)
            modelId(this@TaskCompletionParameters.modelId)
        }
    }

    class Builder(private val taskId: String, private val message: Message) {
        private var retry: Boolean = false
        private var imageDetailsList: List<String>? = null
        private var history: List<Conversation>? = null
        private var referencedFiles: List<ReferencedFile>? = null
        private var psiStructure: Set<ClassStructure>? = null
        private var gitDiff: String = ""
        private var chatMode: ChatMode = ChatMode.AGENT
        private var modelId: String = ""
        private var hasCustomInput: Boolean = false

        fun retry(retry: Boolean) = apply { this.retry = retry }

        fun imageDetailsList(imageDetailsList: List<String>?) = apply { this.imageDetailsList = imageDetailsList }

        fun imageDetailsFromPaths(paths: List<String>?, maxWidth: Int = 1024, quality: Float = 0.8f) = apply {
            this.imageDetailsList = paths?.mapNotNull { path ->
                if (path.isNotEmpty()) ImageUtil.pathToImageDetails(path, maxWidth, quality) else null
            }
        }

        fun gitDiff(gitDiff: String) = apply { this.gitDiff = gitDiff }

        fun history(history: List<Conversation>?) = apply { this.history = history }

        fun referencedFiles(referencedFiles: List<ReferencedFile>?) =
            apply { this.referencedFiles = referencedFiles }

        fun psiStructure(psiStructure: Set<ClassStructure>?) = apply { this.psiStructure = psiStructure }

        fun chatMode(chatMode: ChatMode) = apply { this.chatMode = chatMode }

        fun modelId(modelId: String) = apply { this.modelId = modelId }

        fun hasCustomInput(customInput : Boolean) = apply { this.hasCustomInput = customInput }

        fun build(): TaskCompletionParameters {
            return TaskCompletionParameters(
                taskId,
                message,
                retry,
                imageDetailsList,
                history,
                referencedFiles,
                psiStructure,
                chatMode,
                modelId,
                hasCustomInput
            )
        }
    }

    companion object {
        @JvmStatic
        fun builder(taskId: String, message: Message) = Builder(taskId, message)
    }
}