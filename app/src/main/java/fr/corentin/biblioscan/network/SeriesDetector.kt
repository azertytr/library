package fr.corentin.biblioscan.network

/**
 * Best-effort extraction of "series name" + "index within series" from
 * whatever metadata an API returned. There is no universally reliable
 * series field across providers, so this tries, in order:
 *  1. An explicit series string from Open Library's edition record
 *     (e.g. "Harry Potter -- 1" or "Harry Potter ; 1").
 *  2. Common "Title, Tome N" / "Title, Book N" / "Title #N" patterns
 *     found in the title or subtitle.
 *  3. Parenthetical "(Series Name, #N)" patterns, common in Google Books
 *     titles imported from Goodreads-style data.
 */
data class SeriesInfo(val name: String, val index: Double)

object SeriesDetector {

    private val explicitSeriesRegex = Regex("""^(.+?)\s*[-–;:]+\s*(\d+(?:[.,]\d+)?)$""")

    private val inlineRegexes = listOf(
        // "Harry Potter, Tome 1" / "Harry Potter, T.1"
        Regex("""^(.*?),\s*(?:tome|t\.)\s*n?°?\s*(\d+(?:[.,]\d+)?)""", RegexOption.IGNORE_CASE),
        // "The Hobbit, Book 1" / "..., Vol. 2" / "..., Volume 3"
        Regex("""^(.*?),\s*(?:book|vol\.?|volume)\s*(\d+(?:[.,]\d+)?)""", RegexOption.IGNORE_CASE),
        // "Harry Potter #1"
        Regex("""^(.*?)\s*#\s*(\d+(?:[.,]\d+)?)""")
    )

    // "(Harry Potter, #1)" or "(Harry Potter Book 1)" anywhere in the text
    private val parentheticalRegex = Regex(
        """\(([^()#]{2,60}?)[,]?\s*(?:#|book|tome|vol\.?|volume)\s*(\d+(?:[.,]\d+)?)\)""",
        RegexOption.IGNORE_CASE
    )

    fun detect(title: String?, subtitle: String?, openLibrarySeriesRaw: String? = null): SeriesInfo? {
        openLibrarySeriesRaw?.let { raw ->
            explicitSeriesRegex.find(raw.trim())?.let { m ->
                val name = m.groupValues[1].trim()
                val index = m.groupValues[2].replace(",", ".").toDoubleOrNull()
                if (name.isNotBlank() && index != null) return SeriesInfo(name, index)
            }
        }

        val candidates = listOfNotNull(title, subtitle)
        for (text in candidates) {
            for (regex in inlineRegexes) {
                regex.find(text)?.let { m ->
                    val name = m.groupValues[1].trim().trimEnd(',', ':')
                    val index = m.groupValues[2].replace(",", ".").toDoubleOrNull()
                    if (name.isNotBlank() && index != null) return SeriesInfo(name, index)
                }
            }
        }

        val combined = listOfNotNull(title, subtitle).joinToString(" ")
        parentheticalRegex.find(combined)?.let { m ->
            val name = m.groupValues[1].trim()
            val index = m.groupValues[2].replace(",", ".").toDoubleOrNull()
            if (name.isNotBlank() && index != null) return SeriesInfo(name, index)
        }

        return null
    }
}
