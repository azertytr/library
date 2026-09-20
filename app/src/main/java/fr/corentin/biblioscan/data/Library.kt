package fr.corentin.biblioscan.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Id of the library every fresh install starts with. */
const val DEFAULT_LIBRARY_ID = "default"

/**
 * A named collection of books (e.g. "Bibliothèque de Corentin"). Every
 * [Book] belongs to exactly one library via [Book.libraryId].
 */
@Entity(tableName = "libraries")
data class LibraryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long
)
