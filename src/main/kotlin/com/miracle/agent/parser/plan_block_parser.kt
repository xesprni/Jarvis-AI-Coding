package com.miracle.agent.parser

/**
 * 计划块解析器，用于从文本中提取 <proposed_plan> 标签包裹的计划内容
 */
internal object PlanBlockParser {

    private const val OPEN_TAG = "<proposed_plan>"
    private const val CLOSE_TAG = "</proposed_plan>"

    /**
     * 将文本按照 <proposed_plan> 标签拆分为 Segment 列表，
     * 标签内的内容解析为 ProposedPlanSegment，标签外的内容解析为 TextSegment
     *
     * @param text 待拆分的文本内容
     * @return 拆分后的 Segment 列表
     */
    fun splitText(text: String): List<Segment> {
        if (!text.contains(OPEN_TAG)) {
            return listOf(TextSegment(text))
        }

        val segments = mutableListOf<Segment>()
        var cursor = 0
        while (cursor < text.length) {
            val openIndex = text.indexOf(OPEN_TAG, cursor)
            if (openIndex == -1) {
                addTextSegment(segments, text.substring(cursor))
                break
            }

            addTextSegment(segments, text.substring(cursor, openIndex))

            val contentStart = openIndex + OPEN_TAG.length
            val closeIndex = text.indexOf(CLOSE_TAG, contentStart)
            if (closeIndex == -1) {
                addTextSegment(segments, text.substring(openIndex))
                break
            }

            segments.add(ProposedPlanSegment(text.substring(contentStart, closeIndex).trim()))
            cursor = closeIndex + CLOSE_TAG.length
        }

        return segments
    }

    /**
     * 对 Segment 列表进行展开，将其中的 TextSegment 递归拆分为包含计划块的子列表
     *
     * @param segments 待展开的 Segment 列表
     * @return 展开后的 Segment 列表
     */
    fun expandSegments(segments: List<Segment>): List<Segment> {
        return buildList {
            segments.forEach { segment ->
                when (segment) {
                    is TextSegment -> addAll(splitText(segment.text))
                    else -> add(segment)
                }
            }
        }
    }

    /**
     * 从文本中移除 <proposed_plan> 和 </proposed_plan> 标签，保留标签内的内容。
     */
    fun stripTags(text: String): String {
        return text.replace(OPEN_TAG, "").replace(CLOSE_TAG, "")
    }

    /**
     * 如果文本非空，将其添加为 TextSegment
     *
     * @param segments 目标 Segment 列表
     * @param text 待添加的文本内容
     */
    private fun addTextSegment(segments: MutableList<Segment>, text: String) {
        if (text.isNotEmpty()) {
            segments.add(TextSegment(text))
        }
    }
}
