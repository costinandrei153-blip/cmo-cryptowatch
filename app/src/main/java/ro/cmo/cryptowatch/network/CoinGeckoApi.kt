package ro.cmo.cryptowatch.network

import retrofit2.http.GET

interface CoinGeckoApi {
    @GET("coins/markets?vs_currency=usd&order=market_cap_desc&per_page=50&page=1&sparkline=false")
    suspend fun getTopCoins(): List<CryptoDto>
}
