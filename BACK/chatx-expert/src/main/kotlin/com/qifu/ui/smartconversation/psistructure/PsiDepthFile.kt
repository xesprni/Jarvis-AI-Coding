package com.qifu.ui.smartconversation.psistructure

import com.intellij.psi.PsiFile

data class PsiDepthFile(
    val psiFile: PsiFile,
    val depth: Int,
)