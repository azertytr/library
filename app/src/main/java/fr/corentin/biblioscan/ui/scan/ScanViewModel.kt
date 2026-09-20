package fr.corentin.biblioscan.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.corentin.biblioscan.data.AppMode
import fr.corentin.biblioscan.data.Book
import fr.corentin.biblioscan.data.BookRepository
import fr.corentin.biblioscan.data.LibraryRepository
import fr.corentin.biblioscan.data.ModePreferences
import fr.corentin.biblioscan.network.BookLookupService
import fr.corentin.biblioscan.network.LookupResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    data object Scanning : ScanUiState
    data class Loading(val isbn: String) : ScanUiState
    data class Found(val book: Book, val alreadyOwned: Boolean, val justAdded: Boolean = false) : ScanUiState
    data class NotFound(val isbn: String) : ScanUiState
    data class Error(val isbn: String, val message: String) : ScanUiState
}

class ScanViewModel(
    private val repository: BookRepository,
    private val lookupService: BookLookupService,
    private val modePreferences: ModePreferences,
    libraryRepository: LibraryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Scanning)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    val mode: StateFlow<AppMode> = modePreferences.mode

    /** Shown in the top bar so it's always clear which library a scan will affect. */
    val activeLibraryName: StateFlow<String> = combine(
        modePreferences.activeLibraryId, libraryRepository.observeAll()
    ) { id, libraries -> libraries.find { it.id == id }?.name.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    /** True while a scan result is on screen, so the analyzer can pause. */
    val isPaused: Boolean get() = _uiState.value !is ScanUiState.Scanning

    fun setMode(mode: AppMode) = modePreferences.setMode(mode)

    fun onIsbnDetected(isbn: String) {
        if (isPaused) return
        _uiState.value = ScanUiState.Loading(isbn)
        viewModelScope.launch {
            val libraryId = modePreferences.activeLibraryId.value
            when (val result = lookupService.lookup(isbn)) {
                is LookupResult.Found -> {
                    val owned = repository.isOwned(result.book.isbn, libraryId)
                    if (mode.value == AppMode.COLLECTION && !owned) {
                        repository.save(result.book.copy(dateAdded = System.currentTimeMillis(), libraryId = libraryId))
                        _uiState.value = ScanUiState.Found(result.book, alreadyOwned = true, justAdded = true)
                    } else {
                        _uiState.value = ScanUiState.Found(result.book, alreadyOwned = owned)
                    }
                }
                is LookupResult.NotFound -> _uiState.value = ScanUiState.NotFound(isbn)
                is LookupResult.Error -> _uiState.value = ScanUiState.Error(isbn, result.message)
            }
        }
    }

    fun removeFromCollection(book: Book) {
        viewModelScope.launch {
            repository.delete(book)
            val current = _uiState.value
            if (current is ScanUiState.Found) {
                _uiState.value = current.copy(alreadyOwned = false, justAdded = false)
            }
        }
    }

    fun resumeScanning() {
        _uiState.value = ScanUiState.Scanning
    }
}
