package fr.corentin.biblioscan.data

import androidx.room.TypeConverter

private const val LIST_SEPARATOR = "|||"

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(LIST_SEPARATOR)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(LIST_SEPARATOR)
}
