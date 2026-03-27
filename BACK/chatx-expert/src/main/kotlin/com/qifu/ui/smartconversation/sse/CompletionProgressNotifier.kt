package com.qifu.ui.smartconversation.sse

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.messages.Topic

interface CompletionProgressNotifier {

    fun update()

    companion object {
        @JvmStatic
        val COMPLETION_PROGRESS_TOPIC =
            Topic.create("completionProgressTopic", CompletionProgressNotifier::class.java)

        @JvmStatic
        fun update(project: Project, loading: Boolean) {
            if (project.isDisposed) return

            Key.create<Boolean?>("jarvis.completionInProgress").set(project, loading)

            project.messageBus.syncPublisher(COMPLETION_PROGRESS_TOPIC)?.update()
        }
    }
}