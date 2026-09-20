package fr.corentin.biblioscan.ui.library

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.corentin.biblioscan.data.Book
import fr.corentin.biblioscan.data.BookRepository
import fr.corentin.biblioscan.export.LibraryFileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted

data class SeriesGroup(val name: String, val books: List<Book>)

data class LibraryUiState(
    val allBooks: List<Book> = emptyList(),
    val series: List<SeriesGroup> = emptyList(),
    val standalone: List<Book> = emptyList(),
    val searchQuery: String = "",
    val message: String? = null
)

class LibraryViewModel(private val repository: BookRepository) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.observeAll(), searchQuery, message
    ) { books, query, msg ->
        val filtered = if (query.isBlank()) {
            books
        } else {
            books.filter { book ->
                book.title.contains(query, ignoreCase = true) ||
                    book.authors.any { it.contains(query, ignoreCase = true) } ||
                    book.seriesName?.contains(query, ignoreCase = true) == true ||
                    book.isbn.contains(query)
            }
        }
        val (inSeries, standalone) = filtered.partition { it.seriesName != null }
        val series = inSeries.groupBy { it.seriesName!! }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            .map { (name, books) -> SeriesGroup(name, books.sortedBy { it.seriesIndex ?: Double.MAX_VALUE }) }

        LibraryUiState(
            allBooks = books,
            series = series,
            standalone = standalone.sortedBy { it.title.lowercase() },
            searchQuery = query,
            message = msg
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch { repository.delete(book) }
    }

    fun exportLibrary(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching { LibraryFileManager.writeTo(context, uri, uiState.value.allBooks) }
                .onSuccess { message.value = "Bibliothèque exportée (${uiState.value.allBooks.size} livres)" }
                .onFailure { message.value = "Échec de l'export : ${it.message}" }
        }
    }

    fun importLibrary(context: Context, uri: Uri, merge: Boolean) {
        viewModelScope.launch {
            runCatching { LibraryFileManager.readFrom(context, uri) }
                .onSuccess { imported ->
                    if (merge) repository.saveAll(imported) else repository.replaceLibrary(imported)
                    message.value = "${imported.size} livres importés"
                }
                .onFailure { message.value = "Échec de l'import : ${it.message}" }
        }
    }

    fun consumeMessage() {
        message.value = null
    }
}
