package fr.corentin.biblioscan

import android.content.Context
import fr.corentin.biblioscan.data.AppDatabase
import fr.corentin.biblioscan.data.BookRepository
import fr.corentin.biblioscan.data.LibraryRepository
import fr.corentin.biblioscan.data.ModePreferences
import fr.corentin.biblioscan.network.BookLookupService

/**
 * Minimal manual dependency container - no DI framework needed for an
 * app this size. Initialized once from [BiblioScanApp].
 */
object AppContainer {
    lateinit var repository: BookRepository
        private set

    lateinit var libraryRepository: LibraryRepository
        private set

    lateinit var modePreferences: ModePreferences
        private set

    val lookupService: BookLookupService by lazy { BookLookupService() }

    fun init(context: Context) {
        if (::repository.isInitialized) return
        val db = AppDatabase.getInstance(context)
        repository = BookRepository(db.bookDao())
        libraryRepository = LibraryRepository(db.libraryDao())
        modePreferences = ModePreferences(context.applicationContext)
    }
}
