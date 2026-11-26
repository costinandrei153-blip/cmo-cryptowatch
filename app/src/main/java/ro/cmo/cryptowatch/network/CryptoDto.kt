package ro.cmo.cryptowatch.network

data class CryptoDto(
    val id: String,
    val name: String,
    val symbol: String,
    val current_price: Double,
    val price_change_percentage_24h: Double?,
    val market_cap: Double?,
    val market_cap_rank: Int?,
    val total_volume: Double?,
    val high_24h: Double?,
    val low_24h: Double?,
    val price_change_24h: Double?
)

data class MarketChartDto(
    val prices: List<List<Double>> = emptyList(),
    val market_caps: List<List<Double>> = emptyList(),
    val total_volumes: List<List<Double>> = emptyList()
)

data class PricePoint(
    val timestamp: Long,
    val price: Double
)
