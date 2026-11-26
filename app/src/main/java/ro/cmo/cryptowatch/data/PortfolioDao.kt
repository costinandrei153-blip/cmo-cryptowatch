package ro.cmo.cryptowatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {
    @Query("SELECT * FROM portfolio_account WHERE id = 1")
    fun observeAccount(): Flow<PortfolioAccountEntity?>

    @Query("SELECT * FROM portfolio_account WHERE id = 1")
    suspend fun getAccount(): PortfolioAccountEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun createAccount(account: PortfolioAccountEntity)

    @Query("UPDATE portfolio_account SET cashBalance = :balance WHERE id = 1")
    suspend fun updateCashBalance(balance: Double)

    @Query("SELECT * FROM portfolio_holdings ORDER BY coinName")
    fun observeHoldings(): Flow<List<PortfolioHoldingEntity>>

    @Query("SELECT * FROM portfolio_holdings WHERE coinId = :coinId")
    suspend fun getHolding(coinId: String): PortfolioHoldingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveHolding(holding: PortfolioHoldingEntity)

    @Query("DELETE FROM portfolio_holdings WHERE coinId = :coinId")
    suspend fun deleteHolding(coinId: String)

    @Insert
    suspend fun addTransaction(transaction: PortfolioTransactionEntity)

    @Query("SELECT * FROM portfolio_transactions ORDER BY timestamp DESC LIMIT 50")
    fun observeTransactions(): Flow<List<PortfolioTransactionEntity>>

    @Insert
    suspend fun addAlert(alert: PriceAlertEntity)

    @Query("SELECT * FROM price_alerts ORDER BY createdAt DESC")
    fun observeAlerts(): Flow<List<PriceAlertEntity>>

    @Query("SELECT * FROM price_alerts WHERE isActive = 1 AND isTriggered = 0")
    suspend fun getPendingAlerts(): List<PriceAlertEntity>

    @Query("UPDATE price_alerts SET isTriggered = 1, isActive = 0 WHERE id = :alertId")
    suspend fun markAlertTriggered(alertId: Long)

    @Query("DELETE FROM price_alerts WHERE id = :alertId")
    suspend fun deleteAlert(alertId: Long)

    @Transaction
    suspend fun buy(
        coinId: String, coinName: String, symbol: String,
        spend: Double, quantity: Double, price: Double,
        newCashBalance: Double, newHoldingQuantity: Double, newAveragePrice: Double
    ) {
        updateCashBalance(newCashBalance)
        saveHolding(PortfolioHoldingEntity(coinId, coinName, symbol, newHoldingQuantity, newAveragePrice))
        addTransaction(PortfolioTransactionEntity(
            coinId = coinId, coinName = coinName, symbol = symbol,
            type = "BUY", quantity = quantity, price = price, total = spend
        ))
    }

    @Transaction
    suspend fun sell(
        coinId: String, coinName: String, symbol: String,
        proceeds: Double, quantity: Double, price: Double,
        newCashBalance: Double, remainingQuantity: Double, averagePrice: Double
    ) {
        updateCashBalance(newCashBalance)
        if (remainingQuantity <= 1e-12) deleteHolding(coinId)
        else saveHolding(PortfolioHoldingEntity(coinId, coinName, symbol, remainingQuantity, averagePrice))
        addTransaction(PortfolioTransactionEntity(
            coinId = coinId, coinName = coinName, symbol = symbol,
            type = "SELL", quantity = quantity, price = price, total = proceeds
        ))
    }
}
