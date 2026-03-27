package com.miracle.utils

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.diff.impl.patch.IdeaTextPatchBuilder
import com.intellij.openapi.diff.impl.patch.UnifiedDiffWriter
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.ChangeListManager
import git4idea.GitCommit
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import java.io.File
import java.io.StringWriter

object GitUtil {

    private val logger = thisLogger()

    fun findGitBashPathFromRegistry(): String? {
        val regCmd = arrayOf("reg", "query", "HKLM\\SOFTWARE\\GitForWindows", "/v", "InstallPath")
        val process = ProcessBuilder(*regCmd).start()
        val output = process.inputStream.bufferedReader().readText()
        val match = Regex("InstallPath\\s+REG_SZ\\s+(.*)").find(output)
        return match?.groupValues?.get(1)?.trim()?.let {gitRoot ->
            // 可能的 bash.exe 路径
            val possibleBashPaths = listOf(
                File(gitRoot, "bin\\bash.exe"),
                File(gitRoot, "usr\\bin\\bash.exe")
            )

            // 返回第一个存在的
            possibleBashPaths.firstOrNull { it.exists() }?.absolutePath
        }
    }

    fun findGitBashPathFromPath(): String? {
        return try {
            val process = ProcessBuilder("where", "git")
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()

            if (
                exitCode != 0 ||
                output.isBlank() ||
                output.contains("无法找到", ignoreCase = true) ||
                output.contains("Could not find", ignoreCase = true)
            ) {
                null
            } else {
                // 找到 git.exe 的路径
                val gitExe = output.lines()
                    .map { it.trim() }
                    .firstOrNull { it.isNotEmpty() && File(it).exists() }
                    ?: return null

                // Git 根目录
                val gitRoot = File(gitExe).parentFile.parentFile

                // 可能的 bash.exe 路径
                val possibleBashPaths = listOf(
                    File(gitRoot, "bin\\bash.exe"),
                    File(gitRoot, "usr\\bin\\bash.exe")
                )

                // 返回第一个存在的
                possibleBashPaths.firstOrNull { it.exists() }?.absolutePath
            }
        } catch (_: Exception) {
            null
        }
    }

    @Throws(VcsException::class)
    @JvmStatic
    fun getProjectRepository(project: Project): GitRepository? {
        val repositoryManager = project.service<GitRepositoryManager>()
        val projectDir = project.guessProjectDir()
        return projectDir?.let { repositoryManager.getRepositoryForFile(it) }
            ?: repositoryManager.repositories.firstOrNull()
    }

    @JvmStatic
    fun getGitRoot(project: Project): String? {
        val repositoryManager = project.service<GitRepositoryManager>()
        val repositories = repositoryManager.repositories
        return repositories.firstOrNull()?.let { return it.root.path }
    }

    fun getCurrentChanges(project: Project): String? {
        try {
            val repoRootPath = project.basePath?.toNioPathOrNull() ?: return null
            val changes = ChangeListManager.getInstance(project).allChanges
                .filter { change ->
                    change.virtualFile?.let { !it.fileType.isBinary } ?: false
                }

            val patches = IdeaTextPatchBuilder.buildPatch(
                project, changes, repoRootPath, false, true
            ).sortedByDescending { patch ->
                patch.afterVersionId?.let {
                    it.substringAfter("(date ")
                        .substringBefore(")")
                        .toLongOrNull() ?: 0L
                } ?: 0L
            }
            val diffWriter = StringWriter()
            UnifiedDiffWriter.write(
                null, repoRootPath, patches, diffWriter, "\n\n", null, null
            )
            return diffWriter.toString().take(16_000)
        } catch (e: VcsException) {
            logger.warn("Failed to get git context", e)
            return null
        }
    }

    @Throws(VcsException::class)
    fun getCommitsForHashes(
        project: Project,
        repository: GitRepository,
        commitHashes: List<String>
    ): List<GitCommit> {
        val result = mutableListOf<GitCommit>()

        GitHistoryUtils
            .loadDetails(project, repository.root, { commit ->
                if (commitHashes.contains(commit.id.asString())) {
                    result.add(commit)
                }
            })

        return result
    }

    @Throws(VcsException::class)
    fun getCommitDiffs(
        project: Project,
        gitRepository: GitRepository,
        commitHash: String
    ): List<String> {
        val handler = GitLineHandler(project, gitRepository.root, GitCommand.SHOW)
        handler.addParameters(
            commitHash,
            "--unified=2",
            "--no-prefix",
            "--no-color"
        )

        val commandResult = Git.getInstance().runCommand(handler)
        return filterDiffOutput(commandResult.output)
    }

    @Throws(VcsException::class)
    fun visitRepositoryCommits(
        project: Project,
        repository: GitRepository,
        onVisit: (GitCommit) -> Unit
    ) {
        try {
            GitHistoryUtils.loadDetails(project, repository.root, { onVisit(it) })
        } catch (e: VcsException) {
            logger.warn("Error fetching commit history: ${e.message}")
        }
    }

    @Throws(VcsException::class)
    fun getAllRecentCommits(
        project: Project,
        repository: GitRepository,
        searchText: String? = "",
        limit: Int = 250
    ): List<GitCommit> {
        val result = mutableListOf<GitCommit>()

        try {
            GitHistoryUtils
                .loadDetails(project, repository.root, { commit ->
                    if (searchText.isNullOrEmpty()) {
                        result.add(commit)
                    } else {
                        if (commit.id.asString().contains(searchText, true)
                            || commit.fullMessage.contains(searchText, true)
                        ) {
                            result.add(commit)
                        }
                    }
                }, "-n", "$limit")
        } catch (e: VcsException) {
            logger.warn("Error fetching commit history: ${e.message}")
        }

        return result
    }

    private fun filterDiffOutput(output: List<String>): List<String> {
        return output.filter {
            !it.startsWith("diff --git") &&
                    !it.startsWith("index ") &&
                    !it.startsWith("---") &&
                    !it.startsWith("+++") &&
                    !it.startsWith("- ") &&
                    !it.startsWith("commit ")
        }
    }

    private fun String.cleanDiff(showContext: Boolean = false): String =
        lineSequence()
            .filterNot { line ->
                line.startsWith("index ") ||
                        line.startsWith("diff --git") ||
                        line.startsWith("---") ||
                        line.startsWith("+++") ||
                        line.startsWith("===") ||
                        line.contains("\\ No newline at end of file")
                        (!showContext && line.startsWith(" "))
            }
            .joinToString("\n")

    /**
     * 获取 Git Status (--short)
     */
    fun getRepoStatusDesc(project: Project): String {
        val repo = getProjectRepository(project) ?: return ""

        val handler = GitLineHandler(project, repo.root, GitCommand.STATUS).apply {
            addParameters("--short")
        }

        val result = Git.getInstance().runCommand(handler)
        return result.getOutputOrThrow().trim()
    }


    /**
     * 获取最近 5 条 commit（hash + subject）
     */
    fun getRecentCommitDesc(project: Project): String {
        val repo = getProjectRepository(project) ?: return ""

        val handler = GitLineHandler(project, repo.root, GitCommand.LOG).apply {
            addParameters("-5")
            addParameters("--pretty=format:%h %s")
        }

        val result = Git.getInstance().runCommand(handler)
        return result.getOutputOrThrow().trim()
    }
}
