package com.qifu.ui.utils

import com.intellij.openapi.components.service
import com.qihoo.finance.lowcode.smartconversation.service.ReferencedFile
import com.qifu.ui.smartconversation.psistructure.ClassStructure
import com.qifu.ui.smartconversation.psistructure.ClassStructureSerializer
import com.qifu.utils.addLineNumbers
import com.qifu.utils.file.FileUtil
import com.qifu.utils.toRelativePath
import com.qifu.utils.truncateToCharBudget
import com.qihoo.finance.lowcode.smartconversation.settings.IncludedFilesSettings
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.util.stream.Collectors
import kotlin.io.path.Path
import kotlin.io.path.extension

object CompletionRequestUtil {

    private val psiStructureSerializer = ClassStructureSerializer
    private const val MAX_FILE_LENGTH = 10_000

    private val PSI_STRUCTURE_TITLE = """
        The following is the structure of the file dependencies that were attached above. 
        The structure contains a description of classes with their methods, method arguments, and return types.  
        If the type is specified as TypeUnknown, then the analyzer could not identify the type, 
        try to take it out of context, if necessary for the response.
    """.trimIndent()

    @JvmStatic
    fun formatCode(code: String, filePath: String? = null): String {
        val header = filePath?.let { "${FileUtil.getFileExtension(it)}:$it" } ?: ""
        return buildString {
            append("```${header}\n")
            append("$code\n")
            append("```\n")
        }
    }

    @JvmStatic
    fun formatCodeBlock(filePath: String, projectBasePath: String, fullLineText: String, lineStart: Int, lineEnd: Int): String {
        val fileExt = Path(filePath).extension
        var content = addLineNumbers(fullLineText, lineStart)
        content = truncateToCharBudget(content, MAX_FILE_LENGTH)
        return buildString {
//            append("\n**File:** `${toRelativePath(filePath, projectBasePath)}`\n")
//            append("**Lines:** $lineStart-$lineEnd\n\n")
            append("\n")
            append("```$fileExt:$filePath\n")
            append("$content\n")
            append("```\n")
        }
    }

    @JvmStatic
    fun formatCodeWithLanguage(code: String, language: String): String {
        return buildString {
            append("```${language}\n")
            append("$code\n")
            append("```\n")
        }
    }

    @JvmStatic
    fun getPromptWithContext(
        referencedFiles: List<ReferencedFile>,
        userPrompt: String?,
        psiStructure: Set<ClassStructure>?
    ): String {
        val includedFilesSettings = service<IncludedFilesSettings>().state
        val repeatableContext = includedFilesSettings.repeatableContext
        val fileContext = referencedFiles.stream()
            .map { item: ReferencedFile ->
                formatCode(item.fileContent(), item.filePath())
            }
            .collect(Collectors.joining("\n\n"))

        val structureContext = psiStructure
            ?.map { structure: ClassStructure ->
                formatCode(
                    psiStructureSerializer.serialize(structure),
                    structure.virtualFile.path
                )
                repeatableContext
                    .replace("{FILE_PATH}", structure.virtualFile.path)
                    .replace(
                        "{FILE_CONTENT}",
                        formatCode(
                            psiStructureSerializer.serialize(structure),
                            structure.virtualFile.path
                        )
                    )
            }
            ?.ifNotEmpty {
                joinToString(
                    prefix = "\n\n" + PSI_STRUCTURE_TITLE + "\n\n",
                    separator = "\n\n"
                ) { it }
            }

        return includedFilesSettings.promptTemplate
            .replace("{REPEATABLE_CONTEXT}", fileContext + structureContext.orEmpty())
            .replace("{QUESTION}", userPrompt!!)
    }
}