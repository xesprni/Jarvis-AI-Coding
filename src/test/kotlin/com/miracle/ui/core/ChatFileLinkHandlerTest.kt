package com.miracle.ui.core

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ChatFileLinkHandlerTest {

    @Test
    fun parseShouldDecodeProjectRelativePathAndCoordinates() {
        val target = ChatFileLinkHandler.parse(
            "jarvis-file://src%20dir/%E4%B8%AD%E6%96%87.kt?line=42&column=7"
        )

        assertNotNull(target)
        assertEquals("src dir/中文.kt", target.relativePath)
        assertEquals(42, target.line)
        assertEquals(7, target.column)
    }

    @Test
    fun parseShouldAllowFileOnlyLinks() {
        val target = ChatFileLinkHandler.parse("jarvis-file://src/main/kotlin/com/miracle/App.kt")

        assertNotNull(target)
        assertEquals("src/main/kotlin/com/miracle/App.kt", target.relativePath)
        assertNull(target.line)
        assertNull(target.column)
    }

    @Test
    fun parseShouldRejectAbsoluteOrMalformedTargets() {
        assertNull(ChatFileLinkHandler.parse("jarvis-file:///etc/passwd"))
        assertNull(ChatFileLinkHandler.parse("jarvis-file://src/main.kt?line=0"))
        assertNull(ChatFileLinkHandler.parse("jarvis-file://src/main.kt?line=abc"))
        assertNull(ChatFileLinkHandler.parse("jarvis-file://src/main.kt?column=3"))
    }

    @Test
    fun resolveProjectFileShouldReturnProjectFileInsideRoot() {
        val projectDir = Files.createTempDirectory("jarvis-link-project")
        val targetFile = projectDir.resolve("src/main/kotlin/com/miracle/App.kt")
        Files.createDirectories(targetFile.parent)
        Files.writeString(targetFile, "fun app() = Unit")

        val target = assertNotNull(
            ChatFileLinkHandler.parse("jarvis-file://src/main/kotlin/com/miracle/App.kt?line=2")
        )

        val resolved = ChatFileLinkHandler.resolveProjectFile(projectDir.toString(), target)

        assertEquals(targetFile.toRealPath(), resolved)
    }

    @Test
    fun resolveProjectFileShouldRejectTraversalOutsideProject() {
        val projectDir = Files.createTempDirectory("jarvis-link-project")
        val outsideFile = Files.createTempFile(projectDir.parent, "jarvis-link-outside-", ".kt")
        Files.writeString(outsideFile, "fun outside() = Unit")
        val escapedRelativePath = projectDir.relativize(outsideFile).toString().replace('\\', '/')

        val target = assertNotNull(ChatFileLinkHandler.parse("jarvis-file://$escapedRelativePath"))

        val resolved = ChatFileLinkHandler.resolveProjectFile(projectDir.toString(), target)

        assertNull(resolved)
    }

    @Test
    fun resolveProjectFileShouldRejectSymlinkEscapeWhenSupported() {
        val projectDir = Files.createTempDirectory("jarvis-link-project")
        val outsideDir = Files.createTempDirectory("jarvis-link-outside")
        val outsideFile = outsideDir.resolve("escape.kt")
        Files.writeString(outsideFile, "fun escape() = Unit")

        val linkDir = projectDir.resolve("links")
        Files.createDirectories(linkDir)
        val symlink = linkDir.resolve("escape.kt")
        val created = runCatching {
            Files.createSymbolicLink(symlink, outsideFile)
        }.isSuccess
        if (!created) return

        val target = assertNotNull(ChatFileLinkHandler.parse("jarvis-file://links/escape.kt"))

        val resolved = ChatFileLinkHandler.resolveProjectFile(projectDir.toString(), target)

        assertNull(resolved)
    }

    @Test
    fun resolveProjectFileShouldRejectMissingFile() {
        val projectDir = Files.createTempDirectory("jarvis-link-project")
        val target = assertNotNull(ChatFileLinkHandler.parse("jarvis-file://src/missing.kt"))

        val resolved = ChatFileLinkHandler.resolveProjectFile(projectDir.toString(), target)

        assertNull(resolved)
    }
}
