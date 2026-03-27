package com.qihoo.finance.lowcode.smartconversation.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.qifu.ui.smartconversation.psistructure.ClassStructure
import com.qifu.ui.smartconversation.psistructure.ClassStructureSerializer
import com.qifu.ui.smartconversation.psistructure.PsiStructureRepository
import com.qifu.ui.smartconversation.psistructure.PsiStructureState
import com.qifu.utils.coroutines.CoroutineDispatchers
import com.qifu.utils.coroutines.DisposableCoroutineScope
import com.qihoo.finance.lowcode.smartconversation.utils.EncodingManager
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class PsiStructureTotalTokenProvider(
    parentDisposable: Disposable,
    private val classStructureSerializer: ClassStructureSerializer,
    private val encodingManager: EncodingManager,
    dispatchers: CoroutineDispatchers,
    psiStructureRepository: PsiStructureRepository,
    onPsiTokenHandled: (Int) -> Unit
) {

    private val coroutineScope = DisposableCoroutineScope()

    init {
        Disposer.register(parentDisposable, coroutineScope)
        psiStructureRepository.structureState
            .map { structureState ->
                when (structureState) {
                    is PsiStructureState.Content -> {
                        getPsiTokensCount(structureState.elements)
                    }

                    PsiStructureState.Disabled -> 0

                    is PsiStructureState.UpdateInProgress -> 0
                }
            }
            .flowOn(dispatchers.io())
            .onEach { psiTokens ->
                onPsiTokenHandled(psiTokens)
            }
            .launchIn(coroutineScope)
    }

    private fun getPsiTokensCount(psiStructureSet: Set<ClassStructure>): Int =
        psiStructureSet
            .joinToString(separator = "\n\n") { psiStructure ->
                classStructureSerializer.serialize(psiStructure)
            }
            .let { serializedPsiStructure ->
                encodingManager.countTokens(serializedPsiStructure)
            }
}