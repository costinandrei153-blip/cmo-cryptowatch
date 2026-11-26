package ro.cmo.cryptowatch.data

import kotlinx.coroutines.flow.Flow
import ro.cmo.cryptowatch.network.ApiClient
import ro.cmo.cryptowatch.network.PricePoint

class CryptoRepository(private val cryptoDao: CryptoDao) {
    val allCryptos: Flow<List<CryptoEntity>> = cryptoDao.getAllCryptos()
    val favoriteCryptos: Flow<List<CryptoEntity>> = cryptoDao.getFavorites()

    suspend fun toggleFavorite(crypto: CryptoEntity) {
        cryptoDao.updateFavoriteStatus(crypto.id, !crypto.isFavorite)
    }

    suspend fun refreshDataFromApi() {
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
    }

    suspend fun getPriceHistory(coinId: String, days: Int): List<PricePoint> {
        val response = ApiClient.api.getMarketChart(coinId = coinId, days = days)
        return response.prices.mapNotNull { point ->
            if (point.size >= 2) PricePoint(point[0].toLong(), point[1]) else null
        }
    }
}
