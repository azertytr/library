package fr.corentin.biblioscan.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Collection: each scan is saved to the local library.
 * Brocante: each scan only checks ownership, nothing is written.
 */
enum class AppMode {
    COLLECTION, BROCANTE
}

/** How the library list is ordered and grouped. */
enum class LibrarySortOption {
    TITLE, DATE_ADDED, AUTHOR
}

/** Persists the active [AppMode] across app restarts via SharedPreferences. */
class ModePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("biblioscan_prefs", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(
        runCatching { AppMode.valueOf(prefs.getString(KEY_MODE, AppMode.COLLECTION.name)!!) }
            .getOrDefault(AppMode.COLLECTION)
    )
    val mode: StateFlow<AppMode> = _mode.asStateFlow()

    fun setMode(mode: AppMode) {
        _mode.value = mode
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    private val _librarySort = MutableStateFlow(
        runCatching { LibrarySortOption.valueOf(prefs.getString(KEY_SORT, LibrarySortOption.TITLE.name)!!) }
            .getOrDefault(LibrarySortOption.TITLE)
    )
    val librarySort: StateFlow<LibrarySortOption> = _librarySort.asStateFlow()

    fun setLibrarySort(sort: LibrarySortOption) {
        _librarySort.value = sort
        prefs.edit().putString(KEY_SORT, sort.name).apply()
    }

    companion object {
        private const val KEY_MODE = "app_mode"
        private const val KEY_SORT = "library_sort"
    }
}
