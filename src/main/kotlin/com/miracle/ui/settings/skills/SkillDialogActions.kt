package com.miracle.ui.settings.skills

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.miracle.config.JarvisCoreSettings
import com.miracle.services.AgentService
import com.miracle.utils.file.FileUtil
import com.miracle.utils.getProjectConfigDirectory
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.text.AttributeSet
import javax.swing.text.DocumentFilter
import javax.swing.text.PlainDocument

/**
 * 共用的 Skill 相关对话框操作
 */
object SkillDialogActions {
    fun showAddSkillDialog(project: Project, agentService: AgentService, onSkillCreated: (() -> Unit)? = null) {
        val dialog = AddSkillDialog(project)
        if (!dialog.showAndGet()) {
            return
        }
        val input = dialog.getInput()

        try {
            val projectConfigDir = getProjectConfigDirectory(project)
            val skillDir = Paths.get(projectConfigDir, "skills", input.code)

            Files.createDirectories(skillDir)
            val scriptsDir = skillDir.resolve("scripts")
            val referencesDir = skillDir.resolve("references")
            val assetsDir = skillDir.resolve("assets")
            Files.createDirectories(scriptsDir)
            Files.createDirectories(referencesDir)
            Files.createDirectories(assetsDir)

            val skillMdPath = skillDir.resolve("SKILL.md")
            val settings = JarvisCoreSettings.getInstance()
            val skillMdContent = settings.skillTemplate.replaceFirst("{{SKILL_NAME}}", input.name)
            Files.writeString(skillMdPath, skillMdContent)

            val vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(skillMdPath.toString())
            if (vf != null) {
                FileUtil.reloadFilesFromDisk(vf)
                val descriptor = OpenFileDescriptor(project, vf, 2, 12) // 定位到第三行
                descriptor.navigate(true)
                ProjectView.getInstance(project).select(vf, vf, true)
            }

            agentService.skillLoader.clearCache()
            onSkillCreated?.invoke()
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                "创建 Skill 目录失败：${e.message}",
                "创建 Skill 失败"
            )
        }
    }
}

data class SkillInput(val code: String, val name: String)

/**
 * "添加Skill"对话框
 */
@Suppress("DialogTitleCapitalization")
class AddSkillDialog(val project: Project) : DialogWrapper(project) {

    private val agentService = project.service<AgentService>()
    private val codeField = JBTextField().apply {
        preferredSize = Dimension(JBUI.scale(300), preferredSize.height)
        (document as? PlainDocument)?.documentFilter = CodeInputFilter()
        emptyText.text = "仅支持字母、数字、下划线和短横线"
    }
    private val nameField = JBTextField().apply {
        preferredSize = Dimension(JBUI.scale(300), preferredSize.height)
    }

    init {
        title = "Add Skill"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val form = JPanel(GridBagLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyTop(8)
        }

        val c = GridBagConstraints().apply {
            insets = JBUI.insets(4)
            anchor = GridBagConstraints.NORTHWEST
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
        }

        addRow(form, c, 0, "Skill编码", codeField)
        addRow(form, c, 1, "Skill名称", nameField)

        return form
    }

    override fun doValidate(): ValidationInfo? {
        if (codeField.text.isNullOrBlank()) {
            return ValidationInfo("CODE不能为空", codeField)
        }
        val namePattern = "^[a-zA-Z0-9_-]+$".toRegex()
        if (!namePattern.matches(codeField.text.trim())) {
            return ValidationInfo("CODE只能包含字母、数字、下划线和短横线", codeField)
        }
        val projectConfigDir = getProjectConfigDirectory(project)
        val skillsDir = Paths.get(projectConfigDir, "skills")
        val existingSkillCodes = if (Files.exists(skillsDir)) {
            Files.list(skillsDir).use { stream ->
                stream.filter { Files.isDirectory(it) }
                    .map { it.fileName.toString().lowercase() }
                    .toList()
            }
        } else {
            emptyList()
        }
        if (codeField.text.lowercase().trim() in existingSkillCodes) {
            return ValidationInfo("CODE已存在，请使用其他CODE", codeField)
        }
        if (nameField.text.isNullOrBlank()) {
            return ValidationInfo("NAME不能为空", nameField)
        }
        return null
    }

    fun getInput(): SkillInput {
        return SkillInput(
            code = codeField.text.trim(),
            name = nameField.text.trim()
        )
    }

    private fun addRow(panel: JPanel, c: GridBagConstraints, row: Int, label: String, field: JComponent) {
        val labelConstraints = c.clone() as GridBagConstraints
        labelConstraints.gridx = 0
        labelConstraints.gridy = row
        labelConstraints.weightx = 0.0
        labelConstraints.anchor = GridBagConstraints.CENTER
        panel.add(JBLabel(label), labelConstraints)

        val fieldConstraints = c.clone() as GridBagConstraints
        fieldConstraints.gridx = 1
        fieldConstraints.gridy = row
        fieldConstraints.weightx = 1.0
        panel.add(field, fieldConstraints)
    }
}

/**
 * CODE 字段输入过滤器,只允许字母、数字、下划线和短横线
 */
class CodeInputFilter : DocumentFilter() {
    private val allowedPattern = Regex("^[a-zA-Z0-9_-]*$")

    override fun insertString(fb: FilterBypass?, offset: Int, string: String?, attr: AttributeSet?) {
        if (string != null && allowedPattern.matches(string)) {
            super.insertString(fb, offset, string, attr)
        }
    }

    override fun replace(fb: FilterBypass?, offset: Int, length: Int, text: String?, attrs: AttributeSet?) {
        if (text != null && allowedPattern.matches(text)) {
            super.replace(fb, offset, length, text, attrs)
        }
    }

    override fun remove(fb: FilterBypass?, offset: Int, length: Int) {
        super.remove(fb, offset, length)
    }
}
