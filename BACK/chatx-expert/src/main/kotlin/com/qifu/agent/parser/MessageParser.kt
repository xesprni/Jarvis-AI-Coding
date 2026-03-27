package com.qifu.agent.parser

interface MessageParser {
    fun parse(input: String): List<Segment>
}