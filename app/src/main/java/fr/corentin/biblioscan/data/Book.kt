package fr.corentin.biblioscan.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A single book in the local collection, keyed by normalized ISBN-13.
 * All fields besides [isbn] and [title] are best-effort metadata pulled
 * from an online lookup at scan time; nothing here requires network
 * access afterwards.
 */
@Entity(tableName = "books")
@Serializable
data class Book(
    @PrimaryKey val isbn: String,
    val title: String,
    val subtitle: String? = null,
    val authors: List<String> = emptyList(),
    val publisher: String? = null,
    val publishedDate: String? = null,
    val description: String? = null,
    val pageCount: Int? = null,
    val categories: List<String> = emptyList(),
    val coverUrl: String? = null,
    val seriesName: String? = null,
    val seriesIndex: Double? = null,
    val dateAdded: Long = 0L,
    val source: String? = null
)
