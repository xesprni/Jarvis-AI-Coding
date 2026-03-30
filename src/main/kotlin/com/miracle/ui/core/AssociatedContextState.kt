package com.miracle.ui.core

/**
 * 关联上下文添加操作的返回结果。
 */
internal sealed interface AssociatedContextAddResult {
    /** 添加成功 */
    data object Added : AssociatedContextAddResult
    /** 已存在相同键的条目 */
    data class Existing(val key: String) : AssociatedContextAddResult
}

/**
 * 管理聊天输入框的关联上下文状态，包括文件引用和代码选区。
 */
internal class AssociatedContextState {
    /** 已添加的关联上下文项映射，键为唯一标识 */
    private val items = linkedMapOf<String, AssociatedContextItem>()

    /**
     * 获取所有关联上下文项。
     *
     * @return 上下文项列表
     */
    fun items(): List<AssociatedContextItem> = items.values.toList()

    /**
     * 添加关联上下文项，如已存在则返回 Existing 结果。
     *
     * @param item 要添加的上下文项
     * @return 添加结果
     */
    fun add(item: AssociatedContextItem): AssociatedContextAddResult {
        val existing = items[item.key]
        return if (existing != null) {
            AssociatedContextAddResult.Existing(item.key)
        } else {
            items[item.key] = item
            AssociatedContextAddResult.Added
        }
    }

    /**
     * 移除指定的关联上下文项。
     *
     * @param item 要移除的上下文项
     */
    fun remove(item: AssociatedContextItem) {
        items.remove(item.key)
    }

    /** 清空所有关联上下文项。 */
    fun clear() {
        items.clear()
    }

    /**
     * 检查是否没有关联上下文项。
     *
     * @return 如果为空返回 true
     */
    fun isEmpty(): Boolean = items.isEmpty()

    /**
     * 获取所有关联文件的路径列表。
     *
     * @return 文件路径列表
     */
    fun referencedFilePaths(): List<String> {
        return items.values
            .filterIsInstance<AssociatedContextItem.AssociatedFile>()
            .map { it.path }
    }

    /**
     * 获取所有关联的代码选区列表。
     *
     * @return 代码选区列表
     */
    fun codeSelections(): List<AssociatedContextItem.AssociatedCodeSelection> {
        return items.values.filterIsInstance<AssociatedContextItem.AssociatedCodeSelection>()
    }
}
