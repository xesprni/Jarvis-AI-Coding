package com.qihoo.finance.lowcode.aiquestion.ui.search.renderer

/**
 * InsetValueProvider
 *
 * @author fengjinfu-jk
 * date 2024/8/13
 * @version 1.0.0
 * @apiNote InsetValueProvider
 */
interface SearchInsetValueProvider {
    val left: Int
        get() = 0
    val right: Int
        get() = 0
    val top: Int
        get() = 0
    val down: Int
        get() = 0
}
