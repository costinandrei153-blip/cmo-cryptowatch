package ro.cmo.cryptowatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ro.cmo.cryptowatch.data.CryptoDatabase
import ro.cmo.cryptowatch.data.CryptoEntity
import ro.cmo.cryptowatch.data.CryptoRepository
import ro.cmo.cryptowatch.viewmodel.CryptoViewModel

fun usd(v: Double) = "$" + String.format("%,.2f", v)
fun pct(v: Double) = String.format("%+.2f%%", v)
fun changeColor(v: Double) = if (v >= 0) Color(0xFF1B7F4B) else Color(0xFFB3261E)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = CryptoDatabase.getDatabase(this)
        val repository = CryptoRepository(database.cryptoDao())

        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CryptoViewModel(repository) as T
            }
        }

        setContent {
            val viewModel: CryptoViewModel = viewModel(factory = factory)
            
            MaterialTheme { 
                App(viewModel) 
            }
        }
    }
}

@Composable
fun App(viewModel: CryptoViewModel) {
    val nav = rememberNavController()
    val cryptos by viewModel.cryptoList.collectAsState()

    NavHost(nav, startDestination = "list") {
        composable("list") { 
            ListScreen(
                cryptos = cryptos,
                onOpen = { id -> nav.navigate("detail/$id") },
                onFavoriteToggle = { coin -> viewModel.onFavoriteClick(coin) }
            ) 
        }
        composable("detail/{id}") { entry ->
            val coinId = entry.arguments?.getString("id")
            val coin = cryptos.find { it.id == coinId }
            
            if (coin != null) {
                DetailScreen(
                    coin = coin, 
                    onBack = { nav.popBackStack() },
                    onFavoriteToggle = { viewModel.onFavoriteClick(coin) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    cryptos: List<CryptoEntity>, 
    onOpen: (String) -> Unit, 
    onFavoriteToggle: (CryptoEntity) -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("CryptoWatch") }) }) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            items(cryptos, key = { it.id }) { coin ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(coin.id) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onFavoriteToggle(coin) }) {
                        Icon(
                            imageVector = if (coin.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = "Favorite",
                            tint = if (coin.isFavorite) Color(0xFFFFC107) else Color.Gray
                        )
                    }
                    
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(coin.name, style = MaterialTheme.typography.titleMedium)
                        Text(coin.symbol, style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(usd(coin.currentPrice))
                        Text(pct(coin.priceChange24h), color = changeColor(coin.priceChange24h))
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    coin: CryptoEntity, 
    onBack: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(coin.name) },
            navigationIcon = { TextButton(onClick = onBack) { Text("Înapoi") } },
            actions = {
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (coin.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "Favorite",
                        tint = if (coin.isFavorite) Color(0xFFFFC107) else Color.Gray
                    )
                }
            }
        )
    }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(usd(coin.currentPrice), style = MaterialTheme.typography.displaySmall)
            Text("Variație 24h: " + pct(coin.priceChange24h), color = changeColor(coin.priceChange24h))
            Text("Simbol: ${coin.symbol}")
            
            if (coin.isFavorite) {
                Text("⭐ Salvat la Favorite", color = Color(0xFFE6A800), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
