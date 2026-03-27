package com.qifu.agent.tool

import com.intellij.openapi.project.Project
import com.qifu.agent.TaskState
import com.qifu.agent.parser.ToolSegment
import com.qifu.agent.parser.UiToolName
import com.qifu.utils.CheckpointStorage
import com.qifu.utils.getChatDirectory
import com.qifu.utils.sanitizeFileName
import dev.langchain4j.agent.tool.ToolExecutionRequest
import io.mockk.every
import io.mockk.mockk
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.Path as KPath
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.pathString
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToolExecutorCheckpointTest {

    private val projectRoot: Path = Files.createTempDirectory("tool-executor-checkpoint-project")
    private val projectName = "tool-executor-${UUID.randomUUID()}"
    private val project: Project = mockk(relaxed = true)
    private val convId = "conv-${UUID.randomUUID()}"
    private val messageId = "msg-${UUID.randomUUID()}"
    private val convDir = KPath(getChatDirectory()) / sanitizeFileName(projectName) / convId

    init {
        every { project.name } returns projectName
        every { project.basePath } returns projectRoot.pathString
    }

    @AfterTest
    fun cleanup() {
        runCatching { convDir.toFile().deleteRecursively() }
        runCatching { projectRoot.toFile().deleteRecursively() }
    }

    @Test
    fun testWriteToolRecordsCheckpointWhenUserMessageIdPresent() {
        val file = projectRoot.resolve("src/WriteTarget.kt")
        file.parent?.createDirectories()
        file.writeText("before")

        invokeCheckpointCapture(WriteTool.getName(), mockTaskState(messageId), file.pathString)

        assertTrue(CheckpointStorage.hasChangedFiles(project, convId, messageId))
    }

    @Test
    fun testNonWriteEditToolDoesNotRecordCheckpoint() {
        val file = projectRoot.resolve("src/ReadOnlyTarget.kt")
        file.parent?.createDirectories()
        file.writeText("before")

        invokeCheckpointCapture(ReadTool.getName(), mockTaskState(messageId), file.pathString)

        assertFalse(CheckpointStorage.hasChangedFiles(project, convId, messageId))
    }

    @Test
    fun testMissingUserMessageIdDoesNotRecordCheckpoint() {
        val file = projectRoot.resolve("src/MissingMessageId.kt")
        file.parent?.createDirectories()
        file.writeText("before")

        invokeCheckpointCapture(EditTool.getName(), mockTaskState(null), file.pathString)

        assertFalse(CheckpointStorage.hasChangedFiles(project, convId, messageId))
    }

    private fun mockTaskState(userMessageId: String?): TaskState {
        val taskState = mockk<TaskState>(relaxed = true)
        every { taskState.project } returns project
        every { taskState.convId } returns convId
        every { taskState.userMessageId } returns userMessageId
        return taskState
    }

    private fun invokeCheckpointCapture(toolName: String, taskState: TaskState, filePath: String) {
        val request = ToolExecutionRequest.builder()
            .id("req-${UUID.randomUUID()}")
            .name(toolName)
            .arguments("{}")
            .build()
        val segment = ToolSegment(
            name = UiToolName.EDITED_EXISTING_FILE,
            toolCommand = filePath,
            toolContent = "",
            params = emptyMap()
        )
        val method = ToolExecutor::class.java.getDeclaredMethod(
            "recordCheckpointForWriteEdit",
            ToolExecutionRequest::class.java,
            TaskState::class.java,
            ToolSegment::class.java
        )
        method.isAccessible = true
        method.invoke(ToolExecutor, request, taskState, segment)
    }
}
