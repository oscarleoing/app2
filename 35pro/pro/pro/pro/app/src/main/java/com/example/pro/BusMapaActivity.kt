package com.example.pro

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import java.util.*

class BusMapaActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private var selectedLatLng: LatLng? = null
    private var marker: Marker? = null

    private lateinit var btnUbicacion: ImageButton
    private lateinit var directionBar: TextView
    private lateinit var btnMostrarMapa: ImageButton
    private lateinit var layoutContenido: LinearLayout
    private lateinit var containerFormulario: FrameLayout
    private lateinit var btnEnviar: Button

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Categorías principales
    private lateinit var topButtons: List<LinearLayout>

    // Subcategorías (HorizontalScrollView)
    private lateinit var scrollsSubcategorias: List<HorizontalScrollView>

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
        private const val TAG = "BusMapaActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bus_mapa)

        try {
            if (!Places.isInitialized()) {
                Places.initialize(applicationContext, "TU_API_KEY_GOOGLE_MAPS")
            }

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            // Inicializar vistas
            btnUbicacion = findViewById(R.id.btnUbicacion)
            directionBar = findViewById(R.id.tvDireccion)
            btnMostrarMapa = findViewById(R.id.btnMostrarMapa)
            layoutContenido = findViewById(R.id.layoutContenido)
            containerFormulario = findViewById(R.id.containerFormulario)
            btnEnviar = findViewById(R.id.btnEnviar)

            btnMostrarMapa.visibility = View.GONE

            // Inicializar categorías principales
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

            // Inicializar subcategorías (HorizontalScrollView)
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

            // Aseguramos que todas las subcategorías estén ocultas al inicio
            scrollsSubcategorias.forEach { it.visibility = View.GONE }

            // Configurar clics de categorías
            setTopButtonClickListeners()

            // Inicializar mapa
            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)

            btnEnviar.setOnClickListener {
                if (selectedLatLng != null) {
                    ocultarMapaYBotones()
                } else {
                    Toast.makeText(this, "Selecciona una ubicación en el mapa", Toast.LENGTH_SHORT).show()
                }
            }

            btnUbicacion.setOnClickListener { moveToCurrentLocation() }

        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /** -------------------- CATEGORÍAS & SUBCATEGORÍAS -------------------- **/

    private fun setTopButtonClickListeners() {
        for ((index, btn) in topButtons.withIndex()) {
            btn.setOnClickListener {
                highlightTopButton(btn)
                // Mostrar la HorizontalScrollView correspondiente
                scrollsSubcategorias.forEachIndexed { i, scroll ->
                    scroll.visibility = if (i == index) View.VISIBLE else View.GONE
                }
                // Resetear alfa de subcategorías cuando se cambia de categoría
                resetSubcategoriasAlpha(index)
                setSubcategoriaClickListeners(index)
            }
        }
    }

    private fun highlightTopButton(selected: LinearLayout) {
        for (btn in topButtons) btn.alpha = if (btn == selected) 1f else 0.5f
    }

    /** Resetea el alpha de todos los botones de subcategoría de la categoría seleccionada */
    private fun resetSubcategoriasAlpha(index: Int) {
        val scroll = scrollsSubcategorias[index]
        val layout = scroll.getChildAt(0) as LinearLayout
        for (i in 0 until layout.childCount) {
            layout.getChildAt(i).alpha = 1f
        }
    }

    /** Configura los clics de subcategorías para resaltar la seleccionada */
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

    /** -------------------- MAPA -------------------- **/

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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun moveToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                if (marker == null) {
                    marker = map.addMarker(MarkerOptions().position(currentLatLng).title("Mi ubicación"))
                } else marker?.position = currentLatLng

                selectedLatLng = currentLatLng
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
                updateDireccionBar(currentLatLng)
            } ?: run {
                Toast.makeText(this, "No se pudo obtener ubicación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateDireccionBar(latLng: LatLng) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            directionBar.text = if (!addresses.isNullOrEmpty()) addresses[0].getAddressLine(0) else "Dirección no disponible"
        } catch (e: Exception) {
            directionBar.text = "Error obteniendo dirección"
        }
    }

    /** -------------------- ANIMACIONES: OCULTAR / MOSTRAR -------------------- **/
    private fun ocultarMapaYBotones() {
        val interpolator = AccelerateDecelerateInterpolator()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map)
        mapFragment?.view?.let { mapView ->
            mapView.animate()
                .alpha(0f)
                .translationY(50f)
                .setInterpolator(interpolator)
                .setDuration(600)
                .withEndAction {
                    mapView.visibility = View.GONE
                    // 👇 Mostrar directamente el formulario ya existente
                    layoutContenido.alpha = 0f
                    layoutContenido.visibility = View.VISIBLE
                    findViewById<View>(R.id.layoutFormulario).visibility = View.VISIBLE
                    layoutContenido.animate().alpha(1f).setDuration(400).start()
                }
                .start()
        }

        val autocomplete = findViewById<FrameLayout>(R.id.autocompleteFragment)
        autocomplete?.let { autoView ->
            autoView.animate()
                .alpha(0f)
                .translationY(50f)
                .setInterpolator(interpolator)
                .setDuration(600)
                .withEndAction { autoView.visibility = View.GONE }
                .start()
        }

        btnUbicacion.visibility = View.GONE
        btnMostrarMapa.visibility = View.VISIBLE
        btnEnviar.visibility = View.GONE
    }

    private fun mostrarMapaYBotones() {
        val interpolator = AccelerateDecelerateInterpolator()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map)
        mapFragment?.view?.let { mapView ->
            mapView.visibility = View.VISIBLE
            mapView.alpha = 0f
            mapView.translationY = 50f
            mapView.animate()
                .translationY(0f)
                .alpha(1f)
                .setInterpolator(interpolator)
                .setDuration(600)
                .start()
        }

        val autocomplete = findViewById<FrameLayout>(R.id.autocompleteFragment)
        autocomplete?.let { autoView ->
            autoView.visibility = View.VISIBLE
            autoView.alpha = 0f
            autoView.translationY = 50f
            autoView.animate()
                .translationY(0f)
                .alpha(1f)
                .setInterpolator(interpolator)
                .setDuration(600)
                .start()
        }

        btnUbicacion.visibility = View.VISIBLE
        btnMostrarMapa.visibility = View.GONE
        btnEnviar.visibility = View.VISIBLE

        layoutContenido.animate()
            .alpha(0f)
            .setDuration(400)
            .withEndAction {
                layoutContenido.visibility = View.GONE
                // 👇 Oculpa el formulario otra vez
                findViewById<View>(R.id.layoutFormulario).visibility = View.GONE
            }
            .start()
    }

}
