package com.miracle.ui.core

internal sealed interface AssociatedContextAddResult {
    data object Added : AssociatedContextAddResult
    data class Existing(val key: String) : AssociatedContextAddResult
}

internal class AssociatedContextState {
    private val items = linkedMapOf<String, AssociatedContextItem>()

    fun items(): List<AssociatedContextItem> = items.values.toList()

    fun add(item: AssociatedContextItem): AssociatedContextAddResult {
        val existing = items[item.key]
        return if (existing != null) {
            AssociatedContextAddResult.Existing(item.key)
        } else {
            items[item.key] = item
            AssociatedContextAddResult.Added
        }
    }

    fun remove(item: AssociatedContextItem) {
        items.remove(item.key)
    }

    fun clear() {
        items.clear()
    }

    fun isEmpty(): Boolean = items.isEmpty()

    fun referencedFilePaths(): List<String> {
        return items.values
            .filterIsInstance<AssociatedContextItem.AssociatedFile>()
            .map { it.path }
    }

    fun codeSelections(): List<AssociatedContextItem.AssociatedCodeSelection> {
        return items.values.filterIsInstance<AssociatedContextItem.AssociatedCodeSelection>()
    }
}
