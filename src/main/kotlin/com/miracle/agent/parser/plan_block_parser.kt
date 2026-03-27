package com.miracle.agent.parser

internal object PlanBlockParser {

    private const val OPEN_TAG = "<proposed_plan>"
    private const val CLOSE_TAG = "</proposed_plan>"

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

    private fun addTextSegment(segments: MutableList<Segment>, text: String) {
        if (text.isNotEmpty()) {
            segments.add(TextSegment(text))
        }
    }
}
