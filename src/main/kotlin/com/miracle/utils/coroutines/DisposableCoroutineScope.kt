package com.miracle.utils.coroutines

import com.intellij.openapi.Disposable
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * 可释放的协程作用域，同时实现 [Disposable] 和 [CoroutineScope] 接口。
 * 在 IDE 生命周期中管理协程，当对象被释放时自动取消所有子协程。
 *
 * @param scopeDispatcher 协程调度器，默认使用 [EdtDispatchers.Default]
 */
internal class DisposableCoroutineScope(
    scopeDispatcher: CoroutineDispatcher = EdtDispatchers.Default
) : Disposable, CoroutineScope {

    /** 内部协程作用域，使用 SupervisorJob 保证子协程互不影响 */
    private val coroutineScope = CoroutineScope(SupervisorJob() + scopeDispatcher)

    /**
     * 在协程作用域中启动一个新的协程。
     *
     * @param block 协程执行体
     * @return 协程任务 [Job]
     */
    fun launch(block: suspend CoroutineScope.() -> Unit): Job =
        coroutineScope.launch { block() }


    /**
     * 释放资源，取消所有正在运行的协程。
     */
    override fun dispose() {
        coroutineScope.cancel()
    }

    /** 协程上下文 */
    override val coroutineContext: CoroutineContext
        get() = coroutineScope.coroutineContext
}
