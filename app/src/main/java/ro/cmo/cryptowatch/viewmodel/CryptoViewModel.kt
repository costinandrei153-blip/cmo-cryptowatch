package ro.cmo.cryptowatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ro.cmo.cryptowatch.data.CryptoEntity
import ro.cmo.cryptowatch.data.CryptoRepository
import java.io.IOException

class CryptoViewModel(private val repository: CryptoRepository) : ViewModel() {
    val cryptoList: StateFlow<List<CryptoEntity>> = repository.allCryptos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteList: StateFlow<List<CryptoEntity>> = repository.favoriteCryptos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _lastUpdated = MutableStateFlow<Long?>(null)
    val lastUpdated: StateFlow<Long?> = _lastUpdated.asStateFlow()

    init { refreshData() }

    fun refreshData() {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                repository.refreshDataFromApi()
                _lastUpdated.value = System.currentTimeMillis()
            } catch (exception: Exception) {
                _errorMessage.value = when (exception) {
                    is IOException -> "Nu am putut contacta CoinGecko. Verifică internetul și încearcă din nou."
                    else -> exception.message?.takeIf { it.isNotBlank() }
                        ?: "A apărut o eroare la actualizarea datelor."
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onFavoriteClick(crypto: CryptoEntity) {
        viewModelScope.launch { repository.toggleFavorite(crypto) }
    }

    fun clearError() { _errorMessage.value = null }
}
