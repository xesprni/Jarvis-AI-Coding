package com.qifu.ui.smartconversation.settings.configuration

import com.intellij.util.messages.Topic

fun interface ConfigurationStateListener {
    fun onConfigurationChanged(newState: ConfigurationSettingsState)

    companion object {
        val TOPIC: Topic<ConfigurationStateListener> =
            Topic.create("Configuration Changed", ConfigurationStateListener::class.java)
    }
}