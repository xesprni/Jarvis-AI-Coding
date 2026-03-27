package com.qifu.ui.smartconversation.settings.service

import com.qihoo.finance.lowcode.common.util.Icons
import javax.swing.Icon

object JarvisAvailableModels {

    val DEFAULT_CODE_MODEL = JarvisModel("Qwen 2.5 Coder", "qwen-2.5-32b-code", Icons.Qwen)

    @JvmStatic
    val ALL_CHAT_MODELS: List<JarvisModel> = listOf(
        JarvisModel("o4-mini", "o4-mini", Icons.OpenAI),
        JarvisModel("GPT-5", "gpt-5", Icons.OpenAI),
        JarvisModel("GPT-5 Mini", "gpt-5-mini", Icons.OpenAI),
        JarvisModel("Qwen3 Coder", "qwen3-coder", Icons.Qwen),
    )

    @JvmStatic
    val ALL_CODE_MODELS: List<JarvisModel> = listOf(
        DEFAULT_CODE_MODEL,
        JarvisModel("GPT-3.5 Turbo Instruct", "gpt-3.5-turbo-instruct", Icons.OpenAI),
    )

    @JvmStatic
    fun findByCode(code: String?): JarvisModel? {
        return ALL_CHAT_MODELS.union(ALL_CODE_MODELS).firstOrNull { it.code == code }
    }
}

data class JarvisModel(
    val name: String,
    val code: String,
    val icon: Icon,
)
