package ro.cmo.cryptowatch.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CryptoEntity::class], version = 2, exportSchema = false)
abstract class CryptoDatabase : RoomDatabase() {
    abstract fun cryptoDao(): CryptoDao

    companion object {
        @Volatile
        private var INSTANCE: CryptoDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE crypto_table ADD COLUMN marketCap REAL")
                database.execSQL("ALTER TABLE crypto_table ADD COLUMN marketCapRank INTEGER")
                database.execSQL("ALTER TABLE crypto_table ADD COLUMN totalVolume REAL")
                database.execSQL("ALTER TABLE crypto_table ADD COLUMN high24h REAL")
                database.execSQL("ALTER TABLE crypto_table ADD COLUMN low24h REAL")
                database.execSQL("ALTER TABLE crypto_table ADD COLUMN priceChange24hAmount REAL")
            }
        }

        fun getDatabase(context: Context): CryptoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CryptoDatabase::class.java,
                    "crypto_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
