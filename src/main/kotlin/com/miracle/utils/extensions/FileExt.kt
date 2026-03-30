package com.miracle.utils.extensions

import org.mozilla.universalchardet.UniversalDetector
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.file.Path

/**
 * 检测文件的字符编码。
 *
 * @param default 默认字符编码，默认为 UTF-8
 * @return 检测到的字符编码，检测失败时返回默认值
 */
fun Path.detectCharset(default: Charset = Charsets.UTF_8): Charset {
    return this.toFile().detectCharset(default)
}

/**
 * 安全读取文件文本内容，遇到编码错误时使用替换字符代替。
 *
 * @param charset 文件字符编码，默认为 UTF-8
 * @return 文件文本内容
 */
fun Path.readTextSafely(charset: Charset = Charsets.UTF_8): String {
    val decoder = charset.newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE)

    return this.toFile().inputStream().use { inputStream ->
        InputStreamReader(inputStream, decoder).readText()
    }
}

/**
 * 检测文件的字符编码，使用 UniversalDetector 自动检测。
 *
 * @param default 默认字符编码，默认为 UTF-8
 * @return 检测到的字符编码，检测失败时返回默认值
 */
fun File.detectCharset(default: Charset = Charsets.UTF_8): Charset {
    if (!this.exists() || this.length() == 0L) {
        return default
    }
    val detector = UniversalDetector(null)
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
            default
        }
    } else {
        default
    }
}
