package fr.corentin.biblioscan.network

import fr.corentin.biblioscan.data.Book
import fr.corentin.biblioscan.util.IsbnUtils
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.IOException

sealed interface LookupResult {
    data class Found(val book: Book) : LookupResult
    data object NotFound : LookupResult
    data class Error(val message: String) : LookupResult
}

/**
 * Fetches book metadata for a scanned ISBN. Google Books is tried first
 * (best coverage, good cover images); Open Library is the fallback when
 * Google Books has nothing. Series info is enriched, best-effort, from
 * Open Library's edition record regardless of which source won.
 *
 * This is the only place in the app that talks to the network - the rest
 * of the app (collection, export/import) is fully local.
 */
class BookLookupService(
    private val googleBooksApi: GoogleBooksApi = NetworkModule.googleBooksApi,
    private val openLibraryApi: OpenLibraryApi = NetworkModule.openLibraryApi
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    suspend fun lookup(rawIsbn: String): LookupResult {
        val isbn13 = IsbnUtils.toIsbn13(rawIsbn)

        // Google Books is tried first but its failures (rate limiting, timeouts...) must not
        // abort the lookup - Open Library is a real fallback, not just a "no match" fallback.
        val googleResult = runCatching { fromGoogleBooks(isbn13) }

        val draft = try {
            googleResult.getOrNull() ?: fromOpenLibrary(isbn13)
        } catch (e: IOException) {
            return LookupResult.Error(e.message ?: "Erreur réseau")
        } catch (e: Exception) {
            return LookupResult.Error(e.message ?: "Erreur inattendue")
        } ?: return googleResult.exceptionOrNull()?.let {
            LookupResult.Error(it.message ?: "Erreur inattendue")
        } ?: LookupResult.NotFound

        val seriesRaw = runCatching { openLibraryApi.getEdition(isbn13).series?.firstOrNull() }
            .getOrNull()

        val series = SeriesDetector.detect(draft.title, draft.subtitle, seriesRaw)

        return LookupResult.Found(
            draft.copy(
                seriesName = series?.name,
                seriesIndex = series?.index
            )
        )
    }

    private suspend fun fromGoogleBooks(isbn13: String): Book? {
        val response = googleBooksApi.search("isbn:$isbn13")
        val info = response.items.firstOrNull { it.volumeInfo?.title != null }?.volumeInfo ?: return null
        return Book(
            isbn = isbn13,
            title = info.title.orEmpty(),
            subtitle = info.subtitle,
            authors = info.authors,
            publisher = info.publisher,
            publishedDate = info.publishedDate,
            description = info.description,
            pageCount = info.pageCount,
            categories = info.categories,
            coverUrl = info.imageLinks?.thumbnail?.replace("http://", "https://"),
            source = "google_books"
        )
    }

    private suspend fun fromOpenLibrary(isbn13: String): Book? {
        val response = openLibraryApi.getBooksData("ISBN:$isbn13")
        val entry = response["ISBN:$isbn13"]?.jsonObject ?: return null
        val data = json.decodeFromJsonElement(OlBookData.serializer(), entry)
        if (data.title.isNullOrBlank()) return null
        return Book(
            isbn = isbn13,
            title = data.title,
            subtitle = data.subtitle,
            authors = data.authors.mapNotNull { it.name },
            publisher = data.publishers.mapNotNull { it.name }.firstOrNull(),
            publishedDate = data.publish_date,
            description = null,
            pageCount = data.number_of_pages,
            categories = data.subjects.mapNotNull { it.name },
            coverUrl = data.cover?.large ?: data.cover?.medium ?: data.cover?.small,
            source = "open_library"
        )
    }
}
