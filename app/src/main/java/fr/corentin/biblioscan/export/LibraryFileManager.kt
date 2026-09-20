package fr.corentin.biblioscan.export

import android.content.Context
import android.net.Uri
import fr.corentin.biblioscan.data.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Reads/writes the library JSON file through Android's Storage Access
 * Framework, so the user picks the destination/source themselves -
 * nothing is uploaded anywhere, it's a plain local file.
 */
object LibraryFileManager {

    suspend fun writeTo(context: Context, uri: Uri, books: List<Book>) = withContext(Dispatchers.IO) {
        val json = LibraryJsonCodec.encode(books, exportedAt = System.currentTimeMillis())
        context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
            output.write(json.toByteArray(Charsets.UTF_8))
        } ?: error("Impossible d'ouvrir le fichier en écriture")
    }

    suspend fun readFrom(context: Context, uri: Uri): List<Book> = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val text = BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
            LibraryJsonCodec.decode(text)
        } ?: error("Impossible d'ouvrir le fichier en lecture")
    }
}
