package fr.corentin.biblioscan.ui.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.corentin.biblioscan.data.Book
import fr.corentin.biblioscan.data.BookRepository
import fr.corentin.biblioscan.data.LibraryEntity
import fr.corentin.biblioscan.data.LibraryRepository
import fr.corentin.biblioscan.data.LibrarySortOption
import fr.corentin.biblioscan.data.ModePreferences
import fr.corentin.biblioscan.export.LibraryFileManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted

data class SeriesGroup(val name: String, val books: List<Book>)

data class LibraryUiState(
    val allBooks: List<Book> = emptyList(),
    val series: List<SeriesGroup> = emptyList(),
    val standalone: List<Book> = emptyList(),
    val flatBooks: List<Book> = emptyList(),
    val sortOption: LibrarySortOption = LibrarySortOption.TITLE,
    val searchQuery: String = "",
    val message: String? = null,
    val libraries: List<LibraryEntity> = emptyList(),
    val activeLibraryId: String = "",
) {
    /** Series grouping (with series headers) only applies to the TITLE sort. */
    val isGrouped: Boolean get() = sortOption == LibrarySortOption.TITLE
    val activeLibraryName: String get() = libraries.find { it.id == activeLibraryId }?.name ?: ""
}

private data class Filters(val query: String, val message: String?, val sort: LibrarySortOption)

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val repository: BookRepository,
    private val libraryRepository: LibraryRepository,
    private val modePreferences: ModePreferences
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val message = MutableStateFlow<String?>(null)

    private val booksFlow = modePreferences.activeLibraryId.flatMapLatest { id -> repository.observeAll(id) }
    private val filtersFlow = combine(searchQuery, message, modePreferences.librarySort, ::Filters)

    val uiState: StateFlow<LibraryUiState> = combine(
        booksFlow, libraryRepository.observeAll(), modePreferences.activeLibraryId, filtersFlow
    ) { books, libraries, activeId, filters ->
        val filtered = if (filters.query.isBlank()) {
            books
        } else {
            books.filter { book ->
                book.title.contains(filters.query, ignoreCase = true) ||
                    book.authors.any { it.contains(filters.query, ignoreCase = true) } ||
                    book.seriesName?.contains(filters.query, ignoreCase = true) == true ||
                    book.isbn.contains(filters.query)
            }
        }

        if (filters.sort == LibrarySortOption.TITLE) {
            val (inSeries, standalone) = filtered.partition { it.seriesName != null }
            val series = inSeries.groupBy { it.seriesName!! }
                .toSortedMap(String.CASE_INSENSITIVE_ORDER)
                .map { (name, books) -> SeriesGroup(name, books.sortedBy { it.seriesIndex ?: Double.MAX_VALUE }) }

            LibraryUiState(
                allBooks = books,
                series = series,
                standalone = standalone.sortedBy { it.title.lowercase() },
                sortOption = filters.sort,
                searchQuery = filters.query,
                message = filters.message,
                libraries = libraries,
                activeLibraryId = activeId
            )
        } else {
            val flat = when (filters.sort) {
                LibrarySortOption.DATE_ADDED -> filtered.sortedByDescending { it.dateAdded }
                LibrarySortOption.AUTHOR -> filtered.sortedBy { it.authors.firstOrNull()?.lowercase() ?: "" }
                LibrarySortOption.TITLE -> filtered.sortedBy { it.title.lowercase() }
            }
            LibraryUiState(
                allBooks = books,
                flatBooks = flat,
                sortOption = filters.sort,
                searchQuery = filters.query,
                message = filters.message,
                libraries = libraries,
                activeLibraryId = activeId
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onSortOptionChange(sort: LibrarySortOption) {
        modePreferences.setLibrarySort(sort)
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch { repository.delete(book) }
    }

    fun switchLibrary(id: String) {
        modePreferences.setActiveLibraryId(id)
    }

    fun createLibrary(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val library = libraryRepository.create(trimmed)
            modePreferences.setActiveLibraryId(library.id)
        }
    }

    fun renameLibrary(id: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { libraryRepository.rename(id, trimmed) }
    }

    /** Refuses to delete the last remaining library so there's always one to fall back to. */
    fun deleteLibrary(id: String) {
        viewModelScope.launch {
            if (uiState.value.libraries.size <= 1) {
                message.value = "Impossible de supprimer la seule bibliothèque restante"
                return@launch
            }
            libraryRepository.delete(id)
            if (modePreferences.activeLibraryId.value == id) {
                val fallback = uiState.value.libraries.firstOrNull { it.id != id }?.id
                if (fallback != null) modePreferences.setActiveLibraryId(fallback)
            }
        }
    }

    /** Manual "Exporter" (SAF): backs up the currently active library to a chosen location. */
    fun exportLibrary(context: Context, uri: Uri) {
        viewModelScope.launch {
            val state = uiState.value
            runCatching { LibraryFileManager.writeTo(context, uri, state.activeLibraryId, state.activeLibraryName, state.allBooks) }
                .onSuccess { message.value = "Bibliothèque exportée (${state.allBooks.size} livres)" }
                .onFailure { message.value = "Échec de l'export : ${it.message}" }
        }
    }

    /** Builds a share-sheet intent (any messaging/email app) for the active library. */
    fun shareLibrary(context: Context, onReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val state = uiState.value
            runCatching {
                LibraryFileManager.prepareShareFile(context, state.activeLibraryId, state.activeLibraryName, state.allBooks)
            }.onSuccess { uri ->
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                onReady(Intent.createChooser(sendIntent, "Partager la bibliothèque"))
            }.onFailure {
                message.value = "Échec du partage : ${it.message}"
            }
        }
    }

    /**
     * Imports a library JSON file, whether picked manually or opened from
     * another app (a shared file). If a library with the same id is already
     * known locally it's refreshed in place; otherwise a new named library
     * is created - either way it becomes the active one.
     */
    fun importSharedLibrary(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching { LibraryFileManager.readFrom(context, uri) }
                .onSuccess { decoded ->
                    val existing = libraryRepository.get(decoded.id)
                    if (existing == null) {
                        libraryRepository.upsert(LibraryEntity(decoded.id, decoded.name, System.currentTimeMillis()))
                    } else if (existing.name != decoded.name) {
                        libraryRepository.rename(decoded.id, decoded.name)
                    }
                    repository.replaceLibrary(decoded.id, decoded.books.map { it.copy(libraryId = decoded.id) })
                    modePreferences.setActiveLibraryId(decoded.id)
                    message.value = "Bibliothèque « ${decoded.name} » chargée (${decoded.books.size} livres)"
                }
                .onFailure { message.value = "Fichier invalide : ${it.message}" }
        }
    }

    fun consumeMessage() {
        message.value = null
    }
}
