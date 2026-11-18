package ro.cmo.cryptowatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ro.cmo.cryptowatch.data.CryptoEntity
import ro.cmo.cryptowatch.data.CryptoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CryptoViewModel(private val repository: CryptoRepository) : ViewModel() {

    val cryptoList: StateFlow<List<CryptoEntity>> = repository.allCryptos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        insertMockData()
    }

    private fun insertMockData() {
        viewModelScope.launch {
            val mockData = listOf(
                CryptoEntity("bitcoin", "Bitcoin", "BTC", 64000.0, 1.5),
                CryptoEntity("ethereum", "Ethereum", "ETH", 3450.0, -0.8),
                CryptoEntity("solana", "Solana", "SOL", 145.0, 5.2, true),
                CryptoEntity("cardano", "Cardano", "ADA", 0.45, 0.1)
            )
            repository.cacheCryptos(mockData)
        }
    }

    fun onFavoriteClick(crypto: CryptoEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(crypto)
        }
    }
}
