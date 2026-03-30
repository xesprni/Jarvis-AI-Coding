package com.miracle.agent.parser

/**
 * 流式消息处理，将模型返回的流式消息转换成Segment
 * 模型攒一段时间的流式输出（例如8ms），调用parse接口（如果这段时间没输出则不调用）。
 * 每次调用都会返回List<Segment>对象，由于返回的内容最后一条为解析中状态，下一次时可能会有些调整，UI在拿到List<Segment>对象后，需要判断：
 * 如果上一次消息的最后一条Segment，跟下一次消息的第一条Segment类型相同，且属于（Text, Code)时，需要替换UI当前这块Segment的全部内容。
 * Text, Code（属于未结束的块）这些Segment 每次Content 都会返回全量的内容
 */
class SseMessageParser: MessageParser{

    private companion object {
        const val CODE_FENCE = "```"
        const val SEARCH_MARKER = "<<<<<<< SEARCH"
        const val SEPARATOR_MARKER = "======="
        const val REPLACE_MARKER = ">>>>>>> REPLACE"
        const val NEWLINE = "\n"
        const val HEADER_DELIMITER = ":"
        const val HEADER_PARTS_LIMIT = 2
    }

    /** 当前解析器状态 */
    private var parserState: ParserState = ParserState.Outside
    /** 输入内容缓冲区 */
    private val buffer = StringBuilder()

    /**
     * 解析流式输入文本，将内容拆分为 Segment 列表
     *
     * @param input 本次接收到的流式文本片段
     * @return 本次解析产生的 Segment 列表
     */
    override fun parse(input: String): List<Segment> {
        val segments = mutableListOf<Segment>()
        var position = 0

        while (position < input.length) {
            val endPosition = minOf(position + 16, input.length)
            val chunk = input.substring(position, endPosition)
            buffer.append(chunk)
            // 如果当前buffer里还包含下一种类型的segment，则继续处理
            while (processNextSegment(segments)) {}
            position = endPosition
        }
        segments.addAll(getPendingSegments())
        return segments
    }

    /**
     * 根据当前解析状态尝试从缓冲区中提取下一个 Segment
     *
     * @param segments 用于收集解析结果的 Segment 列表
     * @return 是否成功提取了 Segment，true 表示可以继续提取
     */
    private fun processNextSegment(segments: MutableList<Segment>): Boolean {
        return when (val state = parserState) {
            is ParserState.Outside -> processOutsideState(segments)
            is ParserState.CodeHeaderWaiting -> processCodeHeaderState(segments, state)
            is ParserState.InCode -> processInCodeState(segments, state)
            is ParserState.InSearch -> processInSearchState(segments, state)
            is ParserState.InReplace -> processInReplaceState(segments, state)
        }
    }

    /**
     * 处理代码块外部的文本内容，检测代码块起始标记
     *
     * @param segments 用于收集解析结果的 Segment 列表
     * @return 是否成功提取了内容
     */
    private fun processOutsideState(segments: MutableList<Segment>): Boolean {
        // 这里处理文本区域的时候，如果没有代码块，则直接没读取。
        val fenceIdx = buffer.indexOf(CODE_FENCE)
        return if (fenceIdx != -1) {
            extractTextBeforeIndex(fenceIdx)?.let { segments.add(it) }
            consumeFromBuffer(fenceIdx + CODE_FENCE.length)
            parserState = ParserState.CodeHeaderWaiting()
            true
        } else {
            false
        }
    }

    /**
     * 处理代码块头部信息，解析语言和文件路径
     *
     * @param segments 用于收集解析结果的 Segment 列表
     * @param state 当前等待头部的解析状态
     * @return 是否成功完成头部解析
     */
    private fun processCodeHeaderState(segments: MutableList<Segment>, state: ParserState.CodeHeaderWaiting): Boolean {
        // 代码块头部以换行结束
        val nlIdx = buffer.indexOf(NEWLINE)
        if (nlIdx < 0)  return false

        val headerLine = buffer.substring(0, nlIdx)
        consumeFromBuffer(nlIdx + NEWLINE.length)
        val updatedHeader = state.content + headerLine
        val header = parseCodeHeader(updatedHeader)

        return if (header != null) {
            segments.add(header)
            parserState = ParserState.InCode(header)
            true
        } else {
            segments.add(CodeHeaderWaiting(updatedHeader))
            parserState = ParserState.CodeHeaderWaiting(updatedHeader)
            false
        }
    }

    /**
     * 处理代码块内部内容，检测代码块结束标记或搜索替换标记
     *
     * @param segments 用于收集解析结果的 Segment 列表
     * @param state 当前代码块内的解析状态
     * @return 是否成功处理了一行内容
     */
    private fun processInCodeState(segments: MutableList<Segment>, state: ParserState.InCode): Boolean {
        // 代码块尾部以换行结束
        val nlIdx = buffer.indexOf(NEWLINE)
        if (nlIdx < 0)  return false

        val line = buffer.substring(0, nlIdx)
        consumeFromBuffer(nlIdx + NEWLINE.length)

        return when {
            line.trim() == CODE_FENCE -> {
                if (state.content.isNotEmpty()) {
                    segments.add(Code(state.content, state.header.codeLanguage, state.header.codeFilePath))
                }
                segments.add(CodeEnd(""))
                parserState = ParserState.Outside
                true
            }
            line.trimStart().startsWith(SEARCH_MARKER) -> {
                // Emit accumulated code content before transitioning
                if (state.content.isNotEmpty()) {
                    segments.add(Code(state.content, state.header.codeLanguage, state.header.codeFilePath))
                }
                segments.add(SearchWaiting("", state.header.codeLanguage, state.header.codeFilePath))
                parserState = ParserState.InSearch(state.header, "")
                true
            }
            else -> {
                val newContent = if (state.content.isEmpty()) line else state.content + NEWLINE + line
                parserState = ParserState.InCode(state.header, newContent)
                true
            }
        }
    }

    /**
     * 处理搜索替换块中的搜索部分，检测分隔标记
     *
     * @param segments 用于收集解析结果的 Segment 列表
     * @param state 当前搜索块内的解析状态
     * @return 是否成功处理了一行内容
     */
    private fun processInSearchState(segments: MutableList<Segment>, state: ParserState.InSearch): Boolean {
        val nlIdx = buffer.indexOf(NEWLINE)
        if (nlIdx < 0)  return false

        val line = buffer.substring(0, nlIdx)
        consumeFromBuffer(nlIdx + NEWLINE.length)

        return if (line.trim() == SEPARATOR_MARKER) {
            segments.add(
                ReplaceWaiting(
                    state.searchContent,
                    "",
                    state.header.codeLanguage,
                    state.header.codeFilePath
                )
            )
            parserState = ParserState.InReplace(state.header, state.searchContent, "")
            true
        } else {
            val newSearch =
                if (state.searchContent.isEmpty()) line else state.searchContent + NEWLINE + line
            segments.add(SearchWaiting(newSearch, state.header.codeLanguage, state.header.codeFilePath))
            parserState = ParserState.InSearch(state.header, newSearch)
            false
        }
    }

    /**
     * 处理搜索替换块中的替换部分，检测替换结束标记
     *
     * @param segments 用于收集解析结果的 Segment 列表
     * @param state 当前替换块内的解析状态
     * @return 是否成功处理了一行内容
     */
    private fun processInReplaceState(segments: MutableList<Segment>, state: ParserState.InReplace): Boolean {
        val nlIdx = buffer.indexOf(NEWLINE)
        if (nlIdx < 0)  return false

        val line = buffer.substring(0, nlIdx)
        consumeFromBuffer(nlIdx + NEWLINE.length)

        return when {
            line.trim().startsWith(REPLACE_MARKER) -> {
                segments.add(
                    SearchReplace(
                        search = state.searchContent,
                        replace = state.replaceContent,
                        codeLanguage = state.header.codeLanguage,
                        codeFilePath = state.header.codeFilePath
                    )
                )
                parserState = ParserState.InCode(state.header)
                true
            }

            line.trim() == CODE_FENCE -> {
                segments.add(CodeEnd(""))
                parserState = ParserState.Outside
                true
            }

            else -> {
                val newReplace =
                    if (state.replaceContent.isEmpty()) line else state.replaceContent + NEWLINE + line
                segments.add(
                    ReplaceWaiting(
                        state.searchContent,
                        newReplace,
                        state.header.codeLanguage,
                        state.header.codeFilePath
                    )
                )
                parserState = ParserState.InReplace(state.header, state.searchContent, newReplace)
                true
            }
        }
    }

    /**
     * 提取缓冲区中指定位置之前的文本作为 TextSegment
     *
     * @param index 文本截取的结束位置
     * @return 提取的文本片段，如果位置为 0 则返回 null
     */
    private fun extractTextBeforeIndex(index: Int): TextSegment? {
        return if (index > 0) TextSegment(buffer.substring(0, index)) else null
    }

    /**
     * 从缓冲区头部消费指定长度的内容
     *
     * @param length 需要消费的字符数
     */
    private fun consumeFromBuffer(length: Int) {
        buffer.delete(0, length)
    }

    /**
     * 解析代码块头部文本，提取语言和文件路径
     *
     * @param headerText 头部文本，格式为"语言:文件路径"
     * @return 解析后的 CodeHeader，如果格式不合法则返回 null
     */
    private fun parseCodeHeader(headerText: String): CodeHeader? {
        val parts = headerText.split(HEADER_DELIMITER, limit = HEADER_PARTS_LIMIT)
        return if (parts.isNotEmpty()) {
            CodeHeader(
                codeLanguage = parts.getOrNull(0) ?: "",
                codeFilePath = parts.getOrNull(1)
            )
        } else null
    }

    /** 将未解析完的内容转换成 Segment */
    private fun getPendingSegments(): List<Segment> {
        return when(val state = parserState) {
            is ParserState.Outside -> {
                if (buffer.isNotBlank()) listOf(TextSegment(buffer.toString()))
                else emptyList()
            }
            is ParserState.CodeHeaderWaiting -> {
                if (state.content.isNotBlank()) listOf(CodeHeaderWaiting(state.content))
                else emptyList()
            }
            is ParserState.InCode -> {
                val segments = mutableListOf<Segment>()
                if (buffer.toString().trim() == CODE_FENCE) {
                    if (state.content.isNotBlank()) {
                        segments.add(Code(state.content, state.header.codeLanguage, state.header.codeFilePath))
                    }
                    segments.add(CodeEnd(""))
                } else if (state.content.isNotBlank()) {
                    segments.add(Code(state.content, state.header.codeLanguage, state.header.codeFilePath))
                }
                segments
            }
            is ParserState.InSearch -> {
                if (state.searchContent.isNotBlank()) {
                    listOf(
                        SearchWaiting(
                            state.searchContent,
                            state.header.codeLanguage,
                            state.header.codeFilePath
                        )
                    )
                } else emptyList()
            }
            is ParserState.InReplace -> {
                if (state.replaceContent.isNotBlank()) {
                    listOf(
                        ReplaceWaiting(
                            state.searchContent,
                            state.replaceContent,
                            state.header.codeLanguage,
                            state.header.codeFilePath
                        )
                    )
                } else emptyList()
            }
        }
    }

    /**
     * 重置解析器状态，清空缓冲区和状态机
     */
    fun clear() {
        parserState = ParserState.Outside
        buffer.clear()
    }

    /**
     * SSE 消息解析过程中的不同解析状态
     * 由于枚举类不能携带数据且不能扩展，所以用sealed class来实现状态机
     */
    private sealed class ParserState {

        /** 当前处于解析器之外（默认状态、空闲、未进入任何结构） */
        object Outside : ParserState()

        /** 刚遇到代码块开头（```），等待读取代码头部信息 */
        data class CodeHeaderWaiting(
            val content: String = ""
        ) : ParserState()

        /** 当前正在读取代码块内容（已经有 header） */
        data class InCode(
            val header: CodeHeader,
            val content: String = ""
        ) : ParserState()

        /** 当前在读取代替换标记块中的 SEARCH 部分 */
        data class InSearch(
            val header: CodeHeader,
            val searchContent: String = ""
        ) : ParserState()

        /** 当前在读取 REPLACE 部分 */
        data class InReplace(
            val header: CodeHeader,
            val searchContent: String,
            val replaceContent: String = ""
        ) : ParserState()

    }

}
