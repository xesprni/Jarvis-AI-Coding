package com.qifu.ui.smartconversation.textarea.lookup.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.qifu.ui.smartconversation.panels.UserInputPanel
import com.qifu.ui.smartconversation.textarea.header.ImageTagDetails
import com.qifu.utils.image.ImageUtil
import com.qihoo.finance.lowcode.smartconversation.configuration.JarvisKeys.IMAGE_ATTACHMENT_FILE_PATH

/**
 * Action item for attaching images to chat messages.
 * Opens a file chooser dialog allowing users to select image files.
 */
class ImageActionItem(
    private val taskId: String
) : AbstractLookupActionItem() {

    companion object {
        private val logger = thisLogger()
        private val SUPPORTED_IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "webp", "svg")
    }

    override val displayName = "Images"
    override val icon = AllIcons.FileTypes.Any_type

    override fun execute(project: Project, userInputPanel: UserInputPanel) {
        val descriptor = createImageFileDescriptor()

        try {
            val selectedFiles = FileChooser.chooseFiles(descriptor, project, null)
            handleSelectedFiles(selectedFiles, project, userInputPanel)
        } catch (e: Exception) {
            logger.error("Failed to open file chooser for image attachment", e)
        }
    }

    private fun createImageFileDescriptor(): FileChooserDescriptor {
        return FileChooserDescriptor(true, false, false, false, false, false).apply {
            title = displayName
            description = "Select an image file to attach"
            withFileFilter { file ->
                file.extension?.lowercase() in SUPPORTED_IMAGE_EXTENSIONS
            }
        }
    }

    private fun handleSelectedFiles(
        files: Array<VirtualFile>,
        project: Project,
        userInputPanel: UserInputPanel
    ) {
        if (files.isEmpty()) return

        val selectedFile = files.first()
        val path = ImageUtil.copyImage(selectedFile.path,taskId, project)
        storeImagePath(project, path)
        userInputPanel.addTag(ImageTagDetails(path))
    }

    private fun storeImagePath(project: Project, path: String) {
        val imagePaths = project.getUserData(IMAGE_ATTACHMENT_FILE_PATH)?.toMutableList() ?: mutableListOf()
        imagePaths.add(path)
        project.putUserData(IMAGE_ATTACHMENT_FILE_PATH, imagePaths)
    }
}