package ro.cmo.cryptowatch.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CoinGeckoApi {
    @GET("coins/markets?vs_currency=usd&order=market_cap_desc&per_page=50&page=1&sparkline=false")
    suspend fun getTopCoins(): List<CryptoDto>

    @GET("coins/{id}/market_chart")
    suspend fun getMarketChart(
        @Path("id") coinId: String,
        @Query("vs_currency") currency: String = "usd",
        @Query("days") days: Int
    ): MarketChartDto
}
