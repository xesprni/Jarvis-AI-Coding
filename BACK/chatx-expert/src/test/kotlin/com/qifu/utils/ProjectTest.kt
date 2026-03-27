package com.qifu.utils

import kotlin.test.*

class ProjectTest {

    @Test
    fun testGetCurrentProject() {
        val project = getCurrentProject()
        assertNull(project, "getCurrentProject() should return null when in unit test")
    }

    @Test
    fun testGetCurrentProjectRootPath() {
        val rootPath = getCurrentProjectRootPath()
        assertNotNull(rootPath, "getCurrentProjectRootPath() should return null or String")
        println("Current project root path: $rootPath")
    }

}
