package com.miracle.utils.extensions

import org.mozilla.universalchardet.UniversalDetector
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.file.Path

fun Path.detectCharset(default: Charset = Charsets.UTF_8): Charset {
    // 将 Path 转换为 File，然后调用 File 的扩展方法
    return this.toFile().detectCharset(default)
}

fun Path.readTextSafely(charset: Charset = Charsets.UTF_8): String {
    val decoder = charset.newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE)

    return this.toFile().inputStream().use { inputStream ->
        InputStreamReader(inputStream, decoder).readText()
    }
}

fun File.detectCharset(default: Charset = Charsets.UTF_8): Charset {
    if (!this.exists() || this.length() == 0L) {
        return default
    }
    val detector = UniversalDetector(null)
    // 使用 use 会自动关闭流，更安全
    this.inputStream().use { fis ->
        val buf = ByteArray(4096)
        var nread: Int
        while (fis.read(buf).also { nread = it } > 0 && !detector.isDone) {
            detector.handleData(buf, 0, nread)
        }
    }
    detector.dataEnd()
    val detectedCharsetName = detector.detectedCharset
    detector.reset()

    return if (detectedCharsetName != null) {
        try {
            Charset.forName(detectedCharsetName)
        } catch (e: Exception) {
            default // 如果库返回了一个系统不支持的编码名称，则回退
        }
    } else {
        default // 如果无法检测到，则使用默认值
    }
}