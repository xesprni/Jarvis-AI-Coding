package com.miracle.utils.extensions

private val LINE_SEPARATOR = Regex("\r?\n")

fun String.splitLines(): List<String> = this.split(LINE_SEPARATOR)