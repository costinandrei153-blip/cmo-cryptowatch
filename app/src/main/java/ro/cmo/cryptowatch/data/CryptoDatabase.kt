package ro.cmo.cryptowatch.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CryptoEntity::class,
        PortfolioAccountEntity::class,
        PortfolioHoldingEntity::class,
        PortfolioTransactionEntity::class,
        PriceAlertEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class CryptoDatabase : RoomDatabase() {
    abstract fun cryptoDao(): CryptoDao
    abstract fun portfolioDao(): PortfolioDao

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS portfolio_account (id INTEGER NOT NULL PRIMARY KEY, cashBalance REAL NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS portfolio_holdings (coinId TEXT NOT NULL PRIMARY KEY, coinName TEXT NOT NULL, symbol TEXT NOT NULL, quantity REAL NOT NULL, averageBuyPrice REAL NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS portfolio_transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, coinId TEXT NOT NULL, coinName TEXT NOT NULL, symbol TEXT NOT NULL, type TEXT NOT NULL, quantity REAL NOT NULL, price REAL NOT NULL, total REAL NOT NULL, timestamp INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS price_alerts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, coinId TEXT NOT NULL, coinName TEXT NOT NULL, symbol TEXT NOT NULL, targetPrice REAL NOT NULL, isAbove INTEGER NOT NULL, isActive INTEGER NOT NULL, isTriggered INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
                database.execSQL("INSERT OR IGNORE INTO portfolio_account (id, cashBalance) VALUES (1, 10000.0)")
            }
        }

        fun getDatabase(context: Context): CryptoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CryptoDatabase::class.java,
                    "crypto_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
