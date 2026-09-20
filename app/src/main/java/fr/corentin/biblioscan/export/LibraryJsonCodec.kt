package fr.corentin.biblioscan.export

import fr.corentin.biblioscan.data.Book
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LibraryExport(
    val formatVersion: Int = 1,
    val exportedAt: Long,
    val bookCount: Int,
    val books: List<Book>
)

/** Pure JSON (de)serialization for the whole local library - no I/O here. */
object LibraryJsonCodec {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(books: List<Book>, exportedAt: Long): String =
        json.encodeToString(
            LibraryExport.serializer(),
            LibraryExport(exportedAt = exportedAt, bookCount = books.size, books = books)
        )

    /** Throws [kotlinx.serialization.SerializationException] on malformed input. */
    fun decode(content: String): List<Book> =
        json.decodeFromString(LibraryExport.serializer(), content).books
}
