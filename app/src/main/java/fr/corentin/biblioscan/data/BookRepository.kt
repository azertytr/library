package fr.corentin.biblioscan.data

import kotlinx.coroutines.flow.Flow

/**
 * Thin wrapper around [BookDao]. Everything here is local (Room/SQLite) -
 * no network calls happen past this point. Every method is scoped to a
 * single library ([libraryId]) since the same ISBN can appear in several
 * libraries.
 */
class BookRepository(private val dao: BookDao) {

    fun observeAll(libraryId: String): Flow<List<Book>> = dao.observeAll(libraryId)

    suspend fun getByIsbn(isbn: String, libraryId: String): Book? = dao.getByIsbn(isbn, libraryId)

    fun observeByIsbn(isbn: String, libraryId: String): Flow<Book?> = dao.observeByIsbn(isbn, libraryId)

    suspend fun isOwned(isbn: String, libraryId: String): Boolean = dao.exists(isbn, libraryId)

    suspend fun save(book: Book) = dao.upsert(book)

    suspend fun saveAll(books: List<Book>) = dao.upsertAll(books)

    suspend fun delete(book: Book) = dao.delete(book)

    /** Replaces the entire content of one library, leaving the others untouched. */
    suspend fun replaceLibrary(libraryId: String, books: List<Book>) {
        dao.deleteAllIn(libraryId)
        dao.upsertAll(books)
    }

    suspend fun count(libraryId: String): Int = dao.count(libraryId)
}
