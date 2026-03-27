package com.qifu.utils

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.qifu.external.LowcodeApi
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState
import com.qihoo.finance.lowcode.common.constants.Constants
import com.qihoo.finance.lowcode.common.util.GitUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.concurrent.Executors

@Serializable
data class AgentConfigInfo(

    // 配置作用域(GLOBAL/PROJECT)
    val configScope: String,

    // 配置类型（SUBAGENT、MCP、SKill、AutoApproveSetting等）
    val configType: String,

    // 配置名称（如：subagent_name、mcp_name、skill_name等）
    val configName: String,

    // 配置来源（JARVIS_IDE、JET_BRAIN_PLUGIN等）
    val source: String,

    // 项目名
    val owner: String,

    // git仓库名，统一转换成ssh://的格式
    val gitRepo: String

) {
    enum class TYPE(val value: String) {
        AGENT("agent"),
        MCP("mcp"),
        SKILL("skill"),
        AUTO_APPROVE_SETTING("autoApproveSetting"),
        ;

        companion object {
            fun fromString(value: String): TYPE? {
                return entries.find { it.value.equals(value, ignoreCase = true) }
            }
        }
    }
}

@Serializable
data class AgentConfigFileInfo(
    var configUuid: String,
    val fileName: String,
    val fileValue: String,
    val filePath: String,
    val fileLastModified: String,
    )

@Serializable
data class AgentConfigUnion(
    val config: AgentConfigInfo,
    val configFiles: MutableList<AgentConfigFileInfo>
)

@Serializable
data class ConfigDeleteInfo(
    val configUuid: String,
    val fileIdList: MutableList<String>
)


object AgentConfigTraceUtil {
    private val LOG = Logger.getInstance(AgentLoader::class.java)
    private val SCOPE = CoroutineScope(
        SupervisorJob() + Executors.newSingleThreadExecutor {
            Thread(it, "AgentConfigTraceUtils-Processor").apply { isDaemon = true }
        }.asCoroutineDispatcher()
    )

    /**
     * 附带文件的配置同步
     */
    fun executeSync(
        config: AgentConfigInfo,
        fileList: MutableList<AgentConfigFileInfo>,
    ) {
        SCOPE.launch {
            // 1. 先执行获取jarvis_uuid配置
            try {
                val configUuid = retryUtils.retry(3) {
                    LowcodeApi.insertAgentConfig(config)
                }
                if (configUuid == "") {
                    throw Exception("get configUuid error")
                }
                val fileIdList = mutableListOf<String>()
                // 2. 执行逐个文件上传
                for (file in fileList) {
                    file.configUuid = configUuid
                    val configId = retryUtils.retry(3) {
                        LowcodeApi.insertAgentConfigFile(file)
                    }
                    if (configId != "") {
                        fileIdList.add(configId)
                    }
                }
                // 3. 执行删除操作
                retryUtils.retry(3) {
                    LowcodeApi.deleteAgentConfigFile(ConfigDeleteInfo(configUuid, fileIdList))
                }
            } catch (e: Exception) {
//                LOG.warn("AgentConfigTraceUtil executeSync error: ${e.message}")
            }
        }
    }

    fun traceAgentConfig(
        agentConfig: AgentConfig,
        agentFilePath: String,
        project: Project,
    ) {
        try {
            if (UserInfoPersistentState.getUserInfo().email == null) {
                return
            }
            // 1. 构造
            val configUnion = buildAgentConfig(agentConfig, agentFilePath, project)
            // 2. 传递
            executeSync(configUnion.config, configUnion.configFiles)
        } catch (e: Exception) {
            LOG.warn("AgentConfigTraceUtil traceAgentConfig error: ${e.message}")
        }
    }

    fun buildAgentConfig(agentConfig: AgentConfig, configPath: String, project: Project): AgentConfigUnion {
        val isUserScope = agentConfig.scope == AgentConfig.Scope.USER
        val configScope = if (isUserScope) "GLOBAL" else agentConfig.scope.name
        val gitUrl = GitUtils.getGitUrl(project)
        val owner = if (isUserScope) {
            UserInfoPersistentState.getUserInfo().email
        } else {
            gitUrl.split("/").takeLast(2).joinToString("/").removeSuffix(".git")
        }

        val config = AgentConfigInfo(
            configScope = configScope,
            configType = AgentConfigInfo.TYPE.AGENT.name,
            configName = agentConfig.agentType,
            source = Constants.BusinessConstant.SOURCE,
            owner = owner,
            gitRepo = gitUrl
        )
        val configFile = AgentConfigFileInfo(
            configUuid = "",
            fileName = agentConfig.agentType,
            fileValue = agentConfig.originalContent.orEmpty(),
            filePath = configPath,
            fileLastModified = agentConfig.lastModifiedTime.orEmpty(),
        )
        return AgentConfigUnion(config, mutableListOf(configFile))
    }

    fun traceSkillConfig(
        scope: SkillConfig.Scope,
        skillList: List<SkillUploadConfig>,
        project: Project,
    ) {
        // 1. 构造
        val configUnion = buildSkillConfig(scope, skillList, project)
        if (configUnion == null)
            return
//        // 2. 传递
        executeSync(configUnion.config, configUnion.configFiles)
    }

    fun buildSkillConfig(scope: SkillConfig.Scope, skillList: List<SkillUploadConfig>, project: Project): AgentConfigUnion? {
        if (skillList.isEmpty()) {
            return null
        }
        val isUserScope = scope == SkillConfig.Scope.USER
        val configScope = if (isUserScope) "GLOBAL" else SkillConfig.Scope.PROJECT.name
        val gitUrl = GitUtils.getGitUrl(project)
        val owner = if (isUserScope) {
            UserInfoPersistentState.getUserInfo().email
        } else {
            gitUrl.split("/").takeLast(2).joinToString("/").removeSuffix(".git")
        }

        // 获取所有 skill 的 name 列表
        val skillNames = skillList.map { it.name }.distinct()
        if (skillNames.isEmpty() || skillNames.size > 1 || skillNames[0] == "") {
            return null
        }

        val config = AgentConfigInfo(
            configScope = configScope,
            configType = AgentConfigInfo.TYPE.SKILL.name,
            configName = skillList.firstOrNull()?.name ?: "default name",
            source = Constants.BusinessConstant.SOURCE,
            owner = owner,
            gitRepo = gitUrl
        )

        val configFiles = skillList.map { file ->
            AgentConfigFileInfo(
                configUuid = "",
                fileName = file.name,
                fileValue = file.content,
                filePath = file.filePath,
                fileLastModified = file.lastModifiedTime,
            )
        }
        return AgentConfigUnion(config, configFiles.toMutableList())

    }

    fun traceMcpConfig() {
        // 1. 构造

        // 2. 传递
    }
}

