package fr.corentin.biblioscan.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Book::class, LibraryEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun libraryDao(): LibraryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "biblioscan.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(SeedDefaultLibraryCallback)
                    .build()
                    .also { instance = it }
            }

        /**
         * Introduces named libraries: every book so far becomes part of one
         * default library, since v1 only ever had a single, implicit one.
         * `books` has to be rebuilt because its primary key changes from
         * `isbn` alone to the composite `(isbn, libraryId)`.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `libraries` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO `libraries` (`id`, `name`, `createdAt`) VALUES ('$DEFAULT_LIBRARY_ID', 'Ma bibliothèque', ${System.currentTimeMillis()})"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `books_new` (
                        `isbn` TEXT NOT NULL,
                        `libraryId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subtitle` TEXT,
                        `authors` TEXT NOT NULL,
                        `publisher` TEXT,
                        `publishedDate` TEXT,
                        `description` TEXT,
                        `pageCount` INTEGER,
                        `categories` TEXT NOT NULL,
                        `coverUrl` TEXT,
                        `seriesName` TEXT,
                        `seriesIndex` REAL,
                        `dateAdded` INTEGER NOT NULL,
                        `source` TEXT,
                        PRIMARY KEY(`isbn`, `libraryId`),
                        FOREIGN KEY(`libraryId`) REFERENCES `libraries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `books_new` (
                        `isbn`, `libraryId`, `title`, `subtitle`, `authors`, `publisher`, `publishedDate`,
                        `description`, `pageCount`, `categories`, `coverUrl`, `seriesName`, `seriesIndex`,
                        `dateAdded`, `source`
                    )
                    SELECT
                        `isbn`, '$DEFAULT_LIBRARY_ID', `title`, `subtitle`, `authors`, `publisher`, `publishedDate`,
                        `description`, `pageCount`, `categories`, `coverUrl`, `seriesName`, `seriesIndex`,
                        `dateAdded`, `source`
                    FROM `books`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `books`")
                db.execSQL("ALTER TABLE `books_new` RENAME TO `books`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_books_libraryId` ON `books` (`libraryId`)")
            }
        }

        /** Fresh installs skip the migration, so they need the same default library seeded here. */
        private object SeedDefaultLibraryCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL(
                    "INSERT OR IGNORE INTO `libraries` (`id`, `name`, `createdAt`) VALUES ('$DEFAULT_LIBRARY_ID', 'Ma bibliothèque', ${System.currentTimeMillis()})"
                )
            }
        }
    }
}
