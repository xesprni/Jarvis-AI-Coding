package com.miracle.utils.extensions

import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> CompletableFuture<T>.await(): T = suspendCancellableCoroutine { continuation ->
    this.whenComplete { result, exception ->
        if (exception != null) {
            continuation.resumeWithException(exception)
        } else {
            continuation.resume(result)
        }
    }
}