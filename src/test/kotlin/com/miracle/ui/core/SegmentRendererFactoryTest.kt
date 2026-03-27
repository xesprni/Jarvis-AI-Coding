package com.miracle.ui.core

import com.intellij.openapi.project.Project
import com.miracle.agent.parser.ProposedPlanSegment
import io.mockk.mockk
import java.awt.Component
import java.awt.Container
import javax.swing.AbstractButton
import javax.swing.JPanel
import javax.swing.JScrollPane
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SegmentRendererFactoryTest {

    private val project: Project = mockk(relaxed = true)

    @Test
    fun proposedPlanCardShouldExposeFollowUpAndExecuteActions() {
        val scrollContent = JPanel()
        val scrollPane = JScrollPane(scrollContent)
        val scrollManager = ChatScrollManager(scrollPane, scrollContent)
        val planSegment = ProposedPlanSegment("# Plan\n- Inspect the parser")
        val capturedActions = mutableListOf<ProposedPlanAction>()
        val capturedPlans = mutableListOf<ProposedPlanSegment>()
        val renderer = SegmentRendererFactory(
            project = project,
            scrollManager = scrollManager,
            onProposedPlanAction = { action, segment ->
                capturedActions += action
                capturedPlans += segment
            },
        )

        val component = renderer.renderSegment(planSegment)
        val buttons = collectButtons(component).associateBy { it.text }
        val followUpLabel = "\u7EE7\u7EED\u63D0\u95EE"
        val executeLabel = "\u5207\u6362\u5230 Agent \u6267\u884C"

        assertTrue(buttons.containsKey(followUpLabel))
        assertTrue(buttons.containsKey(executeLabel))

        val followUpButton = assertNotNull(buttons[followUpLabel])
        val executeButton = assertNotNull(buttons[executeLabel])

        followUpButton.doClick()
        executeButton.doClick()

        assertEquals(
            listOf(ProposedPlanAction.ASK_FOLLOW_UP, ProposedPlanAction.EXECUTE_IN_AGENT),
            capturedActions,
        )
        assertEquals(listOf(planSegment, planSegment), capturedPlans)
    }

    private fun collectButtons(component: Component): List<AbstractButton> {
        val buttons = mutableListOf<AbstractButton>()
        if (component is AbstractButton) {
            buttons += component
        }
        if (component is Container) {
            component.components.forEach { child ->
                buttons += collectButtons(child)
            }
        }
        return buttons
    }
}
