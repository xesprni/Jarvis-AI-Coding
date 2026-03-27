package com.qihoo.finance.lowcode.editor.completions;

/**
 * https://plugins.jetbrains.com/docs/intellij/inlay-hints.html#declarative-inlay-hints-provider
 */
public enum ChatxCompletionType {

    /** 内嵌在文本中 */
    Inline,
    /** 官方没说支持这种类型 */
    AfterLineEnd,
    /** 展示在文本上方 */
    Block;
}
