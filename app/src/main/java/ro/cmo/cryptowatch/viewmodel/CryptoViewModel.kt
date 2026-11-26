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
import ro.cmo.cryptowatch.data.PortfolioAccountEntity
import ro.cmo.cryptowatch.data.PortfolioHoldingEntity
import ro.cmo.cryptowatch.data.PortfolioTransactionEntity
import ro.cmo.cryptowatch.data.PriceAlertEntity
import ro.cmo.cryptowatch.network.PricePoint
import java.io.IOException
import java.util.Locale

class CryptoViewModel(private val repository: CryptoRepository) : ViewModel() {
    val cryptoList: StateFlow<List<CryptoEntity>> = repository.allCryptos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favoriteList: StateFlow<List<CryptoEntity>> = repository.favoriteCryptos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val portfolioAccount: StateFlow<PortfolioAccountEntity?> = repository.portfolioAccount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val portfolioHoldings: StateFlow<List<PortfolioHoldingEntity>> = repository.portfolioHoldings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val portfolioTransactions: StateFlow<List<PortfolioTransactionEntity>> = repository.portfolioTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val priceAlerts: StateFlow<List<PriceAlertEntity>> = repository.priceAlerts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    private val _lastUpdated = MutableStateFlow<Long?>(null)
    val lastUpdated: StateFlow<Long?> = _lastUpdated.asStateFlow()
    private val _chartPoints = MutableStateFlow<List<PricePoint>>(emptyList())
    val chartPoints: StateFlow<List<PricePoint>> = _chartPoints.asStateFlow()
    private val _chartLoading = MutableStateFlow(false)
    val chartLoading: StateFlow<Boolean> = _chartLoading.asStateFlow()
    private val _chartError = MutableStateFlow<String?>(null)
    val chartError: StateFlow<String?> = _chartError.asStateFlow()
    private val _selectedChartDays = MutableStateFlow(1)
    val selectedChartDays: StateFlow<Int> = _selectedChartDays.asStateFlow()
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        viewModelScope.launch { repository.initializePortfolio() }
        refreshData()
    }

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
                    else -> exception.message?.takeIf { it.isNotBlank() } ?: "A apărut o eroare la actualizarea datelor."
                }
            } finally { _isLoading.value = false }
        }
    }

    fun loadPriceHistory(coinId: String, days: Int = _selectedChartDays.value) {
        _selectedChartDays.value = days
        viewModelScope.launch {
            _chartLoading.value = true
            _chartError.value = null
            try {
                _chartPoints.value = repository.getPriceHistory(coinId, days)
                if (_chartPoints.value.isEmpty()) _chartError.value = "Nu există date istorice disponibile pentru această perioadă."
            } catch (exception: Exception) {
                _chartError.value = if (exception is IOException) "Nu am putut încărca graficul. Verifică internetul."
                else exception.message?.takeIf { it.isNotBlank() } ?: "Graficul nu este disponibil momentan."
                _chartPoints.value = emptyList()
            } finally { _chartLoading.value = false }
        }
    }

    fun buy(coin: CryptoEntity, spend: Double) = runAction {
        repository.buyCrypto(coin, spend)
        "Ai cumpărat ${coin.symbol} în valoare de $" + String.format(Locale.US, "%.2f", spend) + "."
    }
    fun sell(coin: CryptoEntity, quantity: Double) = runAction {
        repository.sellCrypto(coin, quantity)
        "Vânzare ${coin.symbol} efectuată."
    }
    fun addAlert(coin: CryptoEntity, target: Double, above: Boolean) = runAction {
        repository.createPriceAlert(coin, target, above)
        "Alerta pentru ${coin.symbol} a fost salvată."
    }
    fun deleteAlert(id: Long) = runAction {
        repository.deletePriceAlert(id)
        "Alerta a fost ștearsă."
    }

    private fun runAction(action: suspend () -> String) {
        viewModelScope.launch {
            _actionMessage.value = null
            try { _actionMessage.value = action() }
            catch (e: Exception) { _actionMessage.value = e.message ?: "Operațiunea nu a putut fi efectuată." }
        }
    }

    fun clearActionMessage() { _actionMessage.value = null }
    fun onFavoriteClick(crypto: CryptoEntity) { viewModelScope.launch { repository.toggleFavorite(crypto) } }
    fun clearError() { _errorMessage.value = null }
}
