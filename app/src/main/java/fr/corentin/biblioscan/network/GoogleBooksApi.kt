package fr.corentin.biblioscan.network

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleBooksApi {
    @GET("books/v1/volumes")
    suspend fun search(@Query("q") query: String): GoogleBooksResponse
}

@Serializable
data class GoogleBooksResponse(
    val totalItems: Int = 0,
    val items: List<GoogleBookItem> = emptyList()
)

@Serializable
data class GoogleBookItem(
    val volumeInfo: GoogleVolumeInfo? = null
)

@Serializable
data class GoogleVolumeInfo(
    val title: String? = null,
    val subtitle: String? = null,
    val authors: List<String> = emptyList(),
    val publisher: String? = null,
    val publishedDate: String? = null,
    val description: String? = null,
    val pageCount: Int? = null,
    val categories: List<String> = emptyList(),
    val imageLinks: GoogleImageLinks? = null,
    val industryIdentifiers: List<GoogleIndustryIdentifier> = emptyList()
)

@Serializable
data class GoogleImageLinks(
    val smallThumbnail: String? = null,
    val thumbnail: String? = null
)

@Serializable
data class GoogleIndustryIdentifier(
    val type: String? = null,
    val identifier: String? = null
)
