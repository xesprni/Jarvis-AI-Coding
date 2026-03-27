package com.qifu.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.qifu.utils.ShellType
import com.qifu.utils.detectShell

@State(
    name = "AgentSettings",                       // 持久化配置的唯一标识
    storages = [Storage("jarvis/AgentSettings.xml")]     // 存储位置
)
@Service(Service.Level.APP)
class AgentSettings : SimplePersistentStateComponent<AgentSettings.State>(State()) {

    init {
        if (state.shellType == null) {
            state.shellType = getDefaultShellType()
        }
    }

    class State : BaseState() {
        var shellType by enum<ShellType>()
        var disabledSkills by list<String>()
    }

    companion object {
        private val DEFAULT = State().apply {
            shellType = getDefaultShellType()
        }
        val state: State
            get() = ApplicationManager.getApplication()?.getService(AgentSettings::class.java)?.state ?: DEFAULT

        fun getDefaultShellType(): ShellType {
            return try {
                detectShell().type
            } catch (e: Exception) {
                ShellType.POSIX
            }
        }
    }
}