package fr.corentin.biblioscan.export

import fr.corentin.biblioscan.data.Book
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class LibraryExport(
    val formatVersion: Int = 2,
    /** Absent on files exported before multi-library support (formatVersion 1). */
    val libraryId: String? = null,
    val libraryName: String? = null,
    val exportedAt: Long,
    val bookCount: Int,
    val books: List<Book>
)

/** A library decoded from a shared file, with its identity resolved. */
data class DecodedLibrary(val id: String, val name: String, val books: List<Book>)

/** Pure JSON (de)serialization for one library - no I/O here. */
object LibraryJsonCodec {

    private const val FALLBACK_NAME = "Bibliothèque importée"

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(libraryId: String, libraryName: String, books: List<Book>, exportedAt: Long): String =
        json.encodeToString(
            LibraryExport.serializer(),
            LibraryExport(
                libraryId = libraryId,
                libraryName = libraryName,
                exportedAt = exportedAt,
                bookCount = books.size,
                books = books
            )
        )

    /**
     * Throws [kotlinx.serialization.SerializationException] on malformed input.
     * Files exported before multi-library support (no `libraryId`/`libraryName`)
     * are treated as a new, unnamed library.
     */
    fun decode(content: String): DecodedLibrary {
        val export = json.decodeFromString(LibraryExport.serializer(), content)
        val id = export.libraryId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val name = export.libraryName?.takeIf { it.isNotBlank() } ?: FALLBACK_NAME
        return DecodedLibrary(id, name, export.books)
    }
}
