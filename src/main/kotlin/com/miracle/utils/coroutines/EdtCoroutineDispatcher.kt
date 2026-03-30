package com.miracle.utils.coroutines

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext

/**
 * EDT（Event Dispatch Thread）协程调度器，将协程任务派发到 IntelliJ IDEA 的 UI 线程执行。
 * 如果当前已在 UI 线程则直接执行，否则通过 [ApplicationManager.invokeLater] 投递到 UI 线程。
 *
 * @param modalityState 模态状态，用于控制 UI 线程任务的可中断性
 */
class EdtCoroutineDispatcher(
    private val modalityState: ModalityState = ModalityState.defaultModalityState()
) : CoroutineDispatcher() {

    /**
     * 将协程任务派发到 EDT 线程执行。
     *
     * @param context 协程上下文
     * @param block 待执行的任务
     */
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        val app = ApplicationManager.getApplication()

        if (app.isDispatchThread) {
            block.run()
        } else {
            app.invokeLater({ block.run() }, modalityState)
        }
    }
}
