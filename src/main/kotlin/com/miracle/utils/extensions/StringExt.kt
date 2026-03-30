package com.miracle.utils.extensions

/** 换行符正则表达式，兼容 Unix 和 Windows 换行符 */
private val LINE_SEPARATOR = Regex("\r?\n")

/**
 * 按换行符分割字符串为行列表。
 *
 * @return 分割后的行列表
 */
fun String.splitLines(): List<String> = this.split(LINE_SEPARATOR)
