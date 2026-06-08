package com.arms.ke3

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object NotificationStore {
    private val _items = MutableStateFlow<List<String>>(emptyList())
    val items: StateFlow<List<String>> = _items
    fun add(text: String) { _items.value = (listOf(text) + _items.value).take(50) }
}
