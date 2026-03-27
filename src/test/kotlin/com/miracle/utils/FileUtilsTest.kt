package com.miracle.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class FileUtilsTest {

    @Test
    fun testToRelativePathWithAbsolutePaths() {
        // Windows style paths
        val result1 = toRelativePath("C:/project/src/main.kt", "C:/project")
        assertEquals("src/main.kt", result1)

        // Unix style paths
        val result2 = toRelativePath("/home/user/project/src/main.kt", "/home/user/project")
        assertEquals("src/main.kt", result2)
    }

    @Test
    fun testToRelativePathWithRelativePaths() {
        // When providing a relative path, it should return the same path normalized
        val result1 = toRelativePath("src/main.kt", "/project")
        assertEquals("src/main.kt", result1)

        val result2 = toRelativePath("../other/file.kt", "/project/src")
        assertEquals("../other/file.kt", result2)
    }

    @Test
    fun testToRelativePathWithSamePaths() {
        val result = toRelativePath("/project/src/main.kt", "/project/src/main.kt")
        // When paths are the same, the function returns an empty string, not "."
        assertEquals("", result)
    }

    @Test
    fun testToAbsolutePathWithRelativePath() {
        // Windows style paths
        val result1 = toAbsolutePath("src/main.kt", "C:/project")
        // On Windows, the actual result will include the drive letter
        // We'll test that it ends with the expected path
        assert(result1.endsWith("project/src/main.kt"))

        // Unix style paths
        val result2 = toAbsolutePath("src/main.kt", "/home/user/project")
        // On Windows, the result may include a drive letter, so we'll check if it ends with the expected path
        assert(result2.endsWith("/home/user/project/src/main.kt"))
    }

    @Test
    fun testToAbsolutePathWithAbsolutePath() {
        // When providing an absolute path, it should return the same path normalized
        val result1 = toAbsolutePath("/absolute/path/file.kt", "/project")
        // On Windows, the result may include a drive letter, so we'll check if it ends with the expected path
        assert(result1.endsWith("/absolute/path/file.kt"))

        val result2 = toAbsolutePath("C:/absolute/path/file.kt", "/project")
        assertEquals("C:/absolute/path/file.kt", result2)
    }

    @Test
    fun testToAbsolutePathWithParentDirectoryReferences() {
        val result = toAbsolutePath("../other/file.kt", "/home/user/project/src")
        // The result depends on the actual system, so we'll just check it contains the expected parts
        assert(result.contains("/home/user/project/other/file.kt"))
    }

    @Test
    fun testToRelativePathWithNonPathInput() {
        // Test with non-path input like "Found 3 files in 517ms"
        val result = toRelativePath("Found 3 files in 517ms", "/home/user/project")
        // The function should handle this gracefully and return a normalized version of the input
        assertEquals("Found 3 files in 517ms", result)
    }

    @Test
    fun testToAbsolutePathWithNonPathInput() {
        // Test with non-path input like "Found 3 files in 517ms"
        val result = toAbsolutePath("Found 3 files in 517ms", "/home/user/project")
        // The function should handle this gracefully, resolving it against the base path
        assert(result.contains("Found 3 files in 517ms"))
    }
}