package com.miracle.agent.mcp

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.nio.file.Files
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class McpClientHubTest {

    @Test
    fun testMcpClientHubInitialization() {
        val tempDir = Files.createTempDirectory("mcp-test").toFile()
        val originalUserHome = System.getProperty("user.home")
        val project = mockk<com.intellij.openapi.project.Project>(relaxed = true)
        every { project.name } returns "TestProject"
        every { project.basePath } returns tempDir.absolutePath

        try {
            System.setProperty("user.home", tempDir.resolve("user-home").absolutePath)

            val hub = McpClientHub(project)
            hub.initialize()

            val status = hub.getAllServerStatus()
            assertNotNull(status)
            assertTrue(status.isEmpty(), "Expected no MCP servers to be loaded for a fresh test project")

            val clients = hub.getAllClients()
            assertNotNull(clients)
            assertTrue(clients.isEmpty(), "Expected no MCP clients to be connected for a fresh test project")

            hub.shutdown()
        } finally {
            if (originalUserHome == null) {
                System.clearProperty("user.home")
            } else {
                System.setProperty("user.home", originalUserHome)
            }
            tempDir.deleteRecursively()
        }
    }
}
