package com.miracle.utils

import kotlinx.coroutines.delay

object retryUtils {

    /**
     * 重试函数，用于执行可能失败的操作
     * @param times 重试次数
     * @param block 要执行的操作
     */
    suspend fun <T> retry(times: Int, block: suspend () -> T): T {
        var lastException: Exception? = null
        for (i in 1..times) {
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (i < times) {
                    // 等待一段时间再重试
                    delay(timeMillis = 1000L * i)
                }
            }
        }
        throw lastException!!
    }
}