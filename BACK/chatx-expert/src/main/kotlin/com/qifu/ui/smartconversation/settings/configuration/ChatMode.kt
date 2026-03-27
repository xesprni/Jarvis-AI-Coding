package com.qifu.ui.smartconversation.settings.configuration

data class ChatMode(
    val displayName: String,
    val description: String,
    val isEnabled: Boolean = true
) {
    companion object {
        val ASK = ChatMode(
            displayName = "Ask",
            description = "Conversational responses with explanations"
        )
        val AGENT = ChatMode(
            displayName = "Agent",
            description = "AI agent with tool access"
        )
        val PLAN = ChatMode(
            displayName = "Plan",
            description = "Read-only planning flow"
        )

        val entries = listOf(AGENT, PLAN, ASK)
    }
}
