package com.miracle.utils.extensions

import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 将 [CompletableFuture] 转换为可挂起的协程调用，挂起当前协程直到 Future 完成。
 * 支持协程取消。
 *
 * @param T Future 返回值类型
 * @return Future 的结果值
 * @throws Exception 如果 Future 异常完成，则抛出对应的异常
 */
suspend fun <T> CompletableFuture<T>.await(): T = suspendCancellableCoroutine { continuation ->
    this.whenComplete { result, exception ->
        if (exception != null) {
            continuation.resumeWithException(exception)
        } else {
            continuation.resume(result)
        }
    }
}
