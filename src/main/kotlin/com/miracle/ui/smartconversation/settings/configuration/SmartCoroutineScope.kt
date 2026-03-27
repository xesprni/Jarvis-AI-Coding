package com.miracle.ui.smartconversation.settings.configuration

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.serviceOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Service(Service.Level.APP)
class SmartCoroutineScope(
    private val cs: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    fun coroutineScope(): CoroutineScope = cs

    companion object {
        fun get(): CoroutineScope? {
            return serviceOrNull<SmartCoroutineScope>()?.coroutineScope()
        }
    }
}
