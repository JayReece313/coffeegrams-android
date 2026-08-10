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
 * Drives the brew log list. Mirrors the iOS `LogView`'s reactive `@Query`
 * with an explicit [refresh] instead: [BrewLogStoring] is a one-shot
 * `suspend` port (see its own doc comment), not a `Flow`, so this screen
 * reloads on entry and after every mutation rather than observing the store
 * continuously. The nav graph gives this a fresh instance each time the
 * route is entered, which is what makes a plain reload-on-init sufficient.
 */
class LogViewModel(private val store: BrewLogStoring) : ViewModel() {
    private val _entries = MutableStateFlow<List<BrewLogEntry>>(emptyList())
    val entries: StateFlow<List<BrewLogEntry>> = _entries.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { _entries.value = store.entries() }
    }

    fun delete(id: UUID) {
        viewModelScope.launch {
            store.delete(id)
            refresh()
        }
    }
}
