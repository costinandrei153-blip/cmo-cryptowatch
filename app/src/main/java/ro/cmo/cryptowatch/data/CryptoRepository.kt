package ro.cmo.cryptowatch.data

import kotlinx.coroutines.flow.Flow

class CryptoRepository(private val cryptoDao: CryptoDao) {
    val allCryptos: Flow<List<CryptoEntity>> = cryptoDao.getAllCryptos()
    
    suspend fun toggleFavorite(crypto: CryptoEntity) {
        cryptoDao.updateFavoriteStatus(crypto.id, !crypto.isFavorite)
    }

    suspend fun cacheCryptos(cryptos: List<CryptoEntity>) {
        cryptoDao.insertCryptos(cryptos)
    }
}
