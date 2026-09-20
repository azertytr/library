package fr.corentin.biblioscan.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.corentin.biblioscan.data.Book
import fr.corentin.biblioscan.data.BookRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookDetailViewModel(private val repository: BookRepository, isbn: String) : ViewModel() {

    val book: StateFlow<Book?> = repository.observeByIsbn(isbn)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun delete(book: Book, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.delete(book)
            onDone()
        }
    }
}
