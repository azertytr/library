package fr.corentin.biblioscan.network

import fr.corentin.biblioscan.data.Book
import fr.corentin.biblioscan.util.IsbnUtils
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import retrofit2.HttpException
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
        val openLibraryResult = if (googleResult.getOrNull() == null) {
            runCatching { fromOpenLibrary(isbn13) }
        } else null

        // A source that actually ran and came back empty means the book genuinely isn't
        // listed there - that's a real NotFound, not an error from the *other* source.
        // Only surface an Error when every source we tried failed outright, and never leak
        // a raw HTTP status/exception message to the UI.
        val draft = googleResult.getOrNull() ?: openLibraryResult?.getOrNull()
            ?: return if (googleResult.isFailure && openLibraryResult?.isFailure == true) {
                LookupResult.Error(friendlyMessage(openLibraryResult.exceptionOrNull()!!))
            } else {
                LookupResult.NotFound
            }

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

    /** Turns a network failure into a message a user can act on - never a raw HTTP status/exception text. */
    private fun friendlyMessage(e: Throwable): String = when {
        e is HttpException && e.code() in 500..599 ->
            "Le service de recherche est temporairement indisponible. Réessaie dans quelques instants."
        e is HttpException ->
            "Le service de recherche est indisponible pour le moment."
        e is IOException ->
            "Connexion impossible. Vérifie ta connexion internet."
        else ->
            "Erreur inattendue lors de la recherche."
    }
}
