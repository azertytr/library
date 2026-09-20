package fr.corentin.biblioscan.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.corentin.biblioscan.data.Book
import fr.corentin.biblioscan.data.BookRepository
import fr.corentin.biblioscan.network.BookLookupService
import fr.corentin.biblioscan.network.LookupResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    data object Scanning : ScanUiState
    data class Loading(val isbn: String) : ScanUiState
    data class Found(val book: Book, val alreadyOwned: Boolean) : ScanUiState
    data class NotFound(val isbn: String) : ScanUiState
    data class Error(val isbn: String, val message: String) : ScanUiState
}

class ScanViewModel(
    private val repository: BookRepository,
    private val lookupService: BookLookupService
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Scanning)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    /** True while a scan result is on screen, so the analyzer can pause. */
    val isPaused: Boolean get() = _uiState.value !is ScanUiState.Scanning

    fun onIsbnDetected(isbn: String) {
        if (isPaused) return
        _uiState.value = ScanUiState.Loading(isbn)
        viewModelScope.launch {
            when (val result = lookupService.lookup(isbn)) {
                is LookupResult.Found -> {
                    val owned = repository.isOwned(result.book.isbn)
                    _uiState.value = ScanUiState.Found(result.book, owned)
                }
                is LookupResult.NotFound -> _uiState.value = ScanUiState.NotFound(isbn)
                is LookupResult.Error -> _uiState.value = ScanUiState.Error(isbn, result.message)
            }
        }
    }

    fun addToCollection(book: Book) {
        viewModelScope.launch {
            repository.save(book.copy(dateAdded = System.currentTimeMillis()))
            val current = _uiState.value
            if (current is ScanUiState.Found) {
                _uiState.value = current.copy(alreadyOwned = true)
            }
        }
    }

    fun removeFromCollection(book: Book) {
        viewModelScope.launch {
            repository.delete(book)
            val current = _uiState.value
            if (current is ScanUiState.Found) {
                _uiState.value = current.copy(alreadyOwned = false)
            }
        }
    }

    fun resumeScanning() {
        _uiState.value = ScanUiState.Scanning
    }
}
