package fr.corentin.biblioscan.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books WHERE libraryId = :libraryId ORDER BY dateAdded DESC")
    fun observeAll(libraryId: String): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE isbn = :isbn AND libraryId = :libraryId LIMIT 1")
    suspend fun getByIsbn(isbn: String, libraryId: String): Book?

    @Query("SELECT * FROM books WHERE isbn = :isbn AND libraryId = :libraryId LIMIT 1")
    fun observeByIsbn(isbn: String, libraryId: String): Flow<Book?>

    @Query("SELECT EXISTS(SELECT 1 FROM books WHERE isbn = :isbn AND libraryId = :libraryId)")
    suspend fun exists(isbn: String, libraryId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(book: Book)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(books: List<Book>)

    @Delete
    suspend fun delete(book: Book)

    @Query("DELETE FROM books WHERE libraryId = :libraryId")
    suspend fun deleteAllIn(libraryId: String)

    @Query("SELECT COUNT(*) FROM books WHERE libraryId = :libraryId")
    suspend fun count(libraryId: String): Int
}
