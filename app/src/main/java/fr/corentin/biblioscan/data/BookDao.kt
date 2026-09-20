package fr.corentin.biblioscan.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY dateAdded DESC")
    fun observeAll(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE isbn = :isbn LIMIT 1")
    suspend fun getByIsbn(isbn: String): Book?

    @Query("SELECT * FROM books WHERE isbn = :isbn LIMIT 1")
    fun observeByIsbn(isbn: String): Flow<Book?>

    @Query("SELECT EXISTS(SELECT 1 FROM books WHERE isbn = :isbn)")
    suspend fun exists(isbn: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(book: Book)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(books: List<Book>)

    @Delete
    suspend fun delete(book: Book)

    @Query("DELETE FROM books")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM books")
    suspend fun count(): Int
}
