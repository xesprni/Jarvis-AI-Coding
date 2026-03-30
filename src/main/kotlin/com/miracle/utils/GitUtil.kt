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

/**
 * Git 操作工具类，提供 Git 仓库信息查询和 Diff 生成功能
 */
object GitUtil {

    private val logger = thisLogger()

    /**
     * 从 Windows 注册表中查找 Git Bash 的安装路径
     * @return bash.exe 的绝对路径，未找到时返回 null
     */
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

    /**
     * 通过 PATH 环境变量查找 Git Bash 的安装路径
     * @return bash.exe 的绝对路径，未找到时返回 null
     */
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

    /**
     * 获取项目对应的 Git 仓库
     * @param project 当前项目
     * @return GitRepository 实例，未找到时返回 null
     */
    @Throws(VcsException::class)
    @JvmStatic
    fun getProjectRepository(project: Project): GitRepository? {
        val repositoryManager = project.service<GitRepositoryManager>()
        val projectDir = project.guessProjectDir()
        return projectDir?.let { repositoryManager.getRepositoryForFile(it) }
            ?: repositoryManager.repositories.firstOrNull()
    }

    /**
     * 获取项目 Git 仓库的根目录路径
     * @param project 当前项目
     * @return Git 根目录路径，未找到时返回 null
     */
    @JvmStatic
    fun getGitRoot(project: Project): String? {
        val repositoryManager = project.service<GitRepositoryManager>()
        val repositories = repositoryManager.repositories
        return repositories.firstOrNull()?.let { return it.root.path }
    }

    /**
     * 获取当前未提交的变更 Diff（Unified 格式）
     * @param project 当前项目
     * @return Diff 字符串，最多返回 16000 字符
     */
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

    /**
     * 根据哈希列表获取 Git 提交详情
     * @param project 当前项目
     * @param repository Git 仓库
     * @param commitHashes 提交哈希列表
     * @return 匹配的 GitCommit 列表
     */
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

    /**
     * 获取指定提交的 Diff 内容
     * @param project 当前项目
     * @param gitRepository Git 仓库
     * @param commitHash 提交哈希
     * @return 过滤后的 Diff 行列表
     */
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

    /**
     * 遍历仓库中的所有提交
     * @param project 当前项目
     * @param repository Git 仓库
     * @param onVisit 提交访问回调
     */
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

    /**
     * 获取仓库中最近的提交列表
     * @param project 当前项目
     * @param repository Git 仓库
     * @param searchText 搜索关键词，可匹配哈希或提交消息
     * @param limit 最大返回数量，默认 250
     * @return 匹配的 GitCommit 列表
     */
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

    /**
     * 过滤 Diff 输出，移除元数据行
     * @param output 原始 Diff 行列表
     * @return 过滤后的行列表
     */
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

    /**
     * 清理 Diff 字符串，移除不必要的行
     * @param showContext 是否保留上下文行
     * @return 清理后的 Diff 字符串
     */
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
