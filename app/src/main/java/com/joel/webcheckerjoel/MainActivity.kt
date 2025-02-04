package com.joel.webcheckerjoel

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import android.Manifest
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import java.util.UUID

/**
 * Actividad principal de la aplicación WebChecker.
 *
 * <p>Esta actividad maneja la lógica para interactuar con el usuario, incluyendo los botones
 * para iniciar y detener un trabajo periódico que verifica una URL, así como la captura de entradas
 * de texto para la URL y la palabra clave.</p>
 */
class MainActivity : AppCompatActivity() {

    // Etiqueta única para identificar los trabajos programados
    private val workTag = "WebCheckerWork"

    // ID del trabajo en ejecución, generado dinámicamente
    private var workId = UUID.randomUUID()

    // Semáforo para gestionar el estado de los trabajos
    private var semaforo = "R"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Solicitar permisos para notificaciones si es necesario
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        // Inicializar campos de texto
        val urlField = findViewById<EditText>(R.id.URL)
        val wordField = findViewById<EditText>(R.id.FINDTEXT)

        // Inicializar botones
        val btnPlay = findViewById<Button>(R.id.play)
        val btnStop = findViewById<Button>(R.id.parar)

        // Deshabilitar el botón "Play" al inicio
        btnPlay.isEnabled = false

        // Función para verificar si ambos campos tienen texto
        fun checkFields() {
            val urlText = urlField.text.toString().trim()
            val wordText = wordField.text.toString().trim()
            btnPlay.isEnabled = urlText.isNotEmpty() && wordText.isNotEmpty()
        }

        // Agregar TextWatcher a ambos campos
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkFields()
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        // Asignar el mismo TextWatcher a ambos campos
        urlField.addTextChangedListener(textWatcher)
        wordField.addTextChangedListener(textWatcher)

        // Listener para el botón "Play"
        btnPlay.setOnClickListener {
            this.semaforo = "V"
            val sharedPreferences = getSharedPreferences("WebCheckerPrefs", MODE_PRIVATE)

            // Obtener la URL y la palabra clave guardadas
            val url = urlField.text.toString().trim()
            val word = wordField.text.toString().trim()

            // Verifica que ambos valores sean correctos antes de guardarlos
            if (url.isEmpty() || word.isEmpty()) {
                println("Error: URL o palabra clave vacías. No se inicia el servicio.")
                return@setOnClickListener
            }

            // Guardar los valores en SharedPreferences correctamente
            sharedPreferences.edit()
                .putString("url", url)
                .putString("word", word)
                .putString("semaforo", semaforo)
                .apply()

            // Cancelar cualquier trabajo previamente encolado con la misma etiqueta
            WorkManager.getInstance(this).cancelAllWorkByTag(this.workTag)

            // Crear una solicitud de trabajo periódico
            val workRequest = PeriodicWorkRequestBuilder<WebCheckerWorker>(
                15, // Intervalo mínimo de 15 minutos
                TimeUnit.MINUTES
            ).addTag(this.workTag).build()

            // Encolar el trabajo periódico
            WorkManager.getInstance(this).enqueue(workRequest)

            // Guardar el ID del trabajo en ejecución
            this.workId = workRequest.id
        }


        // Listener para el botón "Stop"
        btnStop.setOnClickListener {
            println("Pulso boton stop")
            this.semaforo = "R"
            val sharedPreferences = getSharedPreferences("WebCheckerPrefs", MODE_PRIVATE)
            sharedPreferences.edit().putString("semaforo", semaforo).apply()

            // Detener el trabajo en ejecución
            stopWork()
        }
    }

    /**
     * Método para detener la tarea de fondo y cancelar los trabajos en ejecución.
     *
     * <p>Este método cancela todos los trabajos asociados con la etiqueta o el ID
     * almacenado. Además, limpia los trabajos pendientes de ejecución.</p>
     */
    private fun stopWork() {
        // Cancelar trabajos específicos por ID
        WorkManager.getInstance(this).cancelWorkById(this.workId)

        // Limpiar trabajos completados o fallidos
        WorkManager.getInstance(this).pruneWork()

        // Obtener los trabajos asociados con la etiqueta y cancelarlos si están encolados o en ejecución
        WorkManager.getInstance(this).getWorkInfosByTag(workTag).get().forEach { workInfo ->
            println("Trabajo ID: ${workInfo.id}, Estado: ${workInfo.state}")

            // Cancelar solo si el trabajo está encolado o en ejecución
            if (workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING) {
                WorkManager.getInstance(this).cancelWorkById(workInfo.id)
                println("Trabajo con ID ${workInfo.id} cancelado")
            }
        }
    }
}
