package ro.cmo.cryptowatch.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import ro.cmo.cryptowatch.network.ApiClient
import ro.cmo.cryptowatch.network.PricePoint

class CryptoRepository(
    private val context: Context,
    private val cryptoDao: CryptoDao,
    private val portfolioDao: PortfolioDao
) {
    val allCryptos: Flow<List<CryptoEntity>> = cryptoDao.getAllCryptos()
    val favoriteCryptos: Flow<List<CryptoEntity>> = cryptoDao.getFavorites()
    val portfolioAccount: Flow<PortfolioAccountEntity?> = portfolioDao.observeAccount()
    val portfolioHoldings: Flow<List<PortfolioHoldingEntity>> = portfolioDao.observeHoldings()
    val portfolioTransactions: Flow<List<PortfolioTransactionEntity>> = portfolioDao.observeTransactions()
    val priceAlerts: Flow<List<PriceAlertEntity>> = portfolioDao.observeAlerts()

    suspend fun initializePortfolio() {
        portfolioDao.createAccount(PortfolioAccountEntity())
    }

    suspend fun toggleFavorite(crypto: CryptoEntity) {
        cryptoDao.updateFavoriteStatus(crypto.id, !crypto.isFavorite)
    }

    suspend fun refreshDataFromApi(): List<PriceAlertEntity> {
        val apiCoins = ApiClient.api.getTopCoins()
        if (apiCoins.isEmpty()) {
            throw IllegalStateException("Serviciul de date nu a returnat nicio criptomonedă.")
        }

        val favoriteIds = cryptoDao.getFavoriteIds()
        val entities = apiCoins.map { dto ->
            CryptoEntity(
                id = dto.id,
                name = dto.name,
                symbol = dto.symbol.uppercase(),
                currentPrice = dto.current_price,
                priceChange24h = dto.price_change_percentage_24h ?: 0.0,
                isFavorite = dto.id in favoriteIds,
                marketCap = dto.market_cap,
                marketCapRank = dto.market_cap_rank,
                totalVolume = dto.total_volume,
                high24h = dto.high_24h,
                low24h = dto.low_24h,
                priceChange24hAmount = dto.price_change_24h
            )
        }
        cryptoDao.insertCryptos(entities)

        val pricesById = entities.associate { it.id to it.currentPrice }
        val triggered = portfolioDao.getPendingAlerts().mapNotNull { alert ->
            val currentPrice = pricesById[alert.coinId] ?: return@mapNotNull null
            val reached = if (alert.isAbove) currentPrice >= alert.targetPrice else currentPrice <= alert.targetPrice
            if (reached) alert to currentPrice else null
        }
        triggered.forEach { (alert, currentPrice) ->
            portfolioDao.markAlertTriggered(alert.id)
            PriceAlertNotifier.show(context, alert, currentPrice)
        }
        return triggered.map { it.first }
    }

    suspend fun getPriceHistory(coinId: String, days: Int): List<PricePoint> {
        val response = ApiClient.api.getMarketChart(coinId = coinId, days = days)
        return response.prices.mapNotNull { point ->
            if (point.size >= 2) PricePoint(point[0].toLong(), point[1]) else null
        }
    }

    suspend fun buyCrypto(coin: CryptoEntity, spend: Double) {
        require(spend.isFinite() && spend > 0.0) { "Introdu o sumă validă, mai mare decât zero." }
        val account = portfolioDao.getAccount() ?: PortfolioAccountEntity().also {
            portfolioDao.createAccount(it)
        }
        require(spend <= account.cashBalance) { "Nu ai suficienți USD în soldul virtual." }

        val quantity = spend / coin.currentPrice
        val oldHolding = portfolioDao.getHolding(coin.id)
        val oldQuantity = oldHolding?.quantity ?: 0.0
        val oldCost = oldQuantity * (oldHolding?.averageBuyPrice ?: 0.0)
        val newQuantity = oldQuantity + quantity
        val newAverage = (oldCost + spend) / newQuantity

        portfolioDao.buy(
            coin.id, coin.name, coin.symbol, spend, quantity, coin.currentPrice,
            account.cashBalance - spend, newQuantity, newAverage
        )
    }

    suspend fun sellCrypto(coin: CryptoEntity, quantity: Double) {
        require(quantity.isFinite() && quantity > 0.0) { "Introdu o cantitate validă, mai mare decât zero." }
        val account = portfolioDao.getAccount() ?: PortfolioAccountEntity().also {
            portfolioDao.createAccount(it)
        }
        val holding = portfolioDao.getHolding(coin.id)
            ?: throw IllegalArgumentException("Nu deții această criptomonedă în portofoliu.")
        require(quantity <= holding.quantity + 1e-12) { "Nu poți vinde mai mult decât deții." }

        val proceeds = quantity * coin.currentPrice
        val remaining = (holding.quantity - quantity).coerceAtLeast(0.0)
        portfolioDao.sell(
            coin.id, coin.name, coin.symbol, proceeds, quantity, coin.currentPrice,
            account.cashBalance + proceeds, remaining, holding.averageBuyPrice
        )
    }

    suspend fun createPriceAlert(coin: CryptoEntity, targetPrice: Double, isAbove: Boolean) {
        require(targetPrice.isFinite() && targetPrice > 0.0) { "Introdu un preț valid, mai mare decât zero." }
        portfolioDao.addAlert(
            PriceAlertEntity(
                coinId = coin.id,
                coinName = coin.name,
                symbol = coin.symbol,
                targetPrice = targetPrice,
                isAbove = isAbove
            )
        )
    }

    suspend fun deletePriceAlert(alertId: Long) {
        portfolioDao.deleteAlert(alertId)
    }
}
