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

/**
 * 聊天输入框自动补全项的基础接口，定义补全项的显示名称、图标以及创建 LookupElement 的能力。
 */
interface ChatComposerLookupItem {
    companion object {
        /** 用于在 LookupElement 中存取原始补全项的 Key */
        val KEY: Key<ChatComposerLookupItem> = Key.create("JARVIS_CHAT_COMPOSER_LOOKUP_ITEM")
    }

    /** 补全项显示名称 */
    val displayName: String

    /** 补全项图标 */
    val icon: Icon?

    /**
     * 创建 IntelliJ 平台的 LookupElement 实例。
     *
     * @return 创建的 LookupElement
     */
    fun createLookupElement(): LookupElement

    /**
     * 设置补全项在弹窗中的显示样式。
     *
     * @param element LookupElement 实例
     * @param presentation 显示样式对象
     */
    fun setPresentation(element: LookupElement, presentation: LookupElementPresentation)
}

/**
 * 可执行的补全项接口，扩展自 [ChatComposerLookupItem]，
 * 表示选中后可以执行具体操作的补全项（如插入文件引用）。
 */
interface ChatComposerLookupActionItem : ChatComposerLookupItem {
    /**
     * 选中补全项后执行的操作。
     *
     * @param composer 聊天输入框实例
     */
    fun execute(composer: ChatComposerField)
}

/**
 * 补全项的抽象基类，提供 [createLookupElement] 和 [setPresentation] 的默认实现，
 * 子类只需实现 [getLookupString] 即可。
 */
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

    /**
     * 获取补全项的查找字符串，用于匹配用户输入。
     *
     * @return 查找字符串
     */
    abstract fun getLookupString(): String
}

/**
 * 文件引用补全项，表示一个可在输入框中引用的文件。
 *
 * @param project 当前项目实例
 * @param file 目标虚拟文件
 */
class FileReferenceLookupItem(
    private val project: Project,
    private val file: VirtualFile,
) : BaseLookupItem(), ChatComposerLookupActionItem {
    /** 文件完整路径 */
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

/**
 * 文件夹引用补全项，表示一个可在输入框中引用的文件夹。
 *
 * @param project 当前项目实例
 * @param folder 目标虚拟文件夹
 */
class FolderReferenceLookupItem(
    private val project: Project,
    private val folder: VirtualFile,
) : BaseLookupItem(), ChatComposerLookupActionItem {
    /** 文件夹完整路径 */
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

/**
 * 斜杠命令补全项，表示一个可在输入框中选择的斜杠命令。
 *
 * @property command 斜杠命令定义
 */
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
