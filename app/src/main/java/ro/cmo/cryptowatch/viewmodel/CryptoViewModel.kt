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
        fetchDataFromApi()
    }

    private fun fetchDataFromApi() {
        viewModelScope.launch {
            repository.refreshDataFromApi()
        }
    }

    fun onFavoriteClick(crypto: CryptoEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(crypto)
        }
    }
}
