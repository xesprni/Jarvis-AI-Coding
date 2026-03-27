package com.miracle.utils

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.ScalableIcon
import com.intellij.util.IconUtil
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.squareup.wire.internal.JvmStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import javax.swing.Icon
import javax.swing.text.JTextComponent

object UiUtil {

    val LOG = thisLogger()

    /**
     * 设置 lookup 单击选中
     */
    fun enableSingleClickSelection(lookup: LookupImpl) {
        val list = lookup.list // JList<LookupElement>
        list.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 1 && e.button == MouseEvent.BUTTON1) {
                    val index = list.locationToIndex(e.point)
                    if (index >= 0) {
                        val element = list.model.getElementAt(index)
                        lookup.finishLookup(Lookup.NORMAL_SELECT_CHAR, element)
                    }
                }
            }
        })
        // 鼠标悬浮时高亮选中项
        list.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                val index = list.locationToIndex(e.point)
                if (index >= 0 && index != list.selectedIndex) {
                    list.selectedIndex = index
                    list.ensureIndexIsVisible(index)
                }
            }
        })
    }

    @JvmStatic
    @RequiresBackgroundThread
    fun loadIconFromUrl(urlStr: String, width: Int? = null, cacheSeconds: Int = 3600): Icon? {
        val cacheDir = Paths.get(System.getProperty("user.home"), ".jarvis", "icons")
        Files.createDirectories(cacheDir)
        
        val fileName = urlStr.hashCode().toString() + getExtension(urlStr)
        val cacheFile = cacheDir.resolve(fileName)
        
        if (Files.exists(cacheFile)) {
            val age = System.currentTimeMillis() - Files.getLastModifiedTime(cacheFile).toMillis()
            if (age < cacheSeconds * 1000) {
                val icon = IconLoader.findIcon(cacheFile.toUri().toURL())
                return resizeIcon(icon, width)
            }
        }
        
        return try {
            URL(urlStr).openStream().use { input ->
                Files.copy(input, cacheFile, StandardCopyOption.REPLACE_EXISTING)
            }
            val icon = IconLoader.findIcon(cacheFile.toUri().toURL())
            return resizeIcon(icon, width)
        } catch (e: Exception) {
            LOG.warn("download icon failed: $urlStr", e)
            null
        }
    }


    fun resizeIcon(icon: Icon?, width: Int?): Icon? {
        if (width == null || icon == null) return icon
        return if (icon is ScalableIcon) {
            if (width != icon.iconWidth) {
                icon.scale(width.toFloat() / icon.iconWidth)
            } else icon
        } else {
           IconUtil.scale(icon, null, width.toFloat() / icon.iconWidth);
        }
    }

    private fun getExtension(url: String): String {
        val path = URL(url).path
        val lastDot = path.lastIndexOf('.')
        return if (lastDot > 0) path.substring(lastDot) else ".png"
    }

    /**
     * macOS 输入法组合态下，直接 setText 可能让 Swing 内部维护的 committed range 失效。
     * 在程序化修改文本前先结束当前组合态，避免 JTextComponent 抛出 Invalid range。
     */
    fun setTextSafely(component: JTextComponent, value: String, moveCaretToEnd: Boolean = false) {
        if (component.text == value) {
            if (moveCaretToEnd) {
                runCatching { component.caretPosition = value.length }
            }
            return
        }
        runCatching { component.inputContext?.endComposition() }
        component.text = value
        if (moveCaretToEnd) {
            runCatching { component.caretPosition = value.length }
        }
    }

    fun clearTextSafely(component: JTextComponent) {
        setTextSafely(component, "")
    }
}
