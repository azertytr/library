package fr.corentin.biblioscan.network

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

private fun httpException(code: Int): HttpException =
    HttpException(Response.error<Any>(code, "".toResponseBody("application/json".toMediaType())))

private class FakeGoogleBooksApi(private val result: () -> GoogleBooksResponse) : GoogleBooksApi {
    override suspend fun search(query: String): GoogleBooksResponse = result()
}

private class FakeOpenLibraryApi(
    private val booksData: () -> JsonObject = { buildJsonObject {} },
    private val edition: () -> OpenLibraryEdition = { OpenLibraryEdition() }
) : OpenLibraryApi {
    override suspend fun getBooksData(bibkeys: String, jscmd: String, format: String): JsonObject = booksData()
    override suspend fun getEdition(isbn: String): OpenLibraryEdition = edition()
}

/**
 * Regression coverage for the scan error card showing raw HTTP status text
 * ("404", "503") to the user instead of a friendly message.
 */
class BookLookupServiceTest {

    private val isbn = "9782070368228"

    @Test
    fun `a source that cleanly finds nothing yields NotFound even if the other source errors`() = runBlocking {
        val service = BookLookupService(
            googleBooksApi = FakeGoogleBooksApi { throw httpException(503) },
            openLibraryApi = FakeOpenLibraryApi(booksData = { buildJsonObject {} })
        )

        val result = service.lookup(isbn)

        assertTrue("expected NotFound, got $result", result is LookupResult.NotFound)
    }

    @Test
    fun `a 404 from the fallback source is not shown as an error when the primary source found nothing`() = runBlocking {
        val service = BookLookupService(
            googleBooksApi = FakeGoogleBooksApi { GoogleBooksResponse() },
            openLibraryApi = FakeOpenLibraryApi(booksData = { throw httpException(404) })
        )

        val result = service.lookup(isbn)

        assertTrue("expected NotFound, got $result", result is LookupResult.NotFound)
    }

    @Test
    fun `when every source fails outright the error message never leaks a raw HTTP status`() = runBlocking {
        val service = BookLookupService(
            googleBooksApi = FakeGoogleBooksApi { throw httpException(503) },
            openLibraryApi = FakeOpenLibraryApi(booksData = { throw httpException(503) })
        )

        val result = service.lookup(isbn)

        assertTrue("expected Error, got $result", result is LookupResult.Error)
        val message = (result as LookupResult.Error).message
        assertFalse("must not leak the raw status code", message.contains("503"))
        assertFalse("must not leak the raw exception text", message.contains("HTTP"))
    }

    @Test
    fun `a 404 from every source also never leaks the raw status`() = runBlocking {
        val service = BookLookupService(
            googleBooksApi = FakeGoogleBooksApi { throw httpException(404) },
            openLibraryApi = FakeOpenLibraryApi(booksData = { throw httpException(404) })
        )

        val result = service.lookup(isbn)

        assertTrue("expected Error, got $result", result is LookupResult.Error)
        assertFalse((result as LookupResult.Error).message.contains("404"))
    }
}
