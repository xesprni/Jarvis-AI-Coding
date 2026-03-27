package com.qifu.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.components.Service

@State(
    name = "AutoApproveSettings",                       // 持久化配置的唯一标识
    storages = [Storage("jarvis/autoApproveSettings.xml")]     // 存储位置
)
@Service(Service.Level.APP)
class AutoApproveSettings: SimplePersistentStateComponent<AutoApproveSettings.State>(State()) {

    class State : BaseState() {
        var enabled by property(true)
        var maxRequests by property(40)
        var actions by property(Actions())
        var favorites by list<String>()
        var autoRunCommandsBlacklist by list<String>()

        init {
            favorites.addAll(listOf("enableAutoApprove", "readFiles", "editFiles"))
            autoRunCommandsBlacklist.addAll(listOf(
                "bash", "erase", "rd", "ri", "rm", "delete", "del", "truncate", "kill",
            ))
        }

        class Actions : BaseState() {
            var readFiles by property(true)
            var readFilesExternally by property(false)
            var editFiles by property(false)
            var editFilesExternally by property(false)
            var executeSafeCommands by property(true)
            var executeAllCommands by property(false)
            var useBrowser by property(false)
            var useMcp by property(false)
            /** 执行 task 工具 */
            var runTask by property(true)
            var runSkill by property(true)
        }
    }

    companion object {
        private val DEFAULT = State()
        val state: State
            get() = ApplicationManager.getApplication()?.getService(AutoApproveSettings::class.java)?.state ?: DEFAULT
    }
}