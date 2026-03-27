package com.miracle.utils

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

enum class LineEndingType {
    CRLF, LF
}

private val LOG = Logger.getInstance("com.miracle.utils.file")

fun readFileSafe(path: String): String? {
    return try {
        File(path).readText(Charsets.UTF_8)
    } catch (e: Exception) {
        LOG.warn(e)
        null
    }
}

fun writeTextContent(
    filePath: String,
    content: String,
    encoding: Charset,
    endings: LineEndingType
) {
    val adjustedContent = when (endings) {
        LineEndingType.CRLF -> content.replace(Regex("(?<!\r)\n"), "\r\n")
        LineEndingType.LF -> content.replace("\r\n", "\n")
    }
    File(filePath).writeText(adjustedContent, encoding)
}

val lineEndingCache = CacheBuilder.newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .build(object : CacheLoader<String, LineEndingType>() {
        override fun load(filePath: String): LineEndingType {
            return detectLineEndingsDirect(filePath)
        }
    })

fun detectLineEndings(filePath: String): LineEndingType {
    val resolvedPath = File(filePath).canonicalPath
    return lineEndingCache.get(resolvedPath)
}

fun File.readFirstBytes(maxBytes: Int = 4096): ByteArray {
    return this.inputStream().use { input ->
        val buffer = ByteArray(maxBytes)
        val bytesRead = input.read(buffer)
        if (bytesRead < 0) ByteArray(0) else buffer.copyOf(bytesRead)
    }
}

fun detectLineEndingsDirect(
    filePath: String,
    encoding: Charset = Charsets.UTF_8
): LineEndingType {
    return try {
        val file = File(filePath)
        val content = file.readFirstBytes().toString(encoding)
        detectContentLineEnding(content)
    } catch (e: Exception) {
        System.err.println("Error detecting line endings for file $filePath: ${e.message}")
        LineEndingType.LF
    }
}

val repoEndingCache = CacheBuilder.newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .build<String, LineEndingType>()

fun detectRepoLineEndings(filePath: String, project: Project): LineEndingType {
    val resolvedPath = normalizeFilePath(filePath, project)

    return repoEndingCache.getIfPresent(resolvedPath)
        ?: detectRepoLineEndingsDirect(resolvedPath).also {
            repoEndingCache.put(resolvedPath, it)
        }
}

fun detectRepoLineEndingsDirect(cwd: String): LineEndingType {
    return LineEndingType.LF
}

fun detectContentLineEnding(content: String): LineEndingType {
    var crlfCount = 0
    var lfCount = 0
    for (i in content.indices) {
        if (content[i] == '\n') {
            if (i > 0 && content[i - 1] == '\r') {
                crlfCount++
            } else {
                lfCount++
            }
        }
    }
    return if (crlfCount > lfCount) LineEndingType.CRLF else LineEndingType.LF
}

fun replaceContentLineEnding(content: String, endings: LineEndingType): String {
    return when (endings) {
        LineEndingType.CRLF -> content.replace(Regex("(?<!\r)\n"), "\r\n")
        LineEndingType.LF -> content.replace("\r\n", "\n")
    }
}

fun isInDirectory(relativePath: String, relativeCwd: String): Boolean {
    if (relativePath == ".") return true
    if (relativePath.startsWith("~")) return false
    if (relativePath.contains("\u0000") || relativeCwd.contains("\u0000")) return false

    val base = Paths.get(System.getProperty("user.dir")).resolve(relativeCwd).normalize()
    val fullPath = base.resolve(relativePath).normalize()

    return fullPath.startsWith(base)
}

fun addLineNumbers(content: String, startLine: Int = 1, maxLineLength: Int = 500): String {
    if (content.isEmpty()) return ""

    return content.lineSequence().mapIndexed { index, line ->
        val lineNum = index + startLine
        val lineContent = if (line.length > maxLineLength) "${line.take(maxLineLength)}..." else line
        // Handle large numbers differently
        if (lineNum.toString().length >= 6) {
            return@mapIndexed "${lineNum}→$lineContent"
        }
        val padded = lineNum.toString().padStart(6, ' ')
        "$padded→$lineContent"
    }.joinToString("\n")
}

fun isDirEmpty(dirPath: String): Boolean {
    val dir = File(dirPath)
    return dir.exists() && dir.isDirectory && dir.listFiles()?.isEmpty() == true
}

fun findSimilarFile(filePath: String): String? {
    val file = File(filePath)
    val dir = file.parentFile ?: return null
    if (!dir.exists() || !dir.isDirectory) return null

    val baseName = file.nameWithoutExtension

    return dir.listFiles()?.firstOrNull {
        it.nameWithoutExtension == baseName && it.absolutePath != file.absolutePath
    }?.absolutePath
}

fun normalizeFilePath(filePath: String, project: Project = getCurrentProject()!!): String {
    val absoluteFilePath = if (Paths.get(filePath).isAbsolute) {
        filePath
    } else {
        Paths.get(project.basePath!!, filePath).toAbsolutePath().toString()
    }

    val narrowNoBreakSpace = '\u202F'

    var normalizedPath =  when {
        absoluteFilePath.endsWith(" AM.png") -> {
            absoluteFilePath.replace(" AM.png", "$narrowNoBreakSpace" + "AM.png")
        }
        absoluteFilePath.endsWith(" PM.png") -> {
            absoluteFilePath.replace(" PM.png", "$narrowNoBreakSpace" + "PM.png")
        }
        else -> absoluteFilePath
    }
    normalizedPath = toPosixPath(normalizedPath)
    return normalizedPath
}

private val fileEncodingCache: Cache<String, String> = CacheBuilder.newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .build()

fun detectFileEncoding(filePath: String): String {
    val absPath = filePath.replace("\\", "/") // standardize path for cache key
    return fileEncodingCache.get(absPath) {
        detectFileEncodingDirect(absPath)
    }
}

fun detectFileEncodingDirect(filePath: String): String {
    val BUFFER_SIZE = 4096
    val file = File(filePath)
    if (!file.exists() || file.isDirectory) {
        LOG.warn("File not found or is a directory: $filePath")
        return StandardCharsets.UTF_8.name()
    }

    var inputStream: InputStream? = null
    return try {
        inputStream = file.inputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        val bytesRead = inputStream.read(buffer)

        if (bytesRead >= 2 &&
            buffer[0] == 0xFF.toByte() &&
            buffer[1] == 0xFE.toByte()
        ) {
            "UTF-16LE"
        }else if (bytesRead >= 3 &&
            buffer[0] == 0xEF.toByte() &&
            buffer[1] == 0xBB.toByte() &&
            buffer[2] == 0xBF.toByte()
        ) {
            "UTF-8"
        } else {
            val asUtf8 = try {
                String(buffer, 0, bytesRead, Charsets.UTF_8)
            } catch (e: Exception) {
                ""
            }
            if (asUtf8.isNotEmpty()) "UTF-8" else "US-ASCII"
        }
    } catch (e: Exception) {
        LOG.warn("Error detecting encoding for file $filePath", e)
        "UTF-8"
    } finally {
        inputStream?.close()
    }
}

fun readTextContent(
    filePath: String,
    offset: Int = 0,
    maxLines: Int? = null
): String {
    val encoding = detectFileEncoding(filePath)
    val charset = Charset.forName(encoding)

    return File(filePath).bufferedReader(charset).useLines { lines ->
        var lines = lines.drop(offset)
        maxLines?.let { lines = lines.take(maxLines)}
        lines.joinToString("\n")
    }
}

fun truncateToCharBudget(text: String, maxResultChars: Int = 10_000): String {
    if (text.length <= maxResultChars) return text
    val head = text.take(maxResultChars)
    val truncatedLines = text.substring(maxResultChars).lines().size
    return "$head\n\n... [$truncatedLines lines truncated] ..."
}

/**
 * 将文件路径转换为 POSIX 格式（使用 '/' 作为分隔符）
 *
 * 示例：
 *   "C:\\Users\\Tom\\Documents\\test.txt" -> "C:/Users/Tom/Documents/test.txt"
 *   "folder\\sub\\file.txt" -> "folder/sub/file.txt"
 *   "/usr/local/bin" -> "/usr/local/bin"
 */
fun toPosixPath(path: String): String {
    // 去除多余空格并将所有反斜杠替换为斜杠
    var normalized = path.trim().replace("\\", "/")

    // 将多个连续的斜杠（除了开头的双斜杠，例如网络路径）合并为一个
    normalized = Regex("(?<!:)//+").replace(normalized, "/")

    // 移除 Windows 盘符后的多余斜杠，例如 "C://Users" -> "C:/Users"
    normalized = Regex("^([A-Za-z]):/+").replace(normalized) { matchResult ->
        "${matchResult.groupValues[1]}:/"
    }

    return normalized
}

fun toRelativePath(pathStr: String, basePathStr: String): String {
    return runCatching {
        val basePath = Path(basePathStr).toAbsolutePath().normalize()
        val targetPath = Path(pathStr)
            .let { if (it.isAbsolute) it else basePath.resolve(it) }
            .normalize()
        targetPath.relativeTo(basePath).pathString
    }.getOrElse {
        runCatching { Path(pathStr).normalize().pathString }.getOrElse { pathStr }
    }
}

fun toAbsolutePath(pathStr: String, basePathStr: String): String {
    return runCatching {
        val basePath = Path(basePathStr).toAbsolutePath().normalize()
        Path(pathStr)
            .let { if (it.isAbsolute) it else basePath.resolve(it) }
            .normalize().pathString
    }.getOrElse {
        runCatching { Path(pathStr).toAbsolutePath().normalize().pathString }.getOrElse { pathStr }
    }
}

fun isFile(pathStr: String?, basePathStr: String): Boolean {
    pathStr ?: return false
    toAbsolutePath(pathStr, basePathStr).let {
        return File(it).isFile
    }
}

fun sanitizeFileName(input: String): String {
    // 替换掉文件名中不合法的字符为下划线
    return input
        .replace(Regex("""[\\/:*?"<>|]"""), "_") // Windows 禁止字符
        .replace(Regex("""\s+"""), "_")           // 连续空格替换为单个下划线
        .trim('_')                               // 去掉首尾下划线
}