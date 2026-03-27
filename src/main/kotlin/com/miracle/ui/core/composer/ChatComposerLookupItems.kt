package com.miracle.ui.core.composer

import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.util.Key
import com.intellij.ui.JBColor
import java.awt.Color
import javax.swing.Icon

interface ChatComposerLookupItem {
    companion object {
        val KEY: Key<ChatComposerLookupItem> = Key.create("JARVIS_CHAT_COMPOSER_LOOKUP_ITEM")
    }

    val displayName: String
    val icon: Icon?

    fun createLookupElement(): LookupElement
    fun setPresentation(element: LookupElement, presentation: LookupElementPresentation)
}

interface ChatComposerLookupActionItem : ChatComposerLookupItem {
    fun execute(composer: ChatComposerField)
}

abstract class BaseLookupItem : ChatComposerLookupItem {
    override fun createLookupElement(): LookupElement {
        val lookupElement = LookupElementBuilder.create(getLookupString())
            .withPresentableText(displayName)
            .withIcon(icon)
            .withRenderer(object : LookupElementRenderer<LookupElement>() {
                override fun renderElement(element: LookupElement, presentation: LookupElementPresentation) {
                    setPresentation(element, presentation)
                }
            })
            .apply {
                putUserData(ChatComposerLookupItem.KEY, this@BaseLookupItem)
            }
        return PrioritizedLookupElement.withPriority(lookupElement, 1.0)
    }

    override fun setPresentation(element: LookupElement, presentation: LookupElementPresentation) {
        presentation.icon = icon
        presentation.itemText = displayName
    }

    abstract fun getLookupString(): String
}

class FileReferenceLookupItem(
    private val project: Project,
    private val file: VirtualFile,
) : BaseLookupItem(), ChatComposerLookupActionItem {
    val filePath: String = file.path
    override val displayName: String = file.name
    override val icon: Icon = file.fileType.icon ?: AllIcons.FileTypes.Any_type

    override fun getLookupString(): String = file.name

    override fun setPresentation(element: LookupElement, presentation: LookupElementPresentation) {
        super.setPresentation(element, presentation)
        val projectDir = project.guessProjectDir()
        presentation.typeText = if (projectDir != null) {
            VfsUtil.getRelativePath(file, projectDir) ?: file.path
        } else {
            file.path
        }
        presentation.isTypeGrayed = true
    }

    override fun execute(composer: ChatComposerField) {
        composer.insertFileReference(file.name, file.path)
    }
}

class FolderReferenceLookupItem(
    private val project: Project,
    private val folder: VirtualFile,
) : BaseLookupItem(), ChatComposerLookupActionItem {
    val folderPath: String = folder.path
    override val displayName: String = folder.name
    override val icon: Icon = AllIcons.Nodes.Folder

    override fun getLookupString(): String = folder.name

    override fun setPresentation(element: LookupElement, presentation: LookupElementPresentation) {
        super.setPresentation(element, presentation)
        val projectDir = project.guessProjectDir()
        presentation.typeText = if (projectDir != null) {
            VfsUtil.getRelativePath(folder, projectDir) ?: folder.path
        } else {
            folder.path
        }
        presentation.isTypeGrayed = true
    }

    override fun execute(composer: ChatComposerField) {
        composer.insertFolderReference(folder.name, folder.path)
    }
}

class SlashCommandLookupItem(
    val command: SlashCommand,
) : BaseLookupItem() {
    override val displayName: String = command.command
    override val icon: Icon = command.icon ?: AllIcons.Actions.Execute

    override fun getLookupString(): String = command.command

    override fun setPresentation(element: LookupElement, presentation: LookupElementPresentation) {
        presentation.icon = icon
        presentation.itemText = command.command
        if (command.argumentTemplates.isNotEmpty()) {
            presentation.appendTailText("  ${command.argumentTemplates.joinToString(" ")}", false)
        }
        if (command.description.isNotBlank()) {
            presentation.appendTailText("  ${command.description}", true)
        }
        presentation.typeText = command.category.displayName
        presentation.isTypeGrayed = true
        presentation.isItemTextBold = true
        presentation.itemTextForeground = when (command.category) {
            SlashCommandCategory.BUILT_IN -> JBColor(Color(0x2E7D32), Color(0x81C784))
            SlashCommandCategory.SKILL -> JBColor(Color(0x1565C0), Color(0x90CAF9))
        }
    }
}
