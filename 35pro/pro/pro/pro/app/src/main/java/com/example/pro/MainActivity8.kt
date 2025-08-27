package com.example.pro

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.ThumbnailUtils
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.pro.databinding.ActivityMain8Binding
import com.example.pro.databinding.LayoutModalGrabacionBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity8 : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMain8Binding
    private lateinit var grabacionBinding: LayoutModalGrabacionBinding

    // ----- MAPA -----
    private lateinit var map: GoogleMap
    private var selectedLatLng: LatLng? = null
    private var marker: Marker? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Categorías
    private lateinit var topButtons: List<LinearLayout>
    private lateinit var scrollsSubcategorias: List<HorizontalScrollView>

    // ----- MULTIMEDIA -----
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

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
        private const val TAG = "MainActivity8"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain8Binding.inflate(layoutInflater)
        setContentView(binding.root)

        grabacionBinding = LayoutModalGrabacionBinding.inflate(layoutInflater)
        grabacionBinding.modalBackground.setBackgroundColor(0x88000000.toInt())
        grabacionBinding.modalContent.setBackgroundResource(R.drawable.bg_modal)
        grabacionBinding.modalContent.elevation = 12f
        mostrarLayoutGrabandoAudio(false)

        try {
            if (!Places.isInitialized()) {
                Places.initialize(applicationContext, "TU_API_KEY_GOOGLE_MAPS")
            }
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            // Inicializar categorías
            topButtons = listOf(
                findViewById(R.id.btnAlumbrado),
                findViewById(R.id.btnCalles),
                findViewById(R.id.btnMIAA),
                findViewById(R.id.btnApoyos),
                findViewById(R.id.btnParques),
                findViewById(R.id.btnCiudad),
                findViewById(R.id.btnConvivencia),
                findViewById(R.id.btnVigilancia),
                findViewById(R.id.btnLimpia),
                findViewById(R.id.btnOtros)
            )

            scrollsSubcategorias = listOf(
                findViewById(R.id.scrollAlumbradoSubcategorias),
                findViewById(R.id.scrollCallesSubcategorias),
                findViewById(R.id.scrollMIAASubcategorias),
                findViewById(R.id.scrollApoyosSubcategorias),
                findViewById(R.id.scrollParquesSubcategorias),
                findViewById(R.id.scrollCiudadSubcategorias),
                findViewById(R.id.scrollConvivenciaSubcategorias),
                findViewById(R.id.scrollVigilanciaSubcategorias),
                findViewById(R.id.scrollLimpiaSubcategorias)
            )

            scrollsSubcategorias.forEach { it.visibility = View.GONE }
            setTopButtonClickListeners()

            // Inicializar mapa
            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)

            binding.btnEnviar.setOnClickListener {
                if (selectedLatLng != null) {
                    ocultarMapaYBotones()
                } else {
                    Toast.makeText(this, "Selecciona una ubicación en el mapa", Toast.LENGTH_SHORT).show()
                }
            }
            binding.btnUbicacion.setOnClickListener { moveToCurrentLocation() }
            binding.btnMostrarMapa.setOnClickListener { mostrarMapaYBotones() }

            // ---- Multimedia ----
            binding.layoutFoto.setOnClickListener { mostrarDialogoSeleccion() }
            grabacionBinding.btnStartRecording.setOnClickListener {
                if (isRecording) detenerGrabacion() else pedirPermisoYGrabar()
            }
            grabacionBinding.btnCancelarGrabacion.setOnClickListener { cancelarGrabacion() }
            grabacionBinding.modalBackground.setOnClickListener {
                if (!isRecording) mostrarLayoutGrabandoAudio(false)
            }
            binding.btnSubmit.setOnClickListener {
                Toast.makeText(this, "Reporte enviado ✅", Toast.LENGTH_SHORT).show()
                finish()
            }

            verificarYSolicitarPermisos()
            actualizarGaleria()

        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ---------------- CATEGORÍAS ----------------
    private fun setTopButtonClickListeners() {
        for ((index, btn) in topButtons.withIndex()) {
            btn.setOnClickListener {
                highlightTopButton(btn)
                scrollsSubcategorias.forEachIndexed { i, scroll ->
                    scroll.visibility = if (i == index) View.VISIBLE else View.GONE
                }
                resetSubcategoriasAlpha(index)
                setSubcategoriaClickListeners(index)
            }
        }
    }

    private fun highlightTopButton(selected: LinearLayout) {
        for (btn in topButtons) btn.alpha = if (btn == selected) 1f else 0.5f
    }

    private fun resetSubcategoriasAlpha(index: Int) {
        val scroll = scrollsSubcategorias[index]
        val layout = scroll.getChildAt(0) as LinearLayout
        for (i in 0 until layout.childCount) layout.getChildAt(i).alpha = 1f
    }

    private fun setSubcategoriaClickListeners(index: Int) {
        val scroll = scrollsSubcategorias[index]
        val layout = scroll.getChildAt(0) as LinearLayout
        for (i in 0 until layout.childCount) {
            layout.getChildAt(i).setOnClickListener { selected ->
                for (j in 0 until layout.childCount) {
                    layout.getChildAt(j).alpha = if (layout.getChildAt(j) == selected) 1f else 0.5f
                }
            }
        }
    }

    // ---------------- MAPA ----------------
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        val initialPosition = LatLng(21.884635798282755, -102.28301879306879)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPosition, 13f))
        map.setOnMapClickListener { latLng ->
            map.clear()
            marker = map.addMarker(MarkerOptions().position(latLng).title("Ubicación seleccionada"))
            selectedLatLng = latLng
            updateDireccionBar(latLng)
        }
        enableMyLocation()
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun moveToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                if (marker == null) marker = map.addMarker(MarkerOptions().position(currentLatLng).title("Mi ubicación"))
                else marker?.position = currentLatLng
                selectedLatLng = currentLatLng
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
                updateDireccionBar(currentLatLng)
            } ?: run { Toast.makeText(this, "No se pudo obtener ubicación", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun updateDireccionBar(latLng: LatLng) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            binding.tvDireccion.text = if (!addresses.isNullOrEmpty()) addresses[0].getAddressLine(0) else "Dirección no disponible"
        } catch (e: Exception) {
            binding.tvDireccion.text = "Error obteniendo dirección"
        }
    }

    // ---------------- ANIMACIONES ----------------
    private fun ocultarMapaYBotones() {
        val interpolator = AccelerateDecelerateInterpolator()
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map)
        mapFragment?.view?.let { mapView ->
            mapView.animate().alpha(0f).translationY(50f).setInterpolator(interpolator).setDuration(600)
                .withEndAction {
                    mapView.visibility = View.GONE
                    binding.layoutPantalla2.alpha = 0f
                    binding.layoutPantalla2.visibility = View.VISIBLE
                    binding.layoutPantalla2.animate().alpha(1f).setDuration(400).start()
                }.start()
        }
        val autocomplete = findViewById<FrameLayout>(R.id.autocompleteFragment)
        autocomplete?.let { autoView ->
            autoView.animate().alpha(0f).translationY(50f).setInterpolator(interpolator).setDuration(600)
                .withEndAction { autoView.visibility = View.GONE }.start()
        }
        binding.btnUbicacion.visibility = View.GONE
        binding.btnMostrarMapa.visibility = View.VISIBLE
        binding.btnEnviar.visibility = View.GONE
    }

    private fun mostrarMapaYBotones() {
        val interpolator = AccelerateDecelerateInterpolator()
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map)
        mapFragment?.view?.let { mapView ->
            mapView.visibility = View.VISIBLE
            mapView.alpha = 0f
            mapView.translationY = 50f
            mapView.animate().translationY(0f).alpha(1f).setInterpolator(interpolator).setDuration(600).start()
        }
        val autocomplete = findViewById<FrameLayout>(R.id.autocompleteFragment)
        autocomplete?.let { autoView ->
            autoView.visibility = View.VISIBLE
            autoView.alpha = 0f
            autoView.translationY = 50f
            autoView.animate().translationY(0f).alpha(1f).setInterpolator(interpolator).setDuration(600).start()
        }
        binding.btnUbicacion.visibility = View.VISIBLE
        binding.btnMostrarMapa.visibility = View.GONE
        binding.btnEnviar.visibility = View.VISIBLE
        binding.layoutPantalla2.animate().alpha(0f).setDuration(400).withEndAction { binding.layoutPantalla2.visibility = View.GONE }.start()
    }

    // ---------------- MULTIMEDIA ----------------

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