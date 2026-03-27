package com.qifu.ui.smartconversation.textarea.lookup.group


import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.NameUtil
import com.qifu.services.AgentService
import com.qifu.ui.smartconversation.textarea.header.TagManager
import com.qifu.ui.smartconversation.textarea.lookup.DynamicLookupGroupItem
import com.qifu.ui.smartconversation.textarea.lookup.LookupActionItem
import com.qifu.ui.smartconversation.textarea.lookup.LookupUtil
import com.qifu.ui.smartconversation.textarea.lookup.action.AgentActionItem
import com.qifu.utils.AgentConfig
import com.qihoo.finance.lowcode.common.util.IconUtil
import com.qihoo.finance.lowcode.common.util.Icons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AgentGroupItem(
    private val project: Project,
    private val tagManager: TagManager
) : AbstractLookupGroupItem(), DynamicLookupGroupItem {

    override val displayName: String = "Agent"
    override val icon = IconUtil.getThemeAwareIcon(Icons.AI_AGENT_LIGHT, Icons.AI_AGENT)

    override suspend fun updateLookupList(lookup: LookupImpl, searchText: String) {
        withContext(Dispatchers.Default) {
            val agents = project.service<AgentService>().agentLoader.getActiveAgents()
            agents.forEach { agent ->
                run {
                    val tools = if (agent.tools is List<*>) agent.tools.joinToString(", ") else agent.tools.toString()
                    "- ${agent.agentType}: ${agent.whenToUse} (Tools: $tools)"
                    if (tools.contains(searchText, true)) {
                        runInEdt {
                            LookupUtil.addLookupItem(lookup, AgentActionItem(agent.agentType,agent.whenToUse))
                        }
                    }
                }
            }
        }
    }

    override suspend fun getLookupItems(searchText: String): List<LookupActionItem> {

        return withContext(Dispatchers.Default) {
            val agents = project.service<AgentService>().agentLoader.getActiveAgents()
            val matcher = NameUtil.buildMatcher("*$searchText").build()
            val matchingAgents = mutableListOf<AgentConfig>()
            agents.forEach { agent ->
                if ((searchText.isEmpty() || matcher.matchingDegree(agent.agentType) != Int.MIN_VALUE)
                ) {
                    matchingAgents.add(agent)
                }
            }
            matchingAgents.take(10).map { AgentActionItem(it.agentType,it.whenToUse) }
        }
    }
}