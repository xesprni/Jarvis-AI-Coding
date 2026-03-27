package com.qifu.ui.smartconversation.settings.configuration

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.serviceOrNull
import kotlinx.coroutines.CoroutineScope


/**
 * @author weiyichao
 * @date 2025-10-02
 **/
@Service
class SmartCoroutineScope(private val cs: CoroutineScope) {

    fun coroutineScope(): CoroutineScope {
        return cs
    }

    companion object {
        fun get(): CoroutineScope? {
            return serviceOrNull<SmartCoroutineScope>()?.coroutineScope()
        }
    }
}