package fr.corentin.biblioscan.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenLibraryApi {

    /** Rich "data" format, keyed by "ISBN:xxxxxxxxxxxxx". */
    @GET("api/books")
    suspend fun getBooksData(
        @Query("bibkeys") bibkeys: String,
        @Query("jscmd") jscmd: String = "data",
        @Query("format") format: String = "json"
    ): JsonObject

    /** Raw edition record - occasionally carries an explicit "series" field. */
    @GET("isbn/{isbn}.json")
    suspend fun getEdition(@Path("isbn") isbn: String): OpenLibraryEdition
}

@Serializable
data class OpenLibraryEdition(
    val title: String? = null,
    val series: List<String>? = null
)

@Serializable
data class OlBookData(
    val title: String? = null,
    val subtitle: String? = null,
    val authors: List<OlAuthor> = emptyList(),
    val publishers: List<OlNamed> = emptyList(),
    val publish_date: String? = null,
    val number_of_pages: Int? = null,
    val subjects: List<OlNamed> = emptyList(),
    val cover: OlCover? = null
)

@Serializable
data class OlAuthor(val name: String? = null)

@Serializable
data class OlNamed(val name: String? = null)

@Serializable
data class OlCover(
    val small: String? = null,
    val medium: String? = null,
    val large: String? = null
)
