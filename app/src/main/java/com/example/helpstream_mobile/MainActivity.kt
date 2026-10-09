package com.example.helpstream_mobile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
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
            tvAdjuntarTexto.text = "📎 Evidencia: $fileName"
            btnAdjuntarEvidencia.setBackgroundResource(R.drawable.bg_upload_button)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etDescripcion = findViewById<TextInputEditText>(R.id.etDescripcion)
        val spinnerSede = findViewById<Spinner>(R.id.spinnerSede)
        val spinnerPiso = findViewById<Spinner>(R.id.spinnerPiso)
        val btnReportar = findViewById<MaterialButton>(R.id.btnReportar)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        btnAdjuntarEvidencia = findViewById(R.id.btnAdjuntarEvidencia)
        tvAdjuntarTexto = findViewById(R.id.tvAdjuntarTexto)

        val placeholderSede = "Seleccione una Sede..."
        val placeholderPiso = "Seleccione un Piso..."

        val sedes = listOf(placeholderSede, "San Isidro", "Chancay", "Ilo", "Pisco", "Coishco")
        val pisosSanIsidro = listOf(
            placeholderPiso,
            "3er piso - Gerencia General", 
            "3er piso - Gerencia Flota", 
            "3er piso - Gerencia Operaciones", 
            "4to piso - Gerencia de Administración y Finanzas", 
            "5to piso - Gerencia de Recursos Humanos"
        )

        val sedeAdapter = android.widget.ArrayAdapter(this, R.layout.custom_spinner_item, sedes)
        sedeAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item)
        spinnerSede.adapter = sedeAdapter

        val pisoAdapter = android.widget.ArrayAdapter(this, R.layout.custom_spinner_item, mutableListOf(placeholderPiso))
        pisoAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item)
        spinnerPiso.adapter = pisoAdapter
        spinnerPiso.isEnabled = false

        val hideKeyboardListener = View.OnTouchListener { v, _ ->
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
            false
        }
        spinnerSede.setOnTouchListener(hideKeyboardListener)
        spinnerPiso.setOnTouchListener(hideKeyboardListener)

        spinnerSede.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                val sedeSeleccionada = sedes[position]
                if (sedeSeleccionada == "San Isidro") {
                    spinnerPiso.isEnabled = true
                    pisoAdapter.clear()
                    pisoAdapter.addAll(pisosSanIsidro)
                    pisoAdapter.notifyDataSetChanged()
                    spinnerPiso.setSelection(0)
                } else {
                    spinnerPiso.isEnabled = false
                    pisoAdapter.clear()
                    pisoAdapter.add(if (sedeSeleccionada == placeholderSede) placeholderPiso else "No aplica piso")
                    pisoAdapter.notifyDataSetChanged()
                    spinnerPiso.setSelection(0)
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        btnAdjuntarEvidencia.setOnClickListener {
            selectFileLauncher.launch("*/*")
        }

        btnReportar.setOnClickListener {
            val descripcionFalla = etDescripcion.text.toString().trim()
            val sede = spinnerSede.selectedItem?.toString() ?: ""
            val piso = if (spinnerPiso.isEnabled) spinnerPiso.selectedItem?.toString() ?: "" else ""

            if (descripcionFalla.isEmpty()) {
                Toast.makeText(this, "Por favor, describe tu problema", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validación de dropdowns con placeholder por defecto
            if (sede.isEmpty() || sede == placeholderSede) {
                Toast.makeText(this, "Por favor, seleccione una Sede válida.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (spinnerPiso.isEnabled && (piso.isEmpty() || piso == placeholderPiso)) {
                Toast.makeText(this, "Por favor, seleccione un Piso / Área válido.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sharedPref = getSharedPreferences("HelpStreamSession", Context.MODE_PRIVATE)
            val userId = sharedPref.getInt("USER_ID", 101)
            val correo = sharedPref.getString("USER_EMAIL", "") ?: ""

            if (correo.isEmpty()) {
                Toast.makeText(this, "Error: No se encontró el correo en la sesión", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Prevención de multi-clics y activación de feedback visual
            btnReportar.isEnabled = false
            progressBar.visibility = View.VISIBLE

            val pisoFinal = if (spinnerPiso.isEnabled && piso != placeholderPiso) piso else "-"
            enviarTicketMultipart(userId, descripcionFalla, correo, sede, pisoFinal, btnReportar, progressBar, etDescripcion, spinnerSede, spinnerPiso)
        }
    }

    private fun enviarTicketMultipart(
        userId: Int,
        descripcion: String,
        correo: String,
        sede: String,
        piso: String,
        btnReportar: MaterialButton,
        progressBar: ProgressBar,
        etDescripcion: TextInputEditText,
        spinnerSede: Spinner,
        spinnerPiso: Spinner
    ) {
        lifecycleScope.launch {
            try {
                val descriptionBody = descripcion.toRequestBody("text/plain".toMediaTypeOrNull())
                val userIdBody = userId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val correoBody = correo.toRequestBody("text/plain".toMediaTypeOrNull())
                val sedeBody = sede.toRequestBody("text/plain".toMediaTypeOrNull())
                val pisoBody = piso.toRequestBody("text/plain".toMediaTypeOrNull())
                
                var filePart: MultipartBody.Part? = null
                selectedFileUri?.let { uri ->
                    filePart = prepareFilePart("archivo", uri)
                }

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.getInstance(this@MainActivity).crearTicket(descriptionBody, userIdBody, correoBody, sedeBody, pisoBody, filePart)
                }

                if (response.isSuccessful) {
                    // Éxito: Limpiar formulario y restablecer estados
                    etDescripcion.text?.clear()
                    spinnerSede.setSelection(0)
                    spinnerPiso.setSelection(0)
                    selectedFileUri = null
                    tvAdjuntarTexto.text = "Adjuntar archivos (foto/video opcional)"
                    btnAdjuntarEvidencia.setBackgroundResource(R.drawable.bg_upload_button)
                    btnReportar.isEnabled = true
                    progressBar.visibility = View.GONE

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
                    // Error de servidor: reactivar botón y ocultar barra de carga
                    btnReportar.isEnabled = true
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Error en el servidor: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Error de red / timeout: reactivar botón y ocultar barra de carga
                btnReportar.isEnabled = true
                progressBar.visibility = View.GONE
                Toast.makeText(this@MainActivity, "Error al enviar solicitud: ${e.message}", Toast.LENGTH_LONG).show()
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
