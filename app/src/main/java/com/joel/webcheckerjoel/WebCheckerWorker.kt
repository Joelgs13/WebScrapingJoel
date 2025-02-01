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

            // Extraer el texto de la página
            val textoPagina = doc.text()

            // Contar las ocurrencias de la palabra en el texto de la página (ignorando mayúsculas/minúsculas)
            val contarOcurrencias = Regex(Regex.escape(word), RegexOption.IGNORE_CASE).findAll(textoPagina).count()

            // Si encontramos la palabra, actualizamos el contador
            if (contarOcurrencias > 0) {
                // Obtener el contador actual de veces que se encontró la palabra
                val currentCount = sharedPreferences.getInt("word_count", 0)
                val newCount = currentCount + contarOcurrencias
                val lastFoundDate = System.currentTimeMillis()

                // Guardar el nuevo contador y la fecha
                sharedPreferences.edit().apply {
                    putInt("word_count", newCount)
                    putLong("last_found_date", lastFoundDate)
                    apply()
                }

                sendNotification(newCount, lastFoundDate)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }

        return Result.success()
    }

    private fun sendNotification(count: Int, lastFoundDate: Long) {
        val context = applicationContext

        // Crear el canal de notificación (solo necesario para API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

        val ultimaFechaFormateada = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(lastFoundDate)

        // Crear y enviar la notificación
        val notification: Notification = NotificationCompat.Builder(context, "guestlist_channel")
            .setContentTitle("¡Se encontró la palabra!")
            .setContentText("La palabra fue encontrada $count veces. Última vez: $ultimaFechaFormateada")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }
}
