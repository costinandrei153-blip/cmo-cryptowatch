package ro.cmo.cryptowatch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ro.cmo.cryptowatch.data.CryptoEntity
import ro.cmo.cryptowatch.data.PortfolioAccountEntity
import ro.cmo.cryptowatch.data.PortfolioHoldingEntity
import ro.cmo.cryptowatch.data.PortfolioTransactionEntity
import ro.cmo.cryptowatch.data.PriceAlertEntity
import java.util.Locale

@Composable
fun TradeDialog(
    coin: CryptoEntity,
    holding: PortfolioHoldingEntity?,
    onDismiss: () -> Unit,
    onBuy: (Double) -> Unit,
    onSell: (Double) -> Unit
) {
    var buying by remember { mutableStateOf(true) }
    var amount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text((if (buying) "Cumpără " else "Vinde ") + coin.symbol) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row {
                    TextButton(onClick = { buying = true; amount = "" }) { Text("Cumpără") }
                    TextButton(onClick = { buying = false; amount = "" }) { Text("Vinde") }
                }
                Text("Preț curent: " + usd(coin.currentPrice))
                Text("Deții: " + String.format(Locale.US, "%.8f", holding?.quantity ?: 0.0) + " " + coin.symbol)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(if (buying) "Sumă în USD" else "Cantitate de vândut") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = amount.replace(',', '.').toDoubleOrNull()
                if (value != null && value > 0.0) {
                    if (buying) onBuy(value) else onSell(value)
                }
            }) { Text("Confirmă") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anulează") } }
    )
}

@Composable
fun PriceAlertDialog(
    coin: CryptoEntity,
    onDismiss: () -> Unit,
    onSave: (Double, Boolean) -> Unit
) {
    var target by remember { mutableStateOf("") }
    var above by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alertă pentru " + coin.symbol) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Preț curent: " + usd(coin.currentPrice))
                OutlinedTextField(target, { target = it }, label = { Text("Preț țintă USD") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = above, onClick = { above = true })
                    Text("Când crește peste")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !above, onClick = { above = false })
                    Text("Când scade sub")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = target.replace(',', '.').toDoubleOrNull()
                if (value != null && value > 0.0) onSave(value, above)
            }) { Text("Salvează") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anulează") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    account: PortfolioAccountEntity?,
    holdings: List<PortfolioHoldingEntity>,
    transactions: List<PortfolioTransactionEntity>,
    coins: List<CryptoEntity>,
    onBack: () -> Unit,
    onOpenCoin: (String) -> Unit
) {
    val cash = account?.cashBalance ?: 10000.0
    val marketValue = holdings.sumOf { holding ->
        val price = coins.firstOrNull { it.id == holding.coinId }?.currentPrice ?: holding.averageBuyPrice
        holding.quantity * price
    }
    val pnl = cash + marketValue - 10000.0
    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Portofoliul meu", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Înapoi", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("VALOARE TOTALĂ", color = Muted)
                        Text(usd(cash + marketValue), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Sold disponibil: " + usd(cash), color = Muted)
                        Text("Valoare dețineri: " + usd(marketValue), color = Muted)
                        Text("Profit / Pierdere totală: " + usd(pnl), color = if (pnl >= 0) Color(0xFF31C48D) else Color(0xFFFF6B75))
                        Text("Capital virtual inițial: $10,000.00", color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            item { Text("DEȚINERI", color = Muted, fontWeight = FontWeight.Bold) }
            if (holdings.isEmpty()) item { Text("Nu ai cumpărat încă nicio criptomonedă. Deschide o monedă și apasă Cumpără.", color = Muted) }
            items(holdings, key = { it.coinId }) { holding ->
                val price = coins.firstOrNull { it.id == holding.coinId }?.currentPrice ?: holding.averageBuyPrice
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenCoin(holding.coinId) },
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(holding.coinName, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(String.format(Locale.US, "%.8f", holding.quantity) + " " + holding.symbol, color = Muted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(usd(holding.quantity * price), color = Color.White)
                            Text("Cost mediu: " + usd(holding.averageBuyPrice), color = Muted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            item { Text("TRANZACȚII RECENTE", color = Muted, fontWeight = FontWeight.Bold) }
            if (transactions.isEmpty()) item { Text("Tranzacțiile tale vor apărea aici.", color = Muted) }
            items(transactions, key = { it.id }) { transaction ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(transaction.type + " • " + transaction.coinName, color = if (transaction.type == "BUY") Color(0xFFFF6B75) else Color(0xFF31C48D))
                        Text(String.format(Locale.US, "%.8f", transaction.quantity) + " " + transaction.symbol, color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(usd(transaction.total), color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    alerts: List<PriceAlertEntity>,
    onBack: () -> Unit,
    onDelete: (Long) -> Unit
) {
    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Alerte de preț", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Înapoi", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        }
    ) { padding ->
        if (alerts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Nu ai configurat alerte. Deschide o criptomonedă și apasă Setează alertă.", color = Muted)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(alerts, key = { it.id }) { alert ->
                    Card(colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(alert.coinName + " (" + alert.symbol + ")", color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text((if (alert.isAbove) "Peste " else "Sub ") + usd(alert.targetPrice), color = Muted)
                                Text(
                                    when {
                                        alert.isTriggered -> "Declanșată"
                                        alert.isActive -> "Activă"
                                        else -> "Inactivă"
                                    },
                                    color = if (alert.isTriggered) Color(0xFF31C48D) else Muted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            TextButton(onClick = { onDelete(alert.id) }) { Text("Șterge", color = Color(0xFFFF6B75)) }
                        }
                    }
                }
            }
        }
    }
}
