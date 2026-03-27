package com.qifu.ui.smartconversation.sse

import com.intellij.openapi.project.Project
import com.qifu.agent.parser.Segment
import com.qifu.ui.smartconversation.settings.service.TaskCompletionParameters
import com.qihoo.finance.lowcode.smartconversation.service.CompletionEventListener
import kotlinx.coroutines.Job
import kotlin.coroutines.cancellation.CancellationException


/**
 * @author weiyichao
 * @date 2025-09-30
 **/

class AgentJob(private val job: Job, private val project: Project) {

    /**
     * 取消任务（替换EventSource.cancel()）
     */
    fun cancel(taskId: String) {
        try {
            val agentCompletionRequestService = project.getService(AgentCompletionRequestService::class.java)
            agentCompletionRequestService.cancel(taskId)
        } catch (e: Throwable) {
            if (e is CancellationException) {
                println("任务手动停止")
            }
        }

    }

    /**
     * 检查是否活跃
     */
    fun isActive(): Boolean {
        return job.isActive
    }

    /**
     * 等待完成
     */
    suspend fun join() {
        job.join()
    }
}

/**
 * EventSource到Agent的适配器
 * 让现有代码可以无缝切换到Agent模式
 */
object EventSourceToAgentAdapter {

    /**
     * 替换CompletionRequestService.getChatCompletionAsync
     */
    fun getChatCompletionAsync(
        callParameters: TaskCompletionParameters,
        eventListener: CompletionEventListener<Segment>?,
        project: Project
    ): AgentJob {
        val agentCompletionRequestService = project.getService(AgentCompletionRequestService::class.java)
        return agentCompletionRequestService.getChatCompletionAsync(callParameters, eventListener, project)
    }
}
