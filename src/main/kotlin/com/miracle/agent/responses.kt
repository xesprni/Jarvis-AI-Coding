package com.miracle.agent

object AiResponse {

    fun toolNotExists(toolName: String, availableToolNames: String) =
        """Error: The tool "$toolName" does not exist. Please choose one of the available tools: $availableToolNames."""

    fun toolDenied() = "The user denied this operation."

    fun toolCancelled() = "The user cancelled this operation."

    fun toolError (error: String?) =
        "The tool execution failed with the following error:\n<error>\n${error}\n</error>"

}

object UiResponse {

    fun autoApprovalMaxReqReached(maxApiRequestCount: Int) =
        "Jarvis has auto-approved $maxApiRequestCount API requests. Would you like to reset the count and proceed with the task?"

    fun consecutiveApiReqFailed() = "Jarvis is having trouble. Would you like to continue the task?"
}