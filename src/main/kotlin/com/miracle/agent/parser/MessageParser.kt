package com.miracle.agent.parser

/**
 * 消息解析器接口，定义将文本输入解析为 Segment 列表的契约
 */
interface MessageParser {
    /**
     * 解析输入文本，返回解析后的片段列表
     *
     * @param input 待解析的文本内容
     * @return 解析后的 Segment 列表
     */
    fun parse(input: String): List<Segment>
}