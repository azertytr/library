package fr.corentin.biblioscan.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    @Query("SELECT * FROM libraries ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<LibraryEntity>>

    @Query("SELECT * FROM libraries WHERE id = :id LIMIT 1")
    suspend fun get(id: String): LibraryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(library: LibraryEntity)

    @Query("UPDATE libraries SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    /** Cascades to that library's books via the FK in [Book]. */
    @Query("DELETE FROM libraries WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM books WHERE libraryId = :id")
    suspend fun countBooks(id: String): Int
}
