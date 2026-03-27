package com.qifu.ui.smartconversation.psistructure

data class MethodStructure(
    val name: String,
    val returnType: ClassName,
    val parameters: List<ParameterInfo>,
    val modifiers: List<String>,
)
