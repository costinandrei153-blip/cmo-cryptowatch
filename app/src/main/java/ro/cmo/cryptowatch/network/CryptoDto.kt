package ro.cmo.cryptowatch.network

data class CryptoDto(
    val id: String,
    val name: String,
    val symbol: String,
    val current_price: Double,
    val price_change_percentage_24h: Double?
)
