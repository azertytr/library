package fr.corentin.biblioscan.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.corentin.biblioscan.data.Book
import fr.corentin.biblioscan.data.BookRepository
import fr.corentin.biblioscan.data.ModePreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BookDetailViewModel(
    private val repository: BookRepository,
    modePreferences: ModePreferences,
    isbn: String
) : ViewModel() {

    val book: StateFlow<Book?> = modePreferences.activeLibraryId
        .flatMapLatest { libraryId -> repository.observeByIsbn(isbn, libraryId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun delete(book: Book, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.delete(book)
            onDone()
        }
    }
}
