package ro.cmo.cryptowatch.data

import kotlinx.coroutines.flow.Flow
import ro.cmo.cryptowatch.network.ApiClient

class CryptoRepository(private val cryptoDao: CryptoDao) {
    val allCryptos: Flow<List<CryptoEntity>> = cryptoDao.getAllCryptos()
    
    suspend fun toggleFavorite(crypto: CryptoEntity) {
        cryptoDao.updateFavoriteStatus(crypto.id, !crypto.isFavorite)
    }

    suspend fun refreshDataFromApi() {
        try {
            val apiCoins = ApiClient.api.getTopCoins()
            val favoriteIds = cryptoDao.getFavoriteIds()
            
            val entities = apiCoins.map { dto ->
                CryptoEntity(
                    id = dto.id,
                    name = dto.name,
                    symbol = dto.symbol.uppercase(),
                    currentPrice = dto.current_price,
                    priceChange24h = dto.price_change_percentage_24h ?: 0.0,
                    isFavorite = favoriteIds.contains(dto.id)
                )
            }
            cryptoDao.insertCryptos(entities)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
