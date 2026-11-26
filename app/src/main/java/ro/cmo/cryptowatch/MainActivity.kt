package ro.cmo.cryptowatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ro.cmo.cryptowatch.data.CryptoDatabase
import ro.cmo.cryptowatch.data.CryptoEntity
import ro.cmo.cryptowatch.data.CryptoRepository
import ro.cmo.cryptowatch.viewmodel.CryptoViewModel
import java.util.Locale

private val AppBackground = Color(0xFF0B1020)
private val CardBackground = Color(0xFF151D31)
private val Accent = Color(0xFF86A2FF)
private val Muted = Color(0xFF9AA7BD)
private val Green = Color(0xFF31C48D)
private val Red = Color(0xFFFF6B75)

fun usd(value: Double): String = if (value >= 1.0) {
    "$" + String.format(Locale.US, "%,.2f", value)
} else {
    "$" + String.format(Locale.US, "%.8f", value).trimEnd('0').trimEnd('.')
}
fun pct(value: Double): String = String.format(Locale.US, "%+.2f%%", value)
fun changeColor(value: Double): Color = if (value >= 0) Green else Red

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = CryptoDatabase.getDatabase(this)
        val repository = CryptoRepository(database.cryptoDao())
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = CryptoViewModel(repository) as T
        }
        setContent {
            val cryptoViewModel: CryptoViewModel = viewModel(factory = factory)
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = AppBackground, contentColor = Color.White) {
                    CryptoWatchApp(cryptoViewModel)
                }
            }
        }
    }
}

@Composable
fun CryptoWatchApp(viewModel: CryptoViewModel) {
    val nav = rememberNavController()
    val coins by viewModel.cryptoList.collectAsState()
    val favorites by viewModel.favoriteList.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(coins, favorites, loading, error, viewModel::refreshData,
                viewModel::clearError, viewModel::onFavoriteClick, { nav.navigate("detail/$it") })
        }
        composable("detail/{coinId}", arguments = listOf(navArgument("coinId") { type = NavType.StringType })) { entry ->
            val coin = coins.firstOrNull { it.id == entry.arguments?.getString("coinId") }
            if (coin != null) {
                DetailScreen(coin, { nav.popBackStack() }, { viewModel.onFavoriteClick(coin) })
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Moneda nu este disponibilă în cache.", color = Muted)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    coins: List<CryptoEntity>,
    favorites: List<CryptoEntity>,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    onFavorite: (CryptoEntity) -> Unit,
    onOpen: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    val visibleCoins = (if (favoritesOnly) favorites else coins).filter {
        it.name.contains(query, true) || it.symbol.contains(query, true)
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("CryptoWatch", fontWeight = FontWeight.Bold)
                        Text("CRYPTO MARKET", color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !loading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizează", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground, titleContentColor = Color.White)
            )
        },
        bottomBar = {
            Surface(color = CardBackground) {
                Row(Modifier.fillMaxWidth().padding(8.dp)) {
                    TextButton(onClick = { favoritesOnly = false }, modifier = Modifier.weight(1f)) {
                        Text("Piață", color = if (!favoritesOnly) Accent else Muted)
                    }
                    TextButton(onClick = { favoritesOnly = true }, modifier = Modifier.weight(1f)) {
                        Text("Favorite (" + favorites.size + ")", color = if (favoritesOnly) Accent else Muted)
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Piața crypto", color = Muted)
                    Text(coins.size.toString() + " active assets", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Prețuri în USD • Date CoinGecko", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(14.dp))
            Surface(color = CardBackground, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Muted)
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        decorationBox = { inner ->
                            if (query.isEmpty()) Text("Caută Bitcoin, BTC, Solana...", color = Muted)
                            inner()
                        }
                    )
                }
            }
            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF482630))) {
                    Column(Modifier.padding(12.dp)) {
                        Text(error, color = Color(0xFFFFD5D8))
                        TextButton(onClick = onDismissError) { Text("Închide", color = Color.White) }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (favoritesOnly) "FAVORITE" else "TOP CRIPTOMONEDE", color = Muted, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = Accent, strokeWidth = 2.dp)
            }
            Spacer(Modifier.height(8.dp))
            when {
                visibleCoins.isEmpty() && loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
                visibleCoins.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (favoritesOnly) "Nu ai adăugat încă monede la favorite." else "Nu există rezultate.", color = Muted)
                }
                else -> LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(visibleCoins, key = { it.id }) { coin ->
                        CryptoCard(coin, { onOpen(coin.id) }, { onFavorite(coin) })
                    }
                }
            }
        }
    }
}

@Composable
fun CryptoCard(coin: CryptoEntity, onClick: () -> Unit, onFavorite: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(15.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onFavorite) {
                Icon(
                    if (coin.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "Favorite",
                    tint = if (coin.isFavorite) Color(0xFFFFC857) else Muted
                )
            }
            Column(Modifier.weight(1f)) {
                Text(coin.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(coin.symbol, color = Muted, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(usd(coin.currentPrice), color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(pct(coin.priceChange24h), color = changeColor(coin.priceChange24h), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(coin: CryptoEntity, onBack: () -> Unit, onFavorite: () -> Unit) {
    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text(coin.name, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Înapoi", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onFavorite) {
                        Icon(
                            if (coin.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = "Favorite",
                            tint = if (coin.isFavorite) Color(0xFFFFC857) else Muted
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(coin.symbol, color = Accent, fontWeight = FontWeight.Bold)
            Text(usd(coin.currentPrice), color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(pct(coin.priceChange24h) + " în ultimele 24h", color = changeColor(coin.priceChange24h), style = MaterialTheme.typography.titleMedium)
            InfoCard("Nume", coin.name)
            InfoCard("Simbol", coin.symbol)
            InfoCard("Preț curent", usd(coin.currentPrice))
            InfoCard("Variație 24h", pct(coin.priceChange24h))
            Text(if (coin.isFavorite) "Adăugată la favorite" else "Nu este în lista de favorite", color = Muted)
        }
    }
}

@Composable
fun InfoCard(label: String, value: String) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Muted)
            Text(value, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}
