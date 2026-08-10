package com.jrlabapps.coffeegrams.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jrlabapps.coffeegrams.core.BrewLogEntry
import com.jrlabapps.coffeegrams.core.BrewLogStoring
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Drives the brew log detail screen: one saved brew, its rating, and its
 * notes. Mirrors iOS's `LogDetailView`, which binds straight to a SwiftData
 * `@Bindable` model — here every mutation is routed explicitly through
 * [BrewLogStoring], same as [LogViewModel], since there is no equivalent
 * live-object binding on this side of the port.
 */
class LogDetailViewModel(private val entryId: UUID, private val store: BrewLogStoring) : ViewModel() {
    private val _entry = MutableStateFlow<BrewLogEntry?>(null)
    val entry: StateFlow<BrewLogEntry?> = _entry.asStateFlow()

    init {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch { _entry.value = store.entries().firstOrNull { it.id == entryId } }
    }

    /** 0 clears the rating back to unrated, matching [com.jrlabapps.coffeegrams.ui.log.StarRating]'s tap-to-clear behavior. */
    fun setRating(rating: Int) {
        val newRating = if (rating == 0) null else rating
        _entry.value = _entry.value?.copy(rating = newRating)
        viewModelScope.launch { store.setRating(newRating, entryId) }
    }

    /** Persist the notes draft once, trimmed — called when the screen is left or backgrounded, not on every keystroke. */
    fun saveNotes(notes: String) {
        val trimmed = notes.trim().ifEmpty { null }
        viewModelScope.launch { store.setNotes(trimmed, entryId) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            store.delete(entryId)
            onDeleted()
        }
    }
}
