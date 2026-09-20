package fr.corentin.biblioscan.util

/**
 * ISBN helpers: normalization, ISBN-10 -> ISBN-13 conversion, and EAN-13
 * checksum validation (used to reject non-book barcodes read by the scanner).
 */
object IsbnUtils {

    fun normalize(raw: String): String = raw.trim().replace("-", "").replace(" ", "").uppercase()

    /** True if [code] is a numeric EAN-13 with a valid check digit and a book prefix (978/979). */
    fun isValidBookEan13(code: String): Boolean {
        val digits = normalize(code)
        if (digits.length != 13 || !digits.all { it.isDigit() }) return false
        if (!digits.startsWith("978") && !digits.startsWith("979")) return false
        return hasValidEan13Checksum(digits)
    }

    private fun hasValidEan13Checksum(digits: String): Boolean {
        var sum = 0
        for (i in 0..11) {
            val d = digits[i] - '0'
            sum += if (i % 2 == 0) d else d * 3
        }
        val check = (10 - (sum % 10)) % 10
        return check == (digits[12] - '0')
    }

    /** Converts a 10-digit ISBN to its ISBN-13 equivalent. Returns input unchanged if not ISBN-10. */
    fun toIsbn13(raw: String): String {
        val isbn = normalize(raw)
        if (isbn.length != 10) return isbn
        val core = "978" + isbn.substring(0, 9)
        var sum = 0
        for (i in 0..11) {
            val d = core[i] - '0'
            sum += if (i % 2 == 0) d else d * 3
        }
        val check = (10 - (sum % 10)) % 10
        return core + check
    }
}
