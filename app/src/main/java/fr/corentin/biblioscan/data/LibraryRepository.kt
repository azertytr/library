package fr.corentin.biblioscan.data

import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Thin wrapper around [LibraryDao] - manages the set of named libraries. */
class LibraryRepository(private val dao: LibraryDao) {

    fun observeAll(): Flow<List<LibraryEntity>> = dao.observeAll()

    suspend fun get(id: String): LibraryEntity? = dao.get(id)

    suspend fun create(name: String): LibraryEntity {
        val library = LibraryEntity(id = UUID.randomUUID().toString(), name = name, createdAt = System.currentTimeMillis())
        dao.upsert(library)
        return library
    }

    suspend fun upsert(library: LibraryEntity) = dao.upsert(library)

    suspend fun rename(id: String, name: String) = dao.rename(id, name)

    suspend fun delete(id: String) = dao.delete(id)

    suspend fun countBooks(id: String): Int = dao.countBooks(id)
}
