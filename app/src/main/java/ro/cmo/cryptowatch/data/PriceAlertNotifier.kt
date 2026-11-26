package ro.cmo.cryptowatch.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ro.cmo.cryptowatch.R

object PriceAlertNotifier {
    private const val CHANNEL_ID = "crypto_price_alerts"

    fun show(context: Context, alert: PriceAlertEntity, currentPrice: Double) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alerte preț CryptoWatch",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notificări când o criptomonedă atinge prețul setat." }
            manager.createNotificationChannel(channel)
        }

        val direction = if (alert.isAbove) "a urcat peste" else "a scăzut sub"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Alertă de preț: " + alert.coinName)
            .setContentText(alert.coinName + " " + direction + " " + alert.targetPrice + " USD. Preț detectat: " + currentPrice + " USD.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                alert.coinName + " (" + alert.symbol + ") " + direction + " pragul de " +
                    alert.targetPrice + " USD. Preț detectat: " + currentPrice + " USD."
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(alert.id.toInt(), notification)
        } catch (_: SecurityException) {
            // User denied notification permission.
        }
    }
}
