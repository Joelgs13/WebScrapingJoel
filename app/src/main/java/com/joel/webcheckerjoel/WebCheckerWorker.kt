package com.joel.webcheckerjoel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import org.jsoup.Jsoup
import java.lang.Exception
import java.util.Locale

class WebCheckerWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        try {
            val sharedPreferences = applicationContext.getSharedPreferences("WebCheckerPrefs", Context.MODE_PRIVATE)

            val semaforo = sharedPreferences.getString("semaforo", null) ?: return Result.failure()

            if (isStopped || semaforo.equals("R")) {
                println("stopped")
                return Result.failure()

            }
            // Obtener la URL y la palabra desde las preferencias compartidas
            val url = sharedPreferences.getString("url", null) ?: return Result.failure()
            val word = sharedPreferences.getString("word", null) ?: return Result.failure()
            if (isStopped || semaforo.equals("R")) {
                return Result.failure()
            }
            // Conectar a la página web
            val doc = Jsoup.connect(url).get()
            if (isStopped || semaforo.equals("R")) {
                return Result.failure()
            }

            // Verificar si la palabra está presente
            if (doc.text().contains(word, ignoreCase = true)) {
                sendNotification()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }

        return Result.success()
    }

    /**
     * Envía una notificación al usuario si se encuentra la palabra en la página web.
     *
     * <p>La notificación se muestra con el título "¡Se encontró la palabra!"
     * y un mensaje que indica que la palabra fue encontrada.</p>
     */
    private fun sendNotification() {
        val context = applicationContext

        // Crear el canal de notificación (solo necesario para API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "guestlist_channel",
                "Guestlist Notification",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Canal para notificaciones de palabras encontradas"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Crear y enviar la notificación
        val notification: Notification = NotificationCompat.Builder(context, "guestlist_channel")
            .setContentTitle("¡Se encontró la palabra!")
            .setContentText("La palabra fue encontrada en la página web.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }
}
