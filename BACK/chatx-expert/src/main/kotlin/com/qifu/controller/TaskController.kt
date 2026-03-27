package com.qifu.controller

import com.intellij.openapi.application.EDT
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.qifu.agent.AgentMessageType
import com.qifu.agent.AskResponse
import com.qifu.agent.JarvisAsk
import com.qifu.agent.Task
import com.qifu.agent.parser.Segment
import com.qifu.agent.parser.ToolSegment
import com.qifu.agent.parser.getToolSegmentHeader
import com.qifu.agent.tool.ToolRegistry
import com.qihoo.finance.lowcode.aiquestion.ui.worker.ChatSwingWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TaskController(
    var chatSwingWorker: ChatSwingWorker,
) {

    private var task: Task? = null
    private var taskId: String? = null
    private var askId: String? = null
    private var job: Job? = null
    private var builder = StringBuilder()
    private var lastSegment: Segment? = null
    private var lastToolPartial: String = ""

    fun chat(
        userInput: String,
    ) {
        if (task == null) {
            clear()
            task = Task(
                taskId = taskId,
                userInput = userInput,
                modelId = "OPENAI_COMPATIBLE_360/claude-4-sonnet",
                tools = ToolRegistry.getAll().values.toList(),
//                tools = ToolRegistry.getAskTools().values.toList(),
                updateConvTitle = { taskId, title ->
                    println("--------> taskId: ${taskId}, 生成了标题：$title")
                }
            )
            taskId = task!!.taskId

            // 启动taskLoop
            job = currentThreadCoroutineScope().launch {
                task!!.startTaskLoop().collect { agentMsg ->
                    withContext(Dispatchers.EDT) {
                        if (agentMsg.type == AgentMessageType.ERROR) {
                            // 先展示Text，再调用工具，这时要把上一个Text结束
                            lastSegment?.let {
                                builder.append(it.toMd()).append("\n")
                                lastSegment = null
                            }

                            builder.append("<font color=\"red\">${agentMsg.data[0].content}</font>").append("\n")
                            chatSwingWorker.process(builder.toString())
                        } else if (agentMsg.type == AgentMessageType.TOOL) {
                            // 先展示Text，再调用工具，这时要把上一个Text结束
                            lastSegment?.let {
                                builder.append(it.toMd()).append("\n")
                                lastSegment = null
                            }

                            if (agentMsg.isPartial) {
                                val segment = agentMsg.data[0] as ToolSegment
                                val partialTool =
                                    "${getToolSegmentHeader(segment).text}\n```${segment.toolCommand}\n${segment.toolContent}"
                                chatSwingWorker.process(builder.toString() + partialTool)
                            } else {
                                // TOOL 入参组装完毕
                                builder.append(agentMsg.data[0].toMd()).append("\n")
                                chatSwingWorker.process(builder.toString())
                            }
                        } else {
                            // 普通 AI 响应
                            agentMsg.data.forEach { segment ->
                                chatSwingWorker.process(builder.toString() + segment.toMd())
                                if (lastSegment != null && (lastSegment!!::class != segment::class)) {
                                    builder.append(lastSegment!!.toMd()).append("\n")
                                }
                                lastSegment = segment
                            }
                        }
                        if (!agentMsg.isPartial && agentMsg is JarvisAsk) {
                            // 模拟展示 Approve/Reject 按钮
                            builder.append("**Input yes/no or other msg to continue...**\n")
                            chatSwingWorker.process(builder.toString())
                            askId = agentMsg.id
                            // 等待用户响应
                            chatSwingWorker.done()
                        }
                    }
                }
                // loop结束
                task = null
                chatSwingWorker.done()
            }
        } else {
            clear()
            var message: String? = null
            val type = when {
                userInput.trim().equals("yes", ignoreCase = true) -> AskResponse.ResponseType.YES
                userInput.trim().equals("no", ignoreCase = true) -> AskResponse.ResponseType.NO
                else -> {
                    message = userInput
                    AskResponse.ResponseType.MESSAGE
                }
            }
            val askResp = AskResponse(id = askId!!, type = type, message = message)
            askId = null
            task!!.askResponse(askResp)
        }
    }

    fun clear() {
        builder.clear()
        lastSegment = null
    }

    /**
     * 用户中止时，需要调用此方法
     */
    fun cancel() {
        job?.cancel()
    }

    private fun convertToolRequest() {

    }
}