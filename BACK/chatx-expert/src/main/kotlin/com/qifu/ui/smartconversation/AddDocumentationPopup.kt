package com.qifu.ui.smartconversation

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.qifu.ui.smartconversation.settings.documentation.DocumentationDetailsState
import com.qifu.ui.smartconversation.settings.documentation.DocumentationSettings
import javax.swing.JComponent

class AddDocumentationDialog(project: Project) : DialogWrapper(project) {

    private var nameField = JBTextField("", 40).apply {
        emptyText.text = "Jarvis docs"
    }
    private var urlField = JBTextField("", 40).apply {
        emptyText.text = "https://docs.tryproxy.io"
    }
    private var saveCheckbox =
        JBCheckBox("Saving for feature", true)

    val documentationDetails: DocumentationDetails
        get() = DocumentationDetails(nameField.text, urlField.text)

    init {
        title = "Add documentation"
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row {
            cell(nameField)
                .label(
                    "Name:",
                    LabelPosition.TOP
                )
                .focused()
        }
        row {
            cell(urlField)
                .label(
                    "URL:",
                    LabelPosition.TOP
                )
        }.rowComment("请输入地址")
        row { cell(saveCheckbox) }.topGap(TopGap.SMALL)
    }

    override fun doOKAction() {
        if (saveCheckbox.isSelected) {
            service<DocumentationSettings>().state.documentations.add(DocumentationDetailsState().apply {
                url = urlField.text
                name = nameField.text
            })
        }

        super.doOKAction()
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class DocumentationDetails(
    @JsonProperty(value = "name") var name: String,
    @JsonProperty(value = "url") var url: String
)
