package ro.cmo.cryptowatch.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crypto_table")
data class CryptoEntity(
    @PrimaryKey val id: String,
    val name: String,
    val symbol: String,
    val currentPrice: Double,
    val priceChange24h: Double,
    val isFavorite: Boolean = false,
    val marketCap: Double? = null,
    val marketCapRank: Int? = null,
    val totalVolume: Double? = null,
    val high24h: Double? = null,
    val low24h: Double? = null,
    val priceChange24hAmount: Double? = null
)
