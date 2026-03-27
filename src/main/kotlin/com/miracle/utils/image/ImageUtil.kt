package com.miracle.utils.image

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.util.io.createDirectories
import com.miracle.utils.ConversationStore
import com.miracle.utils.file.FileUtil
import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.MultiResolutionImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.MemoryCacheImageOutputStream
import kotlin.io.path.div
import kotlin.io.path.pathString

object ImageUtil {

    private val logger = thisLogger()

    fun pathToImageDetails(
        path: String,
        maxWidth: Int = 1024,
        quality: Float = 0.8f
    ): String {
        try {
            val file = File(path)
            if (!file.exists()) {
                throw IllegalArgumentException("Image file does not exist: $path")
            }

            val mediaType = FileUtil.getImageMediaType(path)
            val originalImage = ImageIO.read(file)
                ?: throw IllegalArgumentException("Failed to read image: $path")

            val processedImage = if (originalImage.width > maxWidth) {
                resizeImage(originalImage, maxWidth)
            } else {
                originalImage
            }

            val compressedBytes = compressImage(processedImage, getFormatName(mediaType), quality)
            val base64String = Base64.getEncoder().encodeToString(compressedBytes)

            return "data:$mediaType;base64,$base64String"
        } catch (e: Exception) {
            logger.warn("Failed to process image: $path", e)
            throw e
        }
    }

    fun pathsToImageDetailsList(
        paths: List<String>,
        maxWidth: Int = 1024,
        quality: Float = 0.8f
    ): List<String> {
        return paths.mapNotNull { path ->
            try {
                if (path.isNotEmpty()) pathToImageDetails(path, maxWidth, quality) else null
            } catch (e: Exception) {
                logger.warn("Skipping image due to error: $path", e)
                null
            }
        }
    }

    private fun resizeImage(originalImage: BufferedImage, maxWidth: Int): BufferedImage {
        val originalWidth = originalImage.width
        val originalHeight = originalImage.height
        val aspectRatio = originalHeight.toDouble() / originalWidth.toDouble()
        val newHeight = (maxWidth * aspectRatio).toInt()

        val scaledImage = originalImage.getScaledInstance(maxWidth, newHeight, Image.SCALE_SMOOTH)
        val bufferedImage = BufferedImage(maxWidth, newHeight, BufferedImage.TYPE_INT_RGB)
        val graphics = bufferedImage.createGraphics()
        graphics.drawImage(scaledImage, 0, 0, null)
        graphics.dispose()

        return bufferedImage
    }

    private fun compressImage(
        image: BufferedImage,
        formatName: String,
        quality: Float
    ): ByteArray {
        val outputStream = ByteArrayOutputStream()

        val writers = ImageIO.getImageWritersByFormatName(formatName)
        if (!writers.hasNext()) {
            throw IllegalArgumentException("No writer found for format: $formatName")
        }

        val writer = writers.next()
        val writeParam = writer.defaultWriteParam

        if (writeParam.canWriteCompressed()) {
            writeParam.compressionMode = ImageWriteParam.MODE_EXPLICIT
            writeParam.compressionQuality = quality.coerceIn(0f, 1f)
        }

        val memoryOutputStream = MemoryCacheImageOutputStream(outputStream)
        writer.output = memoryOutputStream

        try {
            writer.write(null, IIOImage(image, null, null), writeParam)
        } finally {
            writer.dispose()
            memoryOutputStream.close()
        }

        return outputStream.toByteArray()
    }

    private fun getFormatName(mediaType: String): String {
        return when (mediaType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            else -> "jpg"
        }
    }

    fun saveTmpImage(image: Image, taskId: String, project: Project): String {
        val bufferedImage = image.toBuffered()
        val dir = ConversationStore.getConvBaseDir(project) / taskId
        dir.createDirectories()
        val fileName = "image_${System.currentTimeMillis()}.png"
        val localFile = File(dir.pathString, fileName)

        ImageIO.write(bufferedImage, "png", localFile)
        return localFile.absolutePath
    }

    fun copyImage(path: String, taskId: String, project: Project): String {
        val srcFile = File(path)
        val dir = ConversationStore.getConvBaseDir(project) / taskId
        dir.createDirectories()
        val fileName = "image_${System.currentTimeMillis()}.png"
        val localFile = File(dir.pathString, fileName)
        srcFile.copyTo(localFile, overwrite = true)
        return localFile.absolutePath
    }

    /**
     * 安全地将任意 Image 转换为 BufferedImage（扩展函数版）
     */
    fun Image.toBuffered(): BufferedImage = when (this) {
        is BufferedImage -> this

        is MultiResolutionImage -> {
            // 取最佳分辨率的变体
            val variant = this.getResolutionVariant(
                this.getWidth(null).toDouble(),
                this.getHeight(null).toDouble()
            )
            variant.toBuffered()
        }

        else -> {
            val w = this.getWidth(null).takeIf { it > 0 } ?: 1
            val h = this.getHeight(null).takeIf { it > 0 } ?: 1

            val bimg = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
            val g = bimg.createGraphics()
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g.drawImage(this, 0, 0, null)
            } finally {
                g.dispose()
            }
            bimg
        }
    }

    fun base64ToBytes(base64String: String): ByteArray {
        return Base64.getDecoder().decode(base64String)
    }

    fun bytesToBase64(bytes: ByteArray): String {
        return Base64.getEncoder().encodeToString(bytes)
    }
}
