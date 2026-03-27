package com.qifu.ui.smartconversation

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.impl.MenuItemPresentationFactory
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.popup.PopupFactoryImpl
import com.intellij.ui.popup.list.PopupListElementRenderer
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.qifu.ui.smartconversation.settings.service.JarvisModel
import com.qihoo.finance.lowcode.common.util.Icons
import javax.swing.Box
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.ListCellRenderer

class ModelListPopup(
    actionGroup: ActionGroup,
    context: DataContext
) : PopupFactoryImpl.ActionGroupPopup(
    null,
    actionGroup,
    context,
    false,
    false,
    true,
    false,
    null,
    -1,
    null,
    null,
    MenuItemPresentationFactory(),
    false
) {

    override fun getListElementRenderer(): ListCellRenderer<*> {
        return object : PopupListElementRenderer<Any>(this) {

            override fun createItemComponent(): JComponent? {
                createLabel()
                val panel = BorderLayoutPanel()
                    .withBorder(JBUI.Borders.emptyRight(8))
                    .addToLeft(myTextLabel)
                myIconBar = createIconBar()
                return layoutComponent(panel)
            }

            override fun createIconBar(): JComponent? {
                return Box.createHorizontalBox().apply {
                    border = JBUI.Borders.emptyRight(JBUI.CurrentTheme.ActionsList.elementIconGap())
                    add(myIconLabel)
                }
            }
        }
    }
}

class JarvisModelsListPopupAction : DumbAwareAction {
    
    val model: JarvisModel?
    private val locked: Boolean
    private val selected: Boolean
    private val onModelChanged: Runnable?
    
    constructor(
        model: JarvisModel,
        locked: Boolean = false,
        selected: Boolean = false,
        onModelChanged: Runnable?
    ) : super(model.name, "", if (locked) Icons.Locked else model.icon) {
        this.model = model
        this.locked = locked
        this.selected = selected
        this.onModelChanged = onModelChanged
    }
    
    constructor(
        name: String,
        code: String,
        icon: Icon,
        locked: Boolean = false,
        selected: Boolean = false,
        onModelChanged: Runnable?
    ) : super(name, "", if (locked) Icons.Locked else icon) {
        this.model = JarvisModel(name, code, icon)
        this.locked = locked
        this.selected = selected
        this.onModelChanged = onModelChanged
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isVisible = true
        event.presentation.isEnabled = !locked && !selected
    }

    override fun actionPerformed(e: AnActionEvent) {
        onModelChanged?.run()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
