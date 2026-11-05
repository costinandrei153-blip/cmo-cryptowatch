package ro.cmo.cryptowatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// Model: un element din listă
data class Coin(
    val id: String, val name: String, val symbol: String,
    val price: Double, val change24h: Double, val marketCap: Double, val rank: Int
)

// Date de TEST (inventate, doar pentru checkpoint-ul cu lista). Se înlocuiesc cu date din API la C4.
val sampleCoins = listOf(
    Coin("bitcoin", "Bitcoin", "BTC", 65000.0, 1.8, 1_280_000_000_000.0, 1),
    Coin("ethereum", "Ethereum", "ETH", 3200.0, -0.9, 385_000_000_000.0, 2),
    Coin("solana", "Solana", "SOL", 150.0, 3.4, 70_000_000_000.0, 5),
    Coin("cardano", "Cardano", "ADA", 0.45, -2.1, 16_000_000_000.0, 9),
    Coin("dogecoin", "Dogecoin", "DOGE", 0.12, 0.5, 17_000_000_000.0, 8)
)

fun usd(v: Double) = "$" + String.format("%,.2f", v)
fun pct(v: Double) = String.format("%+.2f%%", v)
fun changeColor(v: Double) = if (v >= 0) Color(0xFF1B7F4B) else Color(0xFFB3261E)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { App() } }
    }
}

// Navigare între cele două ecrane
@Composable
fun App() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "list") {
        composable("list") { ListScreen { id -> nav.navigate("detail/$id") } }
        composable("detail/{id}") { entry ->
            val coin = sampleCoins.first { it.id == entry.arguments?.getString("id") }
            DetailScreen(coin) { nav.popBackStack() }
        }
    }
}

// Ecranul 1: lista
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(onOpen: (String) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("CryptoWatch") }) }) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            items(sampleCoins, key = { it.id }) { coin ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(coin.id) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(coin.name, style = MaterialTheme.typography.titleMedium)
                        Text(coin.symbol, style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(usd(coin.price))
                        Text(pct(coin.change24h), color = changeColor(coin.change24h))
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

// Ecranul 2: detaliul elementului selectat
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(coin: Coin, onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(coin.name) },
            navigationIcon = { TextButton(onClick = onBack) { Text("Înapoi") } }
        )
    }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(usd(coin.price), style = MaterialTheme.typography.displaySmall)
            Text("Variație 24h: " + pct(coin.change24h), color = changeColor(coin.change24h))
            Text("Simbol: ${coin.symbol}")
            Text("Loc în top: #${coin.rank}")
            Text("Capitalizare: " + usd(coin.marketCap))
        }
    }
}