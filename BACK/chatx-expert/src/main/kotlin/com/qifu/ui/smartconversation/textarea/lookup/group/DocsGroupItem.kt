package ee.carlrobert.codegpt.ui.textarea.lookup.group

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.qifu.ui.smartconversation.DocumentationDetails
import com.qifu.ui.smartconversation.settings.documentation.DocumentationSettings
import com.qihoo.finance.lowcode.smartconversation.service.FeatureType
import com.qifu.ui.smartconversation.settings.service.ModelSelectionService
import com.qihoo.finance.lowcode.smartconversation.service.ServiceType
import com.qifu.ui.smartconversation.textarea.header.DocumentationTagDetails
import com.qifu.ui.smartconversation.textarea.header.TagManager
import com.qifu.ui.smartconversation.textarea.lookup.LookupActionItem
import com.qifu.ui.smartconversation.textarea.lookup.action.AddDocActionItem
import com.qifu.ui.smartconversation.textarea.lookup.action.DocActionItem
import com.qifu.ui.smartconversation.textarea.lookup.action.ViewAllDocsActionItem
import com.qifu.ui.smartconversation.textarea.lookup.group.AbstractLookupGroupItem
import java.time.Instant
import java.time.format.DateTimeParseException

class DocsGroupItem(
    private val tagManager: TagManager
) : AbstractLookupGroupItem() {

    override val displayName: String = "Docs"
    override val icon = AllIcons.Toolwindows.Documentation
    override val enabled: Boolean
        get() = enabled()

    fun enabled(): Boolean {
        if (ModelSelectionService.getInstance().getServiceForFeature(FeatureType.CHAT) != ServiceType.PROXYAI) {
            return false
        }

        return tagManager.getTags().none { it is DocumentationTagDetails }
    }

    override suspend fun getLookupItems(searchText: String): List<LookupActionItem> =
        listOf(AddDocActionItem(), ViewAllDocsActionItem()) +
                service<DocumentationSettings>().state.documentations
                    .sortedByDescending { parseDateTime(it.lastUsedDateTime) }
                    .filter {
                        searchText.isEmpty() || (it.name?.contains(searchText, true) ?: false)
                    }
                    .take(10)
                    .map {
                        DocActionItem(DocumentationDetails(it.name ?: "", it.url ?: ""))
                    }

    private fun parseDateTime(dateTimeString: String?): Instant {
        return dateTimeString?.let {
            try {
                Instant.parse(it)
            } catch (e: DateTimeParseException) {
                Instant.EPOCH
            }
        } ?: Instant.EPOCH
    }
}