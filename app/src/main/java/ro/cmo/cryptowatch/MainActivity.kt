package ro.cmo.cryptowatch

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import ro.cmo.cryptowatch.network.PricePoint
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
fun compactUsd(value: Double?): String = value?.let {
    when {
        it >= 1_000_000_000_000 -> "$" + String.format(Locale.US, "%.2fT", it / 1_000_000_000_000)
        it >= 1_000_000_000 -> "$" + String.format(Locale.US, "%.2fB", it / 1_000_000_000)
        it >= 1_000_000 -> "$" + String.format(Locale.US, "%.2fM", it / 1_000_000)
        it >= 1_000 -> "$" + String.format(Locale.US, "%.2fK", it / 1_000)
        else -> usd(it)
    }
} ?: "N/A"
fun pct(value: Double): String = String.format(Locale.US, "%+.2f%%", value)
fun changeColor(value: Double): Color = if (value >= 0) Green else Red

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
        val database = CryptoDatabase.getDatabase(this)
        val repository = CryptoRepository(applicationContext, database.cryptoDao(), database.portfolioDao())
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
    val chartPoints by viewModel.chartPoints.collectAsState()
    val chartLoading by viewModel.chartLoading.collectAsState()
    val chartError by viewModel.chartError.collectAsState()
    val selectedDays by viewModel.selectedChartDays.collectAsState()
    val account by viewModel.portfolioAccount.collectAsState()
    val holdings by viewModel.portfolioHoldings.collectAsState()
    val transactions by viewModel.portfolioTransactions.collectAsState()
    val alerts by viewModel.priceAlerts.collectAsState()

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(coins, favorites, loading, error, viewModel::refreshData,
                viewModel::clearError, viewModel::onFavoriteClick, { nav.navigate("detail/$it") },
                { nav.navigate("portfolio") }, { nav.navigate("alerts") })
        }
        composable("detail/{coinId}", arguments = listOf(navArgument("coinId") { type = NavType.StringType })) { entry ->
            val coin = coins.firstOrNull { it.id == entry.arguments?.getString("coinId") }
            if (coin != null) {
                LaunchedEffect(coin.id) { viewModel.loadPriceHistory(coin.id, 1) }
                DetailScreen(
                    coin = coin,
                    points = chartPoints,
                    chartLoading = chartLoading,
                    chartError = chartError,
                    selectedDays = selectedDays,
                    onPeriodSelected = { days -> viewModel.loadPriceHistory(coin.id, days) },
                    onBack = { nav.popBackStack() },
                    onFavorite = { viewModel.onFavoriteClick(coin) },
                    onBuy = { viewModel.buy(coin, it) },
                    onSell = { viewModel.sell(coin, it) },
                    onCreateAlert = { target, above -> viewModel.addAlert(coin, target, above) },
                    holding = holdings.firstOrNull { it.coinId == coin.id }
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Moneda nu este disponibilă în cache.", color = Muted)
                }
            }
        }
        composable("portfolio") {
            PortfolioScreen(account, holdings, transactions, coins, { nav.popBackStack() }) {
                nav.navigate("detail/$it")
            }
        }
        composable("alerts") {
            AlertsScreen(alerts, { nav.popBackStack() }, viewModel::deleteAlert)
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
    onOpen: (String) -> Unit,
    onPortfolio: () -> Unit,
    onAlerts: () -> Unit
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
                    TextButton(onClick = onPortfolio, modifier = Modifier.weight(1f)) { Text("Portofoliu", color = Muted) }
                    TextButton(onClick = onAlerts, modifier = Modifier.weight(1f)) { Text("Alerte", color = Muted) }
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
fun DetailScreen(
    coin: CryptoEntity,
    points: List<PricePoint>,
    chartLoading: Boolean,
    chartError: String?,
    selectedDays: Int,
    onPeriodSelected: (Int) -> Unit,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
    onBuy: (Double) -> Unit,
    onSell: (Double) -> Unit,
    onCreateAlert: (Double, Boolean) -> Unit,
    holding: ro.cmo.cryptowatch.data.PortfolioHoldingEntity?
) {
    var tradeDialog by remember { mutableStateOf(false) }
    var alertDialog by remember { mutableStateOf(false) }
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
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Text(coin.symbol.uppercase(), color = Accent, fontWeight = FontWeight.Bold)
            Text(usd(coin.currentPrice), color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pct(coin.priceChange24h), color = changeColor(coin.priceChange24h), fontWeight = FontWeight.SemiBold)
                Text("  în ultimele 24h", color = Muted, style = MaterialTheme.typography.bodySmall)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Evoluția prețului", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PeriodButton("24H", 1, selectedDays, onPeriodSelected, Modifier.weight(1f))
                        PeriodButton("7D", 7, selectedDays, onPeriodSelected, Modifier.weight(1f))
                        PeriodButton("30D", 30, selectedDays, onPeriodSelected, Modifier.weight(1f))
                    }
                    when {
                        chartLoading -> Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Accent)
                        }
                        points.size >= 2 -> PriceChart(points)
                        chartError != null -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            Text(chartError, color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        else -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            Text("Se încarcă datele graficului...", color = Muted)
                        }
                    }
                    if (points.size >= 2) {
                        val first = points.first().price
                        val last = points.last().price
                        val delta = if (first != 0.0) ((last - first) / first) * 100.0 else 0.0
                        Text(
                            "Variație în perioadă: " + pct(delta),
                            color = changeColor(delta),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Text("STATISTICI DE PIAȚĂ", color = Muted, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Market Cap", compactUsd(coin.marketCap), Modifier.weight(1f))
                StatCard("Rank", coin.marketCapRank?.let { "#$it" } ?: "N/A", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Volum 24h", compactUsd(coin.totalVolume), Modifier.weight(1f))
                StatCard("Schimbare 24h", coin.priceChange24hAmount?.let { usd(it) } ?: "N/A", Modifier.weight(1f))
            }
            Text("INTERVAL 24H", color = Muted, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Minim", coin.low24h?.let { usd(it) } ?: "N/A", Modifier.weight(1f), Green)
                StatCard("Maxim", coin.high24h?.let { usd(it) } ?: "N/A", Modifier.weight(1f), Red)
            }
            Text(
                if (coin.isFavorite) "★ Salvată la favorite" else "Apasă steluța pentru a salva moneda",
                color = if (coin.isFavorite) Color(0xFFFFC857) else Muted,
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { tradeDialog = true }, modifier = Modifier.weight(1f)) { Text("Cumpără / Vinde") }
                OutlinedButton(onClick = { alertDialog = true }, modifier = Modifier.weight(1f)) { Text("Setează alertă") }
            }
        }
    }
    if (tradeDialog) TradeDialog(coin, holding, { tradeDialog = false },
        { onBuy(it); tradeDialog = false }, { onSell(it); tradeDialog = false })
    if (alertDialog) PriceAlertDialog(coin, { alertDialog = false },
        { value, above -> onCreateAlert(value, above); alertDialog = false })
}

@Composable
fun PeriodButton(label: String, days: Int, selectedDays: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val selected = days == selectedDays
    TextButton(
        onClick = { onSelect(days) },
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (selected) Accent.copy(alpha = 0.18f) else Color.Transparent,
            contentColor = if (selected) Accent else Muted
        )
    ) {
        Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun PriceChart(points: List<PricePoint>) {
    val prices = points.map { it.price }
    val minPrice = prices.minOrNull() ?: 0.0
    val maxPrice = prices.maxOrNull() ?: 0.0
    val range = (maxPrice - minPrice).takeIf { it > 0.0 } ?: 1.0
    val lineColor = if (prices.last() >= prices.first()) Green else Red

    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val horizontalPadding = 4.dp.toPx()
        val verticalPadding = 10.dp.toPx()
        val usableWidth = size.width - horizontalPadding * 2
        val usableHeight = size.height - verticalPadding * 2
        val path = Path()
        prices.forEachIndexed { index, price ->
            val x = horizontalPadding + usableWidth * index / (prices.size - 1).toFloat()
            val y = verticalPadding + usableHeight * (1f - ((price - minPrice) / range).toFloat())
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawLine(
            color = Muted.copy(alpha = 0.22f),
            start = Offset(0f, size.height - verticalPadding),
            end = Offset(size.width, size.height - verticalPadding),
            strokeWidth = 1.dp.toPx()
        )
        drawPath(path = path, color = lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()))
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.White) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
            Text(value, color = valueColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        }
    }
}


@Composable
fun TradeDialog(coin: CryptoEntity, holding: ro.cmo.cryptowatch.data.PortfolioHoldingEntity?, onDismiss: () -> Unit, onBuy: (Double) -> Unit, onSell: (Double) -> Unit) {
    var buyMode by remember { mutableStateOf(true) }
    var amount by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (buyMode) "Cumpără " + coin.symbol else "Vinde " + coin.symbol) },
        text = {
            Column {
                Row {
                    TextButton(onClick = { buyMode = true; amount = "" }) { Text("Cumpără") }
                    TextButton(onClick = { buyMode = false; amount = "" }) { Text("Vinde") }
                }
                Text("Preț: " + usd(coin.currentPrice))
                Text("Deții: " + (holding?.quantity ?: 0.0) + " " + coin.symbol)
                OutlinedTextField(amount, { amount = it }, label = { Text(if (buyMode) "Sumă USD" else "Cantitate") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = {
            val value = amount.replace(',', '.').toDoubleOrNull()
            if (value != null && value > 0) { if (buyMode) onBuy(value) else onSell(value) }
        }) { Text("Confirmă") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anulează") } }
    )
}

@Composable
fun PriceAlertDialog(coin: CryptoEntity, onDismiss: () -> Unit, onSave: (Double, Boolean) -> Unit) {
    var target by remember { mutableStateOf("") }
    var above by remember { mutableStateOf(true) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Alertă " + coin.symbol) },
        text = {
            Column {
                Text("Preț curent: " + usd(coin.currentPrice))
                OutlinedTextField(target, { target = it }, label = { Text("Preț țintă USD") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(above, { above = true }); Text("Peste")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(!above, { above = false }); Text("Sub")
                }
            }
        },
        confirmButton = { TextButton(onClick = {
            val value = target.replace(',', '.').toDoubleOrNull()
            if (value != null && value > 0) onSave(value, above)
        }) { Text("Salvează") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anulează") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(account: ro.cmo.cryptowatch.data.PortfolioAccountEntity?, holdings: List<ro.cmo.cryptowatch.data.PortfolioHoldingEntity>, transactions: List<ro.cmo.cryptowatch.data.PortfolioTransactionEntity>, coins: List<CryptoEntity>, onBack: () -> Unit, onOpenCoin: (String) -> Unit) {
    val cash = account?.cashBalance ?: 10000.0
    val marketValue = holdings.sumOf { h -> h.quantity * (coins.firstOrNull { it.id == h.coinId }?.currentPrice ?: h.averageBuyPrice) }
    val cost = holdings.sumOf { it.quantity * it.averageBuyPrice }
    val pnl = marketValue - cost
    Scaffold(containerColor = AppBackground, topBar = {
        TopAppBar(title = { Text("Portofoliul meu", color = Color.White) }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Înapoi", tint = Color.White) }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground))
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("VALOARE TOTALĂ", color = Muted)
                        Text(usd(cash + marketValue), color = Color.White, style = MaterialTheme.typography.headlineMedium)
                        Text("Sold disponibil: " + usd(cash), color = Muted)
                        Text("Valoare dețineri: " + usd(marketValue), color = Muted)
                        Text("Profit / Pierdere nerealizată: " + usd(pnl), color = changeColor(pnl))
                        Text("Capital virtual inițial: $10,000.00", color = Muted)
                    }
                }
            }
            item { Text("DEȚINERI", color = Muted, fontWeight = FontWeight.Bold) }
            if (holdings.isEmpty()) item { Text("Nu ai cumpărat încă nicio criptomonedă.", color = Muted) }
            items(holdings, key = { it.coinId }) { h ->
                val price = coins.firstOrNull { it.id == h.coinId }?.currentPrice ?: h.averageBuyPrice
                Card(Modifier.fillMaxWidth().clickable { onOpenCoin(h.coinId) }, colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text(h.coinName, color = Color.White); Text(String.format(Locale.US, "%.6f", h.quantity) + " " + h.symbol, color = Muted) }
                        Text(usd(h.quantity * price), color = Color.White)
                    }
                }
            }
            item { Text("TRANZACȚII RECENTE", color = Muted, fontWeight = FontWeight.Bold) }
            if (transactions.isEmpty()) item { Text("Tranzacțiile vor apărea aici.", color = Muted) }
            items(transactions, key = { it.id }) { t ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(t.type + " • " + t.coinName, color = if (t.type == "BUY") Red else Green)
                    Text(usd(t.total), color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(alerts: List<ro.cmo.cryptowatch.data.PriceAlertEntity>, onBack: () -> Unit, onDelete: (Long) -> Unit) {
    Scaffold(containerColor = AppBackground, topBar = {
        TopAppBar(title = { Text("Alerte de preț", color = Color.White) }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Înapoi", tint = Color.White) }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground))
    }) { padding ->
        if (alerts.isEmpty()) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("Nu ai alerte configurate.", color = Muted)
        } else LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(alerts, key = { it.id }) { a ->
                Card(colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(a.coinName + " (" + a.symbol + ")", color = Color.White)
                            Text((if (a.isAbove) "Peste " else "Sub ") + usd(a.targetPrice), color = Muted)
                            Text(if (a.isTriggered) "Declanșată" else if (a.isActive) "Activă" else "Inactivă", color = if (a.isTriggered) Green else Muted)
                        }
                        TextButton(onClick = { onDelete(a.id) }) { Text("Șterge", color = Red) }
                    }
                }
            }
        }
    }
}
