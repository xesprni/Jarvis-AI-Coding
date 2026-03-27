package com.qifu.ui.smartconversation.sse

import com.intellij.openapi.application.EDT
import com.qihoo.finance.lowcode.aiquestion.ui.worker.ChatSwingWorker
import com.qihoo.finance.lowcode.smartconversation.service.CompletionEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * @author weiyichao
 * @date 2025-10-01
 **/
class AgentUIWorker(
    private val eventListener: CompletionEventListener<String>
) {
    private var chatSwingWorker: ChatSwingWorker? = null

    /**
     * 设置实际的UI工作器
     */
    fun setChatSwingWorker(worker: ChatSwingWorker) {
        this.chatSwingWorker = worker
    }

    /**
     * 处理内容更新
     */
    suspend fun process(content: String) {
        withContext(Dispatchers.EDT) {
            chatSwingWorker?.process(content)
        }
    }

    /**
     * 完成处理
     */
    suspend fun done() {
        withContext(Dispatchers.EDT) {
            chatSwingWorker?.done()
        }
    }
}