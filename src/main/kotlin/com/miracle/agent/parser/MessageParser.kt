package com.miracle.agent.parser

interface MessageParser {
    fun parse(input: String): List<Segment>
}