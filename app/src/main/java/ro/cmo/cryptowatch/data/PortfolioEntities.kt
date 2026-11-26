package ro.cmo.cryptowatch.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "portfolio_account")
data class PortfolioAccountEntity(
    @PrimaryKey val id: Int = 1,
    val cashBalance: Double = 10_000.0
)

@Entity(tableName = "portfolio_holdings")
data class PortfolioHoldingEntity(
    @PrimaryKey val coinId: String,
    val coinName: String,
    val symbol: String,
    val quantity: Double,
    val averageBuyPrice: Double
)

@Entity(tableName = "portfolio_transactions")
data class PortfolioTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val coinId: String,
    val coinName: String,
    val symbol: String,
    val type: String,
    val quantity: Double,
    val price: Double,
    val total: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "price_alerts")
data class PriceAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val coinId: String,
    val coinName: String,
    val symbol: String,
    val targetPrice: Double,
    val isAbove: Boolean,
    val isActive: Boolean = true,
    val isTriggered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
