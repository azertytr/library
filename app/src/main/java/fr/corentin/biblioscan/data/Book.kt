package fr.corentin.biblioscan.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A single book in the local collection, keyed by normalized ISBN-13
 * within its owning library ([libraryId]) - the same ISBN can exist in
 * several libraries. All fields besides [isbn] and [title] are
 * best-effort metadata pulled from an online lookup at scan time;
 * nothing here requires network access afterwards.
 *
 * [libraryId] is excluded from JSON export ([Transient]): a shared file
 * carries the library's id/name once at the top level instead.
 */
@Entity(
    tableName = "books",
    primaryKeys = ["isbn", "libraryId"],
    foreignKeys = [
        ForeignKey(
            entity = LibraryEntity::class,
            parentColumns = ["id"],
            childColumns = ["libraryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("libraryId")]
)
@Serializable
data class Book(
    val isbn: String,
    @Transient val libraryId: String = DEFAULT_LIBRARY_ID,
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
