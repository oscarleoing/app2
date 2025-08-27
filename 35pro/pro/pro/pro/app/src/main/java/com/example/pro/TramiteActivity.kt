package com.example.pro

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.example.pro.databinding.ActivityTramiteBinding
import com.example.pro.databinding.LayoutModalGrabacionBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class TramiteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTramiteBinding
    private lateinit var grabacionBinding: LayoutModalGrabacionBinding

    private val imagesList = mutableListOf<Bitmap>()
    private val imagePaths = mutableListOf<String>()
    private var videoPath: String? = null
    private val audioPaths = mutableListOf<String>()
    private val pdfPaths = mutableListOf<String>()

    private var currentPhotoPath: String? = null
    private var recorder: MediaRecorder? = null
    private var isRecording = false
    private val handler = Handler()
    private var segundosGrabacion = 0
    private var archivoAudioTemporal: File? = null

    // Mapa botones con colores para el borde
    private val buttonColors = mapOf(
        "btnAlumbrado" to Color.parseColor("#FFC107"),
        "btnCalles" to Color.parseColor("#9E9E9E"),
        "btnMIAA" to Color.parseColor("#03A9F4"),
        "btnApoyos" to Color.parseColor("#E91E63"),
        "btnParques" to Color.parseColor("#4CAF50"),
        "btnCiudad" to Color.parseColor("#FF8A65"),
        "btnConvivencia" to Color.parseColor("#607D8B"),
        "btnVigilancia" to Color.parseColor("#B71C1C"),
        "btnLimpia" to Color.parseColor("#673AB7"),
        "btnOtros" to Color.parseColor("#FF9800"),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTramiteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        grabacionBinding = LayoutModalGrabacionBinding.inflate(layoutInflater)
        grabacionBinding.modalBackground.setBackgroundColor(0x88000000.toInt())
        grabacionBinding.modalContent.setBackgroundResource(R.drawable.bg_modal)
        grabacionBinding.modalContent.elevation = 12f

        mostrarLayoutGrabandoAudio(false)

        binding.layoutFoto.setOnClickListener {
            mostrarDialogoSeleccion()
        }

        grabacionBinding.btnStartRecording.setOnClickListener {
            if (isRecording) {
                detenerGrabacion()
            } else {
                pedirPermisoYGrabar()
            }
        }

        grabacionBinding.btnCancelarGrabacion.setOnClickListener {
            cancelarGrabacion()
        }

        grabacionBinding.modalBackground.setOnClickListener {
            if (!isRecording) mostrarLayoutGrabandoAudio(false)
        }

        binding.btnSubmit.setOnClickListener {
            Toast.makeText(this, "Reporte enviado ✅", Toast.LENGTH_SHORT).show()
            finish()
        }

        verificarYSolicitarPermisos()

        actualizarGaleria()

        // *** NUEVO: Inicialmente deshabilitar el formulario ***
        setFormEnabled(false)

        // Lista de botones que activan el formulario y cambian borde
        val buttons = listOf(
            binding.btnAlumbrado,
            binding.btnCalles,
            binding.btnMIAA,
            binding.btnApoyos,
            binding.btnParques,
            binding.btnCiudad,
            binding.btnConvivencia,
            binding.btnVigilancia,
            binding.btnLimpia,
            binding.btnOtros,
        )

        buttons.forEach { btn ->
            btn.setOnClickListener {
                // Habilitar formulario
                setFormEnabled(true)

                // Poner borde con color según botón
                val color = buttonColors[resources.getResourceEntryName(btn.id)] ?: Color.BLACK
                setFormBorderColor(color)
            }
        }
    }

    // Habilita o deshabilita todos los hijos de binding.main (el formulario)
    private fun setFormEnabled(enabled: Boolean) {
        setEnabledRecursively(binding.main, enabled)
    }

    private fun setEnabledRecursively(view: View, enabled: Boolean) {
        view.isEnabled = enabled
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setEnabledRecursively(view.getChildAt(i), enabled)
            }
        }
    }

    // Pone borde visible alrededor del formulario con el color del botón seleccionado
    private fun setFormBorderColor(color: Int) {
        val drawable = GradientDrawable()
        drawable.setStroke(8, color)
        drawable.cornerRadius = 16f
        drawable.setColor(Color.WHITE)
        binding.main.background = drawable
    }

    private fun mostrarDialogoSeleccion() {
        val opciones = arrayOf("Tomar foto", "Elegir de galería", "Grabar video", "Grabar audio", "Agregar PDF")
        AlertDialog.Builder(this)
            .setTitle("Selecciona una opción")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> tomarFoto()
                    1 -> seleccionarDeGaleria()
                    2 -> grabarVideo()
                    3 -> mostrarLayoutGrabandoAudio(true)
                    4 -> seleccionarPdf()
                }
            }
            .show()
    }

    private fun pedirPermisoYGrabar() {
        val permiso = Manifest.permission.RECORD_AUDIO
        if (ActivityCompat.checkSelfPermission(this, permiso) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permiso), 200)
        } else {
            iniciarGrabacion()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            val permisosDenegados = mutableListOf<String>()
            for (i in permissions.indices) {
                if (grantResults[i] != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    permisosDenegados.add(permissions[i])
                }
            }
            if (permisosDenegados.isNotEmpty()) {
                Toast.makeText(this, "Se necesitan todos los permisos para un correcto funcionamiento.", Toast.LENGTH_LONG).show()
            }
        }

        if (requestCode == 200) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                iniciarGrabacion()
            } else {
                Toast.makeText(this, "Permiso de grabación denegado.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun iniciarGrabacion() {
        val fileName = "AUDIO_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.3gp"
        val file = File(cacheDir, fileName)
        archivoAudioTemporal = file

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        isRecording = true
        segundosGrabacion = 0
        grabacionBinding.tvRecordingTimer.text = "00:00"
        grabacionBinding.btnStartRecording.text = "Guardar"
        handler.post(runnableContador())
    }

    private fun detenerGrabacion() {
        grabacionBinding.tvRecordingTimer.text = "00:00"
        if (!isRecording) return
        isRecording = false
        try {
            recorder?.stop()
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }
        recorder?.release()
        recorder = null
        handler.removeCallbacksAndMessages(null)

        archivoAudioTemporal?.let {
            audioPaths.add(it.absolutePath)
            archivoAudioTemporal = null
        }
        mostrarLayoutGrabandoAudio(false)
        grabacionBinding.btnStartRecording.text = "Grabar"
        actualizarGaleria()
    }

    private fun cancelarGrabacion() {
        grabacionBinding.tvRecordingTimer.text = "00:00"
        if (isRecording) {
            try {
                recorder?.stop()
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
        }
        recorder?.release()
        recorder = null
        isRecording = false
        handler.removeCallbacksAndMessages(null)

        archivoAudioTemporal?.let {
            if (it.exists()) it.delete()
            archivoAudioTemporal = null
        }
        mostrarLayoutGrabandoAudio(false)
        grabacionBinding.btnStartRecording.text = "Grabar"
    }

    private fun runnableContador(): Runnable = object : Runnable {
        override fun run() {
            segundosGrabacion++
            val min = segundosGrabacion / 60
            val seg = segundosGrabacion % 60
            grabacionBinding.tvRecordingTimer.text = String.format("%02d:%02d", min, seg)
            handler.postDelayed(this, 1000)
        }
    }

    private fun mostrarLayoutGrabandoAudio(mostrar: Boolean) {
        if (mostrar) {
            if (grabacionBinding.root.parent == null) {
                addContentView(
                    grabacionBinding.root,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                )
            }
            grabacionBinding.root.visibility = View.VISIBLE
        } else {
            grabacionBinding.root.visibility = View.GONE
        }
    }

    private fun tomarFoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile: File? = try {
            crearArchivoImagen()
        } catch (ex: IOException) {
            null
        }

        photoFile?.also {
            val photoURI = FileProvider.getUriForFile(this, "${packageName}.fileprovider", it)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            tomarFotoLauncher.launch(intent)
        }
    }

    private fun crearArchivoImagen(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = cacheDir
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    private val tomarFotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && currentPhotoPath != null) {
            val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
            agregarImagen(bitmap, currentPhotoPath!!)
        }
    }

    private fun seleccionarDeGaleria() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        seleccionarImagenLauncher.launch(intent)
    }

    private val seleccionarImagenLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                val copiedFile = copiarImagenACache(it)
                copiedFile?.let { file -> agregarImagen(bitmap, file.absolutePath) }
            }
        }
    }

    private fun copiarImagenACache(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(cacheDir, "IMG_${timeStamp}.jpg")
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun agregarImagen(bitmap: Bitmap, path: String) {
        imagesList.add(bitmap)
        imagePaths.add(path)
        actualizarGaleria()
    }

    private fun grabarVideo() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        val videoFile = crearArchivoVideo()
        videoFile?.also {
            val videoURI = FileProvider.getUriForFile(this, "${packageName}.fileprovider", it)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, videoURI)
            grabarVideoLauncher.launch(intent)
        }
    }

    private fun crearArchivoVideo(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(cacheDir, "VID_${timeStamp}.mp4")
        videoPath = file.absolutePath
        return file
    }

    private val grabarVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && videoPath != null) {
            actualizarGaleria()
        } else {
            videoPath = null
        }
    }

    private fun seleccionarPdf() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        seleccionarPdfLauncher.launch(intent)
    }

    private val seleccionarPdfLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let {
                val copiedFile = copiarPdfACache(it)
                copiedFile?.let { file ->
                    pdfPaths.add(file.absolutePath)
                    actualizarGaleria()
                }
            }
        }
    }



    private fun copiarPdfACache(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(cacheDir, "PDF_${timeStamp}.pdf")
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun actualizarGaleria() {
        binding.imageContainer.removeAllViews()
        binding.audioContainer.removeAllViews()
        binding.pdfContainer.removeAllViews()

        val tieneImagenesOVideo = imagesList.isNotEmpty() || videoPath != null
        val tieneAudios = audioPaths.isNotEmpty()
        val tienePdfs = pdfPaths.isNotEmpty()

        binding.tvGalleryLabel.visibility = if (tieneImagenesOVideo) View.VISIBLE else View.GONE
        binding.horizontalScrollView.visibility = if (tieneImagenesOVideo) View.VISIBLE else View.GONE

        binding.tvAudioLabel.visibility = if (tieneAudios) View.VISIBLE else View.GONE
        binding.audioContainer.visibility = if (tieneAudios) View.VISIBLE else View.GONE

        binding.tvPdfLabel.visibility = if (tienePdfs) View.VISIBLE else View.GONE
        binding.pdfContainer.visibility = if (tienePdfs) View.VISIBLE else View.GONE

        // Mostrar imágenes
        for ((index, bitmap) in imagesList.withIndex()) {
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_imagen, binding.imageContainer, false)
            val imageView: ImageView = itemView.findViewById(R.id.imageView)
            val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminarImagen)

            imageView.setImageBitmap(bitmap)
            btnEliminar.setOnClickListener {
                imagesList.removeAt(index)
                imagePaths.removeAt(index)
                actualizarGaleria()
            }

            imageView.setOnClickListener {
                val intent = Intent(this, VerImagenActivity::class.java)
                intent.putExtra("imagePath", imagePaths[index])
                verImagenLauncher.launch(intent)
            }

            binding.imageContainer.addView(itemView)
        }

        // Mostrar video
        videoPath?.let { path ->
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_video, binding.imageContainer, false)
            val thumbnail: ImageView = itemView.findViewById(R.id.videoThumbnail)
            val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminarVideo)

            val thumb = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MINI_KIND)
            thumbnail.setImageBitmap(thumb)

            thumbnail.setOnClickListener {
                val intent = Intent(this, VerVideoActivity::class.java)
                intent.putExtra("videoPath", path)
                startActivity(intent)
            }

            btnEliminar.setOnClickListener {
                videoPath = null
                actualizarGaleria()
            }

            binding.imageContainer.addView(itemView)
        }

        // Mostrar audios
        for ((index, audioPath) in audioPaths.withIndex()) {
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_audio, binding.audioContainer, false)
            val btnPlay = itemView.findViewById<ImageButton>(R.id.btnPlayAudio)
            val btnEliminar = itemView.findViewById<ImageButton>(R.id.btnDeleteAudio)
            val seekBar = itemView.findViewById<SeekBar>(R.id.audioSeekBar)
            val tvDuracion = itemView.findViewById<TextView>(R.id.tvAudioDuration)

            val playerTemp = MediaPlayer()
            playerTemp.setDataSource(audioPath)
            playerTemp.prepare()
            val duracionMs = playerTemp.duration
            val duracionSeg = duracionMs / 1000
            tvDuracion.text = String.format("%02d:%02d", duracionSeg / 60, duracionSeg % 60)
            playerTemp.release()

            var currentPlayer: MediaPlayer? = null
            var isSeeking = false

            btnPlay.setOnClickListener {
                if (currentPlayer == null) {
                    currentPlayer = MediaPlayer().apply {
                        setDataSource(audioPath)
                        prepare()
                        start()
                        setOnCompletionListener {
                            btnPlay.setImageResource(android.R.drawable.ic_media_play)
                            currentPlayer?.release()
                            currentPlayer = null
                            btnPlay.setImageResource(android.R.drawable.ic_media_play)
                            currentPlayer?.release()
                            currentPlayer = null
                        }
                    }
                    btnPlay.setImageResource(android.R.drawable.ic_media_pause)
                    seekBar.max = currentPlayer!!.duration

                    val updateSeek = object : Runnable {
                        override fun run() {
                            if (!isSeeking) {
                                seekBar.progress = currentPlayer?.currentPosition ?: 0
                            }
                            if (currentPlayer?.isPlaying == true) handler.postDelayed(this, 500)
                        }
                    }
                    handler.post(updateSeek)

                    seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            if (fromUser) currentPlayer?.seekTo(progress)
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {
                            isSeeking = true
                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            isSeeking = false
                        }
                    })
                } else {
                    currentPlayer?.stop()
                    currentPlayer?.release()
                    currentPlayer = null
                    btnPlay.setImageResource(android.R.drawable.ic_media_play)
                }
            }

            btnEliminar.setOnClickListener {
                currentPlayer?.release()
                currentPlayer = null
                audioPaths.removeAt(index)
                actualizarGaleria()
            }

            binding.audioContainer.addView(itemView)
        }

        // Mostrar PDFs
        for ((index, pdfPath) in pdfPaths.withIndex()) {
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_pdf, binding.pdfContainer, false)
            val tvPdfName = itemView.findViewById<TextView>(R.id.tvPdfName)
            val btnEliminar = itemView.findViewById<ImageButton>(R.id.btnEliminarPdf)
            val ivIcon = itemView.findViewById<ImageView>(R.id.pdfIcon)

            val file = File(pdfPath)
            tvPdfName.text = file.name
            ivIcon.setImageResource(android.R.drawable.ic_menu_save)

            itemView.setOnClickListener {
                val intent = Intent(this, PdfViewerActivity::class.java)
                intent.putExtra("pdfPath", pdfPath)
                startActivity(intent)
            }

            btnEliminar.setOnClickListener {
                pdfPaths.removeAt(index)
                actualizarGaleria()
            }

            binding.pdfContainer.addView(itemView)
        }
    }

    private val verImagenLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            for ((index, path) in imagePaths.withIndex()) {
                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap != null) {
                    imagesList[index] = bitmap
                }
            }
            actualizarGaleria()
        }
    }

    private fun verificarYSolicitarPermisos() {
        val permisosNecesarios = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            permisosNecesarios.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            permisosNecesarios.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val permisosNoConcedidos = permisosNecesarios.filter {
            ActivityCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (permisosNoConcedidos.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permisosNoConcedidos.toTypedArray(), 100)
        }
    }
}
