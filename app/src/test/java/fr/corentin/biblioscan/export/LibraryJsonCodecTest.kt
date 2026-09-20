package fr.corentin.biblioscan.export

import fr.corentin.biblioscan.data.Book
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryJsonCodecTest {

    private val sampleBooks = listOf(
        Book(isbn = "9780000000001", libraryId = "irrelevant-at-encode-time", title = "Livre A"),
        Book(isbn = "9780000000002", libraryId = "irrelevant-at-encode-time", title = "Livre B", authors = listOf("Autrice X"))
    )

    @Test
    fun `round trip keeps the library identity and its books`() {
        val json = LibraryJsonCodec.encode("lib-corentin", "Bibliothèque de Corentin", sampleBooks, exportedAt = 42L)

        val decoded = LibraryJsonCodec.decode(json)

        assertEquals("lib-corentin", decoded.id)
        assertEquals("Bibliothèque de Corentin", decoded.name)
        assertEquals(sampleBooks.map { it.isbn }, decoded.books.map { it.isbn })
        assertEquals(sampleBooks.map { it.title }, decoded.books.map { it.title })
    }

    @Test
    fun `libraryId is never written into the shared JSON`() {
        val json = LibraryJsonCodec.encode("lib-corentin", "Bibliothèque de Corentin", sampleBooks, exportedAt = 42L)

        assertFalse("the internal libraryId must not leak into the shared file", json.contains("irrelevant-at-encode-time"))
    }

    @Test
    fun `a pre-multi-library export (no libraryId or libraryName) becomes a new named library`() {
        val legacyJson = """
            {
                "formatVersion": 1,
                "exportedAt": 1,
                "bookCount": 1,
                "books": [
                    {"isbn": "9780000000003", "title": "Vieux livre", "authors": [], "categories": []}
                ]
            }
        """.trimIndent()

        val decoded = LibraryJsonCodec.decode(legacyJson)

        assertTrue(decoded.id.isNotBlank())
        assertTrue(decoded.name.isNotBlank())
        assertEquals(1, decoded.books.size)
        assertEquals("9780000000003", decoded.books.first().isbn)
    }

    @Test
    fun `two legacy imports get different synthesized ids`() {
        val legacyJson = """
            {"formatVersion": 1, "exportedAt": 1, "bookCount": 0, "books": []}
        """.trimIndent()

        val first = LibraryJsonCodec.decode(legacyJson)
        val second = LibraryJsonCodec.decode(legacyJson)

        assertNotEquals(first.id, second.id)
    }

    @Test(expected = SerializationException::class)
    fun `malformed JSON is rejected instead of crashing the app`() {
        LibraryJsonCodec.decode("{ not valid json at all")
    }
}
