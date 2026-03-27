package com.miracle.utils

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecurityUtilTest {

    @Test
    fun testIsReadOnlyCommandWithSafeCommands() {
        assertTrue(SecurityUtil.isReadOnlyCommand("ls"))
        assertTrue(SecurityUtil.isReadOnlyCommand("cat file.txt"))
        assertTrue(SecurityUtil.isReadOnlyCommand("grep pattern file.txt"))
        assertTrue(SecurityUtil.isReadOnlyCommand("find . -name '*.txt'"))
        assertTrue(SecurityUtil.isReadOnlyCommand("head file.txt"))
        assertTrue(SecurityUtil.isReadOnlyCommand("tail file.txt"))
        assertTrue(SecurityUtil.isReadOnlyCommand("stat file.txt"))
        assertTrue(SecurityUtil.isReadOnlyCommand("pwd"))
        assertTrue(SecurityUtil.isReadOnlyCommand("whoami"))
        assertTrue(SecurityUtil.isReadOnlyCommand("which ls"))
        assertTrue(SecurityUtil.isReadOnlyCommand("ps aux"))
        assertTrue(SecurityUtil.isReadOnlyCommand("top -n 1"))
        assertTrue(SecurityUtil.isReadOnlyCommand("df -h"))
        assertTrue(SecurityUtil.isReadOnlyCommand("du -sh"))
    }

    @Test
    fun testIsReadOnlyCommandWithUnsafeCommands() {
        assertFalse(SecurityUtil.isReadOnlyCommand("rm file.txt"))
        assertFalse(SecurityUtil.isReadOnlyCommand("rmdir dir"))
        assertFalse(SecurityUtil.isReadOnlyCommand("del file.txt"))
        assertFalse(SecurityUtil.isReadOnlyCommand("format /dev/sda"))
        assertFalse(SecurityUtil.isReadOnlyCommand("shutdown"))
        assertFalse(SecurityUtil.isReadOnlyCommand("reboot"))
        assertFalse(SecurityUtil.isReadOnlyCommand("halt"))
        assertFalse(SecurityUtil.isReadOnlyCommand("poweroff"))
        assertFalse(SecurityUtil.isReadOnlyCommand("sudo ls"))
        assertFalse(SecurityUtil.isReadOnlyCommand("su root"))
        assertFalse(SecurityUtil.isReadOnlyCommand("passwd"))
        assertFalse(SecurityUtil.isReadOnlyCommand("chpasswd"))
        assertFalse(SecurityUtil.isReadOnlyCommand("chmod 755 file.txt"))
        assertFalse(SecurityUtil.isReadOnlyCommand("chown user file.txt"))
        assertFalse(SecurityUtil.isReadOnlyCommand("chgrp group file.txt"))
        assertFalse(SecurityUtil.isReadOnlyCommand("dd if=/dev/zero of=file.txt"))
        assertFalse(SecurityUtil.isReadOnlyCommand("shred file.txt"))
        assertFalse(SecurityUtil.isReadOnlyCommand("wipe file.txt"))
    }

    @Test
    fun testIsReadOnlyCommandWithMixedCommands() {
        assertFalse(SecurityUtil.isReadOnlyCommand("ls && rm file.txt"))
        assertFalse(SecurityUtil.isReadOnlyCommand("cat file.txt; rm file.txt"))
        assertFalse(SecurityUtil.isReadOnlyCommand("grep pattern file.txt || shutdown"))
        assertFalse(SecurityUtil.isReadOnlyCommand("find . -name '*.txt' && chmod 755 file.txt"))
    }

    @Test
    fun testIsReadOnlyCommandWithEmptyOrCommentOnlyCommands() {
        assertTrue(SecurityUtil.isReadOnlyCommand(""))
        assertTrue(SecurityUtil.isReadOnlyCommand("# This is a comment"))
        assertTrue(SecurityUtil.isReadOnlyCommand("   # Another comment   "))
    }

    @Test
    fun testIsReadOnlyCommandWithComplexSafeCommands() {
        assertTrue(SecurityUtil.isReadOnlyCommand("ls -la | grep txt"))
        assertTrue(SecurityUtil.isReadOnlyCommand("cat file1.txt file2.txt > output.txt"))
        assertTrue(SecurityUtil.isReadOnlyCommand("find . -name '*.kt' -exec cat {} \\;"))
        assertTrue(SecurityUtil.isReadOnlyCommand("ps aux | grep java"))
        assertTrue(SecurityUtil.isReadOnlyCommand("cat file.txt | less"))
        assertTrue(SecurityUtil.isReadOnlyCommand("ls | head -n 10"))
    }
    
    @Test
    fun testIsReadOnlyCommandWithUnsafePipelineCommands() {
        assertFalse(SecurityUtil.isReadOnlyCommand("cat hello.txt | rm"))
        assertFalse(SecurityUtil.isReadOnlyCommand("ls | grep txt && rm found.txt"))
        assertFalse(SecurityUtil.isReadOnlyCommand("find . -name '*.tmp' | xargs rm"))
        assertFalse(SecurityUtil.isReadOnlyCommand("cat file.txt | sh"))
    }
    
    @Test
    fun testGetCommandFirstWordsWithPipeline() {
        SecurityUtil.getCommandFirstWords("cat hello.txt | rm").let { commands ->
            assertTrue(commands.contains("cat"))
            assertTrue(commands.contains("rm"))
        }
        
        SecurityUtil.getCommandFirstWords("ls -la | grep txt && echo 'done'").let { commands ->
            assertTrue(commands.contains("ls"))
            assertTrue(commands.contains("grep"))
            assertTrue(commands.contains("echo"))
        }
    }
    
    @Test
    fun testIsBannedCommand() {
        // Test banned commands
        assertTrue(SecurityUtil.isBannedCommand("rm file.txt"))
        assertTrue(SecurityUtil.isBannedCommand("rmdir dir"))
        assertTrue(SecurityUtil.isBannedCommand("del file.txt"))
        assertTrue(SecurityUtil.isBannedCommand("format /dev/sda"))
        assertTrue(SecurityUtil.isBannedCommand("shutdown"))
        assertTrue(SecurityUtil.isBannedCommand("reboot"))
        assertTrue(SecurityUtil.isBannedCommand("halt"))
        assertTrue(SecurityUtil.isBannedCommand("poweroff"))
        assertTrue(SecurityUtil.isBannedCommand("sudo ls"))
        assertTrue(SecurityUtil.isBannedCommand("su root"))
        assertTrue(SecurityUtil.isBannedCommand("passwd"))
        assertTrue(SecurityUtil.isBannedCommand("chpasswd"))
        assertTrue(SecurityUtil.isBannedCommand("chmod 755 file.txt"))
        assertTrue(SecurityUtil.isBannedCommand("chown user file.txt"))
        assertTrue(SecurityUtil.isBannedCommand("chgrp group file.txt"))
        assertTrue(SecurityUtil.isBannedCommand("dd if=/dev/zero of=file.txt"))
        assertTrue(SecurityUtil.isBannedCommand("shred file.txt"))
        assertTrue(SecurityUtil.isBannedCommand("wipe file.txt"))
        
        // Test safe commands
        assertFalse(SecurityUtil.isBannedCommand("ls"))
        assertFalse(SecurityUtil.isBannedCommand("cat file.txt"))
        assertFalse(SecurityUtil.isBannedCommand("grep pattern file.txt"))
        assertFalse(SecurityUtil.isBannedCommand("find . -name '*.txt'"))
        assertFalse(SecurityUtil.isBannedCommand("pwd"))
        assertFalse(SecurityUtil.isBannedCommand("whoami"))
        assertFalse(SecurityUtil.isBannedCommand("head file.txt"))
        assertFalse(SecurityUtil.isBannedCommand("tail file.txt"))
        assertFalse(SecurityUtil.isBannedCommand("stat file.txt"))
        assertFalse(SecurityUtil.isBannedCommand("which ls"))
        assertFalse(SecurityUtil.isBannedCommand("ps aux"))
        assertFalse(SecurityUtil.isBannedCommand("top -n 1"))
        assertFalse(SecurityUtil.isBannedCommand("df -h"))
        assertFalse(SecurityUtil.isBannedCommand("du -sh"))
        assertFalse(SecurityUtil.isBannedCommand("lsof -i"))
        assertFalse(SecurityUtil.isBannedCommand("ping google.com"))
        assertFalse(SecurityUtil.isBannedCommand("curl http://example.com"))
        assertFalse(SecurityUtil.isBannedCommand("less file.txt"))
        assertFalse(SecurityUtil.isBannedCommand("more file.txt"))
        assertFalse(SecurityUtil.isBannedCommand("wc -l file.txt"))
        assertFalse(SecurityUtil.isBannedCommand("sort file.txt"))
        
        // Test edge cases
        assertFalse(SecurityUtil.isBannedCommand(""))
        assertFalse(SecurityUtil.isBannedCommand("   "))
        assertFalse(SecurityUtil.isBannedCommand("# comment"))
        assertFalse(SecurityUtil.isBannedCommand("echo 'rm'")) // Should not ban if rm is just a string
        assertFalse(SecurityUtil.isBannedCommand("cat ./rm_backup.sh")) // Safe if rm is part of filename
        
        // Test mixed commands
        assertTrue(SecurityUtil.isBannedCommand("ls && rm file.txt"))
        assertTrue(SecurityUtil.isBannedCommand("cat file.txt; shutdown"))
        assertTrue(SecurityUtil.isBannedCommand("grep pattern file.txt || sudo rm -rf /"))
        assertTrue(SecurityUtil.isBannedCommand("find . -name '*.txt' && chmod 755 file.txt"))
        
        // Test pipeline with banned commands
        assertTrue(SecurityUtil.isBannedCommand("cat safe.txt | rm dangerous.txt"))
        assertTrue(SecurityUtil.isBannedCommand("ls | grep pattern && rm file.txt"))
    }
}