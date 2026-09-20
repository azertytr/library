package fr.corentin.biblioscan.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import fr.corentin.biblioscan.data.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Reads/writes a library JSON file. [writeTo]/[readFrom] go through
 * Android's Storage Access Framework, so the user picks the
 * destination/source themselves - nothing is uploaded anywhere, it's a
 * plain local file. [prepareShareFile] instead stages the file in the
 * app's own cache to hand off to any other app (messaging, email, ...)
 * via a content:// Uri.
 */
object LibraryFileManager {

    suspend fun writeTo(context: Context, uri: Uri, libraryId: String, libraryName: String, books: List<Book>) =
        withContext(Dispatchers.IO) {
            val json = LibraryJsonCodec.encode(libraryId, libraryName, books, exportedAt = System.currentTimeMillis())
            context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                output.write(json.toByteArray(Charsets.UTF_8))
            } ?: error("Impossible d'ouvrir le fichier en écriture")
        }

    suspend fun readFrom(context: Context, uri: Uri): DecodedLibrary = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val text = BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
            LibraryJsonCodec.decode(text)
        } ?: error("Impossible d'ouvrir le fichier en lecture")
    }

    /** Writes the library to `cacheDir/shared/<name>.json` and returns a shareable content:// Uri. */
    suspend fun prepareShareFile(context: Context, libraryId: String, libraryName: String, books: List<Book>): Uri =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            val safeName = libraryName.ifBlank { "bibliotheque" }.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val file = File(dir, "$safeName.json")
            val json = LibraryJsonCodec.encode(libraryId, libraryName, books, exportedAt = System.currentTimeMillis())
            file.writeText(json, Charsets.UTF_8)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
}
