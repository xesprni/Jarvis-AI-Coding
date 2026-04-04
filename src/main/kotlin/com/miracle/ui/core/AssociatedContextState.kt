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
 * 管理聊天输入框的关联上下文状态，包括文件引用、代码选区和系统推荐文件。
 */
internal class AssociatedContextState {
    /** 已添加的关联上下文项映射，键为唯一标识 */
    private val items = linkedMapOf<String, AssociatedContextItem>()
    /** 系统预测推荐的文件列表（置灰显示，点击后加入 items） */
    private val predictedItems = linkedMapOf<String, AssociatedContextItem.AssociatedFile>()
    /** 自动关联代码选区的固定 key，同一时间只保留一个 */
    private val AUTO_CODE_SELECTION_KEY = "auto-code-selection"

    /**
     * 获取所有已选中的关联上下文项（不含推荐文件）。
     *
     * @return 上下文项列表
     */
    fun items(): List<AssociatedContextItem> = items.values.toList()

    /**
     * 获取所有推荐文件（未选中的预测项）。
     *
     * @return 推荐文件列表
     */
    fun predictedFiles(): List<AssociatedContextItem.AssociatedFile> = predictedItems.values.toList()

    /**
     * 获取所有项（已选中 + 推荐），用于渲染。
     *
     * @return 全部上下文项列表
     */
    fun allItems(): List<AssociatedContextItem> {
        return items.values.toList() + predictedItems.values.toList()
    }

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

    /** 清空所有关联上下文项和推荐文件。 */
    fun clear() {
        items.clear()
        predictedItems.clear()
    }

    /**
     * 检查是否没有关联上下文项。
     *
     * @return 如果为空返回 true
     */
    fun isEmpty(): Boolean = items.isEmpty()

    /**
     * 获取所有关联文件的路径列表（仅已选中的文件）。
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

    /**
     * 更新推荐的文件列表，替换之前的所有推荐。
     * 过滤掉已选中的文件和已存在的推荐文件。
     *
     * @param files 新的推荐文件列表
     */
    fun setPredictedFiles(files: List<AssociatedContextItem.AssociatedFile>) {
        predictedItems.clear()
        files.forEach { file ->
            if (!items.containsKey(file.key)) {
                predictedItems[file.key] = file
            }
        }
    }

    /**
     * 确认一个推荐文件，将其从推荐列表移到已选中列表。
     *
     * @param file 要确认的推荐文件
     * @return 是否成功确认（推荐列表中不存在则返回 false）
     */
    fun confirmPredicted(file: AssociatedContextItem.AssociatedFile): Boolean {
        if (!predictedItems.containsKey(file.key)) return false
        predictedItems.remove(file.key)
        items[file.key] = file.copy(suggested = false, sourceLabel = null)
        return true
    }

    /**
     * 移除一个推荐文件。
     *
     * @param file 要移除的推荐文件
     */
    fun removePredicted(file: AssociatedContextItem.AssociatedFile) {
        predictedItems.remove(file.key)
    }

    /**
     * 自动关联代码选区：直接加入已选中列表（替换之前的自动选区）。
     */
    fun setAutoCodeSelection(selection: AssociatedContextItem.AssociatedCodeSelection) {
        items.remove(AUTO_CODE_SELECTION_KEY)
        items[AUTO_CODE_SELECTION_KEY] = selection
    }

    /**
     * 移除自动关联的代码选区（失焦时调用）。
     */
    fun removeAutoCodeSelection() {
        items.remove(AUTO_CODE_SELECTION_KEY)
    }
}
