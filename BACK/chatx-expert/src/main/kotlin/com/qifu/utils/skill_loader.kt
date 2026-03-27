package com.qifu.utils

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.qifu.config.AgentSettings
import com.qifu.utils.SkillConfig.Scope
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.NameFileFilter
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.io.monitor.FileAlterationListener
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class SkillConfig(
    val name: String,               // skill name
    val description: String,        // Description of the skill
    var content: String,            // skill content
    var filePath: String,
    val baseDir: String,
    val argumentHints: List<String> = emptyList(),
    val scope: Scope,         // 'built-in' | 'user' | 'project'
) {
    enum class Scope(val value: String) {
        USER("user"),
        PROJECT("project"),
        ;
    }
}

data class SkillUploadConfig(
    val name: String,
    val content: String,
    val filePath: String,
    val lastModifiedTime: String,
)


class SkillLoader {

    companion object {
        private val LOG = thisLogger()
    }
    
    private val yaml = Yaml()
    private val skillCache = ConcurrentHashMap<String, List<SkillConfig>>()
    private val changeListeners = CopyOnWriteArrayList<() -> Unit>()
    private var monitor: FileAlterationMonitor? = null

    fun scanSkillDirectory(dirPath: Path, scope: Scope, isRecordConfig: Boolean, project: Project?): List<SkillConfig> {
        val dir = dirPath.toFile()
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        val skillFiles = FileUtils.listFiles(
            dir,
            NameFileFilter("SKILL.md"),
            TrueFileFilter.INSTANCE
        )

        return skillFiles.mapNotNull { parseSkillFile(it, scope, isRecordConfig, project) }
    }

    private fun uploadSkill(dirPath: String, skillName: String, scope: Scope, project: Project?) {
        try {
            if (UserInfoPersistentState.getUserInfo().email == null) {
                return
            }
            // 1. 遍历文件夹内的所有文件
            val dir = File(dirPath)
            if (!dir.exists() || !dir.isDirectory) {
                LOG.warn("Invalid directory path: $dirPath")
                return
            }
            // 2. 遍历文件夹内的所有文件
            val list = mutableListOf<SkillUploadConfig>()
            dir.walkTopDown().forEach { file ->
                val isGlobal = scope == Scope.USER
                if (file.isFile) {
                    val filePath = if (isGlobal) {
                        file.absolutePath
                    } else {
                        val projectRoot = GitUtil.getGitRoot(project!!)
                        if (projectRoot != null) {
                            file.relativeTo(File(projectRoot)).path
                        } else {
                            // fixme project级有时候读取不到git路径
                            ""
                        }
                    }
                    if (filePath.isNotBlank()) {
                        val skillUploadConfig = SkillUploadConfig(
                            name = skillName,
                            content = file.readText(),
                            filePath = toPosixPath(filePath),
                            lastModifiedTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(file.lastModified())
                        )
                        list.add(skillUploadConfig)
                    }
                }
            }
            //
            AgentConfigTraceUtil.traceSkillConfig(scope, list, project!!)
        } catch (e: Exception) {
            LOG.warn("Failed to upload skill: ${e.message}")
        }
    }

    private fun parseSkillFile(file: File, scope: Scope, isRecordConfig: Boolean, project: Project?): SkillConfig? {
        return try {
            val text = file.readText()
            val (front, body) = parseFrontmatter(text)
            val name = front["name"]?.toString()
            val desc = front["description"]?.toString()
            val filePath = toPosixPath(file.absolutePath)

            if (name.isNullOrBlank() || desc.isNullOrBlank()) {
                LOG.warn("Skipping ${file.name}: missing required fields (name, description)")
                return null
            }

            val skillBaseDir = toBashPath(file.parent, AgentSettings.state.shellType!!)
            val skillContent = body.replace("{{SKILL_BASE_DIR}}", skillBaseDir).trim()
            val argumentHints = parseArgumentHints(front, desc, body, name)

            // 上传配置
            if (isRecordConfig) {
                uploadSkill(file.parent, name, scope, project)
            }

            SkillConfig(
                name = name,
                description = desc.replace("\\n", "\n"),
                content = skillContent,
                filePath = filePath,
                baseDir = skillBaseDir,
                argumentHints = argumentHints,
                scope = scope,
            )
        } catch (e: Exception) {
            LOG.error("Failed to parse skill file ${file.parent}: ${e.message}")
            null
        }
    }

    private fun parseFrontmatter(content: String): Pair<Map<String, Any>, String> {
        try {
            val parts = content.replace("\r", "").split("---")
            if (parts.size < 3) return emptyMap<String, Any>() to content
            val yamlContent = parts[1]
            val body = parts.drop(2).joinToString("---")
            val data = yaml.load<Map<String, Any>>(yamlContent) ?: emptyMap()
            return data to body
        } catch (e: Exception)  {
            LOG.warn("Failed to parse skill frontmatter: ${e.message}")
            return emptyMap<String, Any>() to ""
        }
    }

    private fun parseArgumentHints(
        front: Map<String, Any>,
        description: String,
        body: String,
        skillName: String,
    ): List<String> {
        val fromFrontmatter = listOf("arguments", "args", "params", "parameters")
            .asSequence()
            .mapNotNull { key -> front[key] }
            .flatMap { value -> flattenArgumentHints(value).asSequence() }
            .toList()

        if (fromFrontmatter.isNotEmpty()) {
            return normalizeArgumentHints(fromFrontmatter)
        }

        return inferArgumentHintsFromText(skillName, description, body)
    }

    private fun flattenArgumentHints(value: Any?): List<String> {
        if (value == null) return emptyList()

        return when (value) {
            is String -> listOf(value)
            is Collection<*> -> value.flatMap { flattenArgumentHints(it) }
            is Map<*, *> -> {
                val explicit = value["value"]
                    ?: value["template"]
                    ?: value["text"]

                if (explicit != null) {
                    flattenArgumentHints(explicit)
                } else {
                    value.mapNotNull { (k, v) ->
                        val key = k?.toString()?.trim().orEmpty()
                        val content = v?.toString()?.trim().orEmpty()
                        when {
                            key.isEmpty() && content.isEmpty() -> null
                            key.isNotEmpty() && content.isNotEmpty() -> "$key=$content"
                            key.isNotEmpty() -> key
                            else -> content
                        }
                    }
                }
            }
            else -> listOf(value.toString())
        }
    }

    private fun normalizeArgumentHints(rawHints: List<String>): List<String> {
        return rawHints
            .mapNotNull { raw ->
                val cleaned = raw.trim('`', '"', '\'', ' ').trim()
                cleaned.ifEmpty { null }
            }
            .distinct()
            .take(6)
    }

    private fun inferArgumentHintsFromText(skillName: String, description: String, body: String): List<String> {
        val skillCommand = "/$skillName"
        val tailRegex = Regex("""${Regex.escape(skillCommand)}\s+(.+)$""")
        val inferred = (description.lines() + body.lines())
            .asSequence()
            .map { it.trim() }
            .mapNotNull { line -> tailRegex.find(line)?.groupValues?.getOrNull(1) }
            .toList()
        return normalizeArgumentHints(inferred)
    }

    data class AllSkills(val activeSkills: List<SkillConfig>, val allSkills: List<SkillConfig>)
    fun loadAllSkills(showAll: Boolean = true, isRecordConfig: Boolean = false, project: Project? = null): AllSkills {
        return try {
            val userConfigDir = getUserConfigDirectory()
            val projectConfigDir = getProjectConfigDirectory()

            val userAgentDir = Paths.get(userConfigDir, "skills")
            val projectAgentDir = Paths.get(projectConfigDir, "skills")

            val userSkills = scanSkillDirectory(userAgentDir, Scope.USER, isRecordConfig, project)
            val projectSkills = scanSkillDirectory(projectAgentDir, Scope.PROJECT, isRecordConfig, project)

            val disabled = AgentSettings.state.disabledSkills.toSet()
            val skillMap = linkedMapOf<String, SkillConfig>()
            (userSkills + projectSkills).forEach { skillMap[it.name] = it }
            val activeSkills = skillMap.values.filter { it.name !in disabled }
            val allSkills = if (!showAll) {
                val filterSkills =  linkedMapOf<String, SkillConfig>()
                (userSkills + projectSkills).forEach { filterSkills[it.name] = it }
                filterSkills.values.toList()
            }else{userSkills + projectSkills}

            AllSkills(activeSkills, allSkills)
        } catch (e: Exception) {
            LOG.warn("Failed to load skills: ${e.message}")
            AllSkills(emptyList(), emptyList())
        }
    }

    fun getActiveSkills(): List<SkillConfig> =
        skillCache.getOrPut("active") {
            loadAllSkills().activeSkills
        }

    suspend fun getAllSkills(): List<SkillConfig> =
        skillCache.getOrPut("all") {
            loadAllSkills().allSkills
        }

    fun clearCache() {
        skillCache.clear()
    }

    fun addChangeListener(disposable: Disposable? = null, listener: () -> Unit) {
        changeListeners.add(listener)
        disposable?.let { Disposer.register(it) { changeListeners.remove(listener) } }
    }

    private fun notifyChange(onChange: (() -> Unit)?) {
        clearCache()
        changeListeners.forEach { listener ->
            runCatching { listener() }.onFailure {
                LOG.warn("Skill change listener failed", it)
            }
        }
        onChange?.invoke()
    }

    fun startWatcher(onChange: (() -> Unit)? = null) {
        stopWatcher()

        val rootDirs = listOf(
            Paths.get(getUserConfigDirectory(), "skills"),
            Paths.get(getProjectConfigDirectory(), "skills"),
        )

        monitor = FileAlterationMonitor(1000).apply {
            rootDirs.forEach { rootDir ->
                rootDir.toFile().mkdirs()
                
                val observer = FileAlterationObserver(rootDir.toFile())
                observer.addListener(object : FileAlterationListener {
                    override fun onStart(observer: FileAlterationObserver?) {}
                    override fun onStop(observer: FileAlterationObserver?) {}
                    override fun onDirectoryCreate(directory: File?) {}
                    override fun onDirectoryChange(directory: File?) {}
                    override fun onDirectoryDelete(directory: File?) {}
                    
                    override fun onFileCreate(file: File?) {
                        if (file?.name == "SKILL.md") {
                            LOG.info("Skill file created: ${file.absolutePath}")
                            notifyChange(onChange)
                        }
                    }
                    
                    override fun onFileChange(file: File?) {
                        if (file?.name == "SKILL.md") {
                            LOG.info("Skill file changed: ${file.absolutePath}")
                            notifyChange(onChange)
                        }
                    }
                    
                    override fun onFileDelete(file: File?) {
                        if (file?.name == "SKILL.md") {
                            LOG.info("Skill file deleted: ${file.absolutePath}")
                            notifyChange(onChange)
                        }
                    }
                })
                
                addObserver(observer)
            }
            start()
        }
    }

    fun stopWatcher() {
        monitor?.let {
            try {
                it.stop()
            } catch (_: Exception) {}
        }
        monitor = null
    }
}
