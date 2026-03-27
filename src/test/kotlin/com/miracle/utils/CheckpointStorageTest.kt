package com.miracle.utils

import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.mockk
import java.nio.file.Files
import java.util.UUID
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.pathString
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckpointStorageTest {

    private val projectDir = Files.createTempDirectory("checkpoint-project")
    private val projectName = "checkpoint-test-${UUID.randomUUID()}"
    private val project: Project = mockk(relaxed = true)
    private val convId = "conv-${UUID.randomUUID()}"
    private val messageId = "msg-${UUID.randomUUID()}"
    private val convDir = Path(getChatDirectory()) / sanitizeFileName(projectName) / convId

    init {
        every { project.name } returns projectName
        every { project.basePath } returns projectDir.toString()
    }

    @AfterTest
    fun cleanup() {
        runCatching { convDir.toFile().deleteRecursively() }
        runCatching { projectDir.toFile().deleteRecursively() }
    }

    @Test
    fun testRecordFileCheckpointIdempotentAndRestoreFiles() {
        val targetFile = projectDir.resolve("src/TestFile.kt")
        targetFile.parent?.createDirectories()
        targetFile.writeText("line-1")

        CheckpointStorage.initMessageContextCheckpoint(project, convId, messageId)
        CheckpointStorage.recordFileCheckpointIfAbsent(project, convId, messageId, targetFile.pathString)
        CheckpointStorage.recordFileCheckpointIfAbsent(project, convId, messageId, targetFile.pathString)

        targetFile.writeText("line-2")
        assertEquals(1, CheckpointStorage.getChangedFiles(project, convId, messageId).size)
        val summaries = CheckpointStorage.getPendingFileChangeSummaries(project, convId, messageId)
        assertEquals(1, summaries.size)
        assertEquals(1, summaries.first().addedLines)
        assertEquals(1, summaries.first().deletedLines)
        assertEquals(2, summaries.first().totalChangedLines)

        CheckpointStorage.restoreCheckpointAndContext(project, convId, messageId)
        assertEquals("line-1", targetFile.readText())
    }

    @Test
    fun testRestoreDeletesFilesThatDidNotExistAtSnapshotAndRestoreContext() {
        convDir.createDirectories()
        val history = convDir / "chat-history.jsonl"
        val memory = convDir / "chat-memory-default.jsonl"
        val todo = convDir / "todo-agent-default.json"
        history.writeText("history-before")
        memory.writeText("memory-before")
        todo.writeText("todo-before")

        val newFile = projectDir.resolve("src/NewFile.kt")
        newFile.parent?.createDirectories()

        CheckpointStorage.initMessageContextCheckpoint(project, convId, messageId)
        CheckpointStorage.recordFileCheckpointIfAbsent(project, convId, messageId, newFile.pathString)

        newFile.writeText("created-after")
        history.writeText("history-after")
        memory.writeText("memory-after")
        todo.writeText("todo-after")
        val extraMemory = convDir / "chat-memory-extra.jsonl"
        extraMemory.writeText("extra")

        CheckpointStorage.restoreCheckpointAndContext(project, convId, messageId)

        assertFalse(newFile.exists(), "File created after checkpoint should be deleted on restore")
        assertEquals("history-before", history.readText())
        assertEquals("memory-before", memory.readText())
        assertEquals("todo-before", todo.readText())
        assertFalse(extraMemory.exists(), "Extra memory files created after checkpoint should be removed")
    }

    @Test
    fun testPendingFileChangeSummariesForNewFile() {
        val newFile = projectDir.resolve("src/NewFile.kt")
        newFile.parent?.createDirectories()

        CheckpointStorage.initMessageContextCheckpoint(project, convId, messageId)
        CheckpointStorage.recordFileCheckpointIfAbsent(project, convId, messageId, newFile.pathString)

        newFile.writeText("created-after")

        val summaries = CheckpointStorage.getPendingFileChangeSummaries(project, convId, messageId)
        assertEquals(1, summaries.size)
        assertTrue(summaries.first().isNewFile)
        assertEquals(1, summaries.first().addedLines)
        assertEquals(0, summaries.first().deletedLines)
        assertEquals(1, summaries.first().totalChangedLines)
    }

    @Test
    fun testRestoreSingleFileKeepsOtherFilesAndContextUntouched() {
        convDir.createDirectories()
        val history = convDir / "chat-history.jsonl"
        history.writeText("history-before")

        val fileA = projectDir.resolve("src/FileA.kt")
        val fileB = projectDir.resolve("src/FileB.kt")
        fileA.parent?.createDirectories()
        fileB.parent?.createDirectories()
        fileA.writeText("alpha-before")
        fileB.writeText("beta-before")

        CheckpointStorage.initMessageContextCheckpoint(project, convId, messageId)
        CheckpointStorage.recordFileCheckpointIfAbsent(project, convId, messageId, fileA.pathString)
        CheckpointStorage.recordFileCheckpointIfAbsent(project, convId, messageId, fileB.pathString)

        fileA.writeText("alpha-after")
        fileB.writeText("beta-after")
        history.writeText("history-after")

        CheckpointStorage.restoreSingleFile(project, convId, messageId, fileA.pathString)

        assertEquals("alpha-before", fileA.readText())
        assertEquals("beta-after", fileB.readText())
        assertEquals("history-after", history.readText(), "Single-file restore should not rewind conversation context")
        assertTrue(CheckpointStorage.hasRollbackCheckpoint(project, convId, messageId))
        assertEquals(listOf(fileB.pathString), CheckpointStorage.getPendingFileChangeSummaries(project, convId, messageId).map { it.absolutePath })
    }

    @Test
    fun testClearCheckpointsFromMessageRemovesCurrentAndFollowing() {
        val m1 = "msg-${UUID.randomUUID()}"
        val m2 = "msg-${UUID.randomUUID()}"
        val m3 = "msg-${UUID.randomUUID()}"

        CheckpointStorage.initMessageContextCheckpoint(project, convId, m1)
        Thread.sleep(2)
        CheckpointStorage.initMessageContextCheckpoint(project, convId, m2)
        Thread.sleep(2)
        CheckpointStorage.initMessageContextCheckpoint(project, convId, m3)

        CheckpointStorage.clearCheckpointsFromMessage(project, convId, m2)

        val checkpointRoot = convDir / "checkpoint"
        assertTrue((checkpointRoot / m1).exists(), "Older checkpoint should remain")
        assertFalse((checkpointRoot / m2).exists(), "Selected checkpoint should be removed")
        assertFalse((checkpointRoot / m3).exists(), "Newer checkpoint should be removed")
    }

    @Test
    fun testRollbackRestoresEarlierMessageStateAndClearsFollowingCheckpoints() {
        convDir.createDirectories()
        val history = convDir / "chat-history.jsonl"
        val memory = convDir / "chat-memory-default.jsonl"
        val todo = convDir / "todo-agent-default.json"
        val targetFile = projectDir.resolve("src/RollbackTarget.kt")
        val siblingFile = projectDir.resolve("src/RollbackSibling.kt")
        targetFile.parent?.createDirectories()
        siblingFile.parent?.createDirectories()

        history.writeText("history-v1")
        memory.writeText("memory-v1")
        todo.writeText("todo-v1")
        targetFile.writeText("file-v1")
        siblingFile.writeText("sibling-v1")

        val firstMessageId = "msg-${UUID.randomUUID()}"
        val secondMessageId = "msg-${UUID.randomUUID()}"

        CheckpointStorage.initMessageContextCheckpoint(project, convId, firstMessageId)
        CheckpointStorage.recordFileCheckpointIfAbsent(project, convId, firstMessageId, targetFile.pathString)
        CheckpointStorage.recordFileCheckpointIfAbsent(project, convId, firstMessageId, siblingFile.pathString)

        history.writeText("history-v2")
        memory.writeText("memory-v2")
        todo.writeText("todo-v2")
        targetFile.writeText("file-v2")
        siblingFile.writeText("sibling-v2")

        CheckpointStorage.restoreSingleFile(project, convId, firstMessageId, targetFile.pathString)

        assertEquals("file-v1", targetFile.readText())
        assertEquals("sibling-v2", siblingFile.readText())
        assertTrue(CheckpointStorage.hasRollbackCheckpoint(project, convId, firstMessageId))

        Thread.sleep(2)
        CheckpointStorage.initMessageContextCheckpoint(project, convId, secondMessageId)
        CheckpointStorage.recordFileCheckpointIfAbsent(project, convId, secondMessageId, targetFile.pathString)

        history.writeText("history-v3")
        memory.writeText("memory-v3")
        todo.writeText("todo-v3")
        targetFile.writeText("file-v3")
        siblingFile.writeText("sibling-v3")

        CheckpointStorage.restoreCheckpointAndContext(project, convId, firstMessageId)
        CheckpointStorage.clearCheckpointsFromMessage(project, convId, firstMessageId)

        assertEquals("history-v1", history.readText())
        assertEquals("memory-v1", memory.readText())
        assertEquals("todo-v1", todo.readText())
        assertEquals("file-v1", targetFile.readText())
        assertEquals("sibling-v1", siblingFile.readText())
        assertFalse((convDir / "checkpoint").exists(), "Rollback should remove the restored checkpoint and all following checkpoints")
    }

    @Test
    fun testLegacyManifestWithoutRestoredFieldStillReadable() {
        val legacyMessageId = "msg-${UUID.randomUUID()}"
        val targetFile = projectDir.resolve("src/Legacy.kt")
        targetFile.parent?.createDirectories()
        targetFile.writeText("legacy-before")

        CheckpointStorage.initMessageContextCheckpoint(project, convId, legacyMessageId)
        CheckpointStorage.recordFileCheckpointIfAbsent(project, convId, legacyMessageId, targetFile.pathString)

        val manifestPath = convDir / "checkpoint" / legacyMessageId / "files" / "manifest.json"
        manifestPath.writeText(
            """
            {
              "entries": [
                {
                  "filePath": "${targetFile.pathString.replace("\\", "\\\\")}",
                  "existed": true,
                  "snapshotFile": "${sha256(targetFile.pathString)}.bin"
                }
              ]
            }
            """.trimIndent()
        )

        targetFile.writeText("legacy-after")

        val summaries = CheckpointStorage.getPendingFileChangeSummaries(project, convId, legacyMessageId)
        assertEquals(1, summaries.size)
        assertEquals(2, summaries.first().totalChangedLines)
    }

    private fun sha256(text: String): String {
        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
    }
}
