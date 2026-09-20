package fr.corentin.biblioscan.data

import kotlinx.coroutines.flow.Flow

/**
 * Thin wrapper around [BookDao]. Everything here is local (Room/SQLite) -
 * no network calls happen past this point.
 */
class BookRepository(private val dao: BookDao) {

    fun observeAll(): Flow<List<Book>> = dao.observeAll()

    suspend fun getByIsbn(isbn: String): Book? = dao.getByIsbn(isbn)

    fun observeByIsbn(isbn: String): Flow<Book?> = dao.observeByIsbn(isbn)

    suspend fun isOwned(isbn: String): Boolean = dao.exists(isbn)

    suspend fun save(book: Book) = dao.upsert(book)

    suspend fun saveAll(books: List<Book>) = dao.upsertAll(books)

    suspend fun delete(book: Book) = dao.delete(book)

    suspend fun replaceLibrary(books: List<Book>) {
        dao.deleteAll()
        dao.upsertAll(books)
    }

    suspend fun count(): Int = dao.count()
}
