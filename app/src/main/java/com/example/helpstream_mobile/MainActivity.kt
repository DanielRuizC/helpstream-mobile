package com.example.helpstream_mobile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var selectedFileUri: Uri? = null
    private lateinit var tvAdjuntarTexto: TextView
    private lateinit var btnAdjuntarEvidencia: View

    private val selectFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            val fileName = getFileName(it)
            tvAdjuntarTexto.text = "✅ Evidencia: $fileName"
            btnAdjuntarEvidencia.setBackgroundResource(R.drawable.bg_rounded_success)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etDescripcion = findViewById<EditText>(R.id.etDescripcion)
        val btnReportar = findViewById<MaterialButton>(R.id.btnReportar)
        btnAdjuntarEvidencia = findViewById(R.id.btnAdjuntarEvidencia)
        tvAdjuntarTexto = findViewById(R.id.tvAdjuntarTexto)

        btnAdjuntarEvidencia.setOnClickListener {
            selectFileLauncher.launch("*/*")
        }

        btnReportar.setOnClickListener {
            val descripcionFalla = etDescripcion.text.toString().trim()

            if (descripcionFalla.isEmpty()) {
                Toast.makeText(this, "Por favor, describe la falla", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sharedPref = getSharedPreferences("HelpStreamSession", Context.MODE_PRIVATE)
            val userId = sharedPref.getInt("USER_ID", 101)

            enviarTicketMultipart(userId, descripcionFalla)
        }
    }

    private fun enviarTicketMultipart(userId: Int, descripcion: String) {
        lifecycleScope.launch {
            try {
                val descriptionBody = descripcion.toRequestBody("text/plain".toMediaTypeOrNull())
                val userIdBody = userId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                
                var filePart: MultipartBody.Part? = null
                selectedFileUri?.let { uri ->
                    filePart = prepareFilePart("archivo", uri)
                }

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.crearTicket(descriptionBody, userIdBody, filePart)
                }

                if (response.isSuccessful) {
                    val ticketRes = response.body()
                    if (ticketRes != null) {
                        val intent = Intent(this@MainActivity, TicketConfirmacionActivity::class.java).apply {
                            putExtra("TICKET_ID", ticketRes.id)
                            putExtra("FECHA", ticketRes.fecha_creacion)
                            putExtra("DESCRIPCION", ticketRes.descripcion)
                            putStringArrayListExtra("TAGS", ArrayList(ticketRes.palabras_clave))
                            putExtra("CRITICIDAD", ticketRes.criticidad)
                        }
                        startActivity(intent)
                        finish()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Error en el servidor: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun prepareFilePart(partName: String, fileUri: Uri): MultipartBody.Part? {
        val file = getFileFromUri(fileUri) ?: return null
        val mediaType = contentResolver.getType(fileUri)?.toMediaTypeOrNull()
        val requestFile = file.asRequestBody(mediaType)
        return MultipartBody.Part.createFormData(partName, file.name, requestFile)
    }

    private fun getFileFromUri(uri: Uri): File? {
        val fileName = getFileName(uri)
        val file = File(cacheDir, fileName)
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "temp_file"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
}