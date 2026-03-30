package com.miracle.utils.coroutines

import com.intellij.openapi.application.ModalityState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * 协程调度器工厂类，提供获取各类协程调度器的便捷方法。
 */
class CoroutineDispatchers {

    /**
     * 获取默认调度器（共享后台线程池）。
     *
     * @return 默认协程调度器
     */
    fun default(): CoroutineDispatcher = Dispatchers.Default

    /**
     * 获取无限制调度器，不限制线程和执行位置。
     *
     * @return 无限制协程调度器
     */
    fun unconfined(): CoroutineDispatcher = Dispatchers.Unconfined

    /**
     * 获取 IO 调度器，适用于磁盘/网络 IO 操作。
     *
     * @return IO 协程调度器
     */
    fun io(): CoroutineDispatcher = Dispatchers.IO

    /**
     * 获取非模态 EDT 调度器。
     *
     * @return 非模态 EDT 协程调度器
     */
    fun edtNonModal(): CoroutineDispatcher = EdtDispatchers.NonModal

    /**
     * 获取当前模态状态的 EDT 调度器。
     *
     * @return 当前模态 EDT 协程调度器
     */
    fun edtCurrent(): CoroutineDispatcher = EdtDispatchers.Current

    /**
     * 获取默认模态状态的 EDT 调度器。
     *
     * @return 默认 EDT 协程调度器
     */
    fun edtDefault(): CoroutineDispatcher = EdtDispatchers.Default
}

/**
 * EDT 协程调度器单例集合，提供预配置的 EDT 调度器实例。
 */
object EdtDispatchers {

    /** 非模态 EDT 调度器，不会阻塞任何模态对话框 */
    val NonModal = EdtCoroutineDispatcher(ModalityState.NON_MODAL)

    /** 默认模态状态的 EDT 调度器 */
    val Default = EdtCoroutineDispatcher()

    /** 当前模态状态的 EDT 调度器 */
    val Current = EdtCoroutineDispatcher(ModalityState.current())

    /**
     * 根据指定的模态状态创建 EDT 调度器。
     *
     * @param modalityState 模态状态
     * @return 对应模态状态的 EDT 协程调度器
     */
    fun withModalityState(modalityState: ModalityState): CoroutineDispatcher {
        return EdtCoroutineDispatcher(modalityState)
    }
}
