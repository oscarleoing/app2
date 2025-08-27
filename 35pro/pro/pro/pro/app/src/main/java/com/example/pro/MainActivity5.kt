package com.example.pro

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
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

class MainActivity5 : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private var selectedLatLng: LatLng? = null
    private var marker: Marker? = null

    private lateinit var btnUbicacion: ImageButton
    private lateinit var btnMostrarMapa: Button
    private lateinit var btnEnviar: Button
    private lateinit var directionBar: TextView

    private lateinit var layoutContenido: LinearLayout
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
        private const val TAG = "MainActivity5"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main5)

        try {
            // Inicializar Places API
            if (!Places.isInitialized()) {
                Places.initialize(applicationContext, "TU_API_KEY_GOOGLE_MAPS")
            }

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            // Inicializar vistas
            btnUbicacion = findViewById(R.id.btnUbicacion)
            btnMostrarMapa = findViewById(R.id.btnMostrarMapa)
            btnEnviar = findViewById(R.id.btnEnviar)
            directionBar = findViewById(R.id.tvDireccion)

            layoutContenido = findViewById(R.id.layoutFormulario)

            btnMostrarMapa.visibility = View.GONE
            layoutContenido.visibility = View.GONE // formulario oculto al iniciar

            // Inicializar mapa
            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)

            // Botón enviar ubicación
            btnEnviar.setOnClickListener {
                if (selectedLatLng != null) {
                    ocultarMapaYBotones()
                } else {
                    Toast.makeText(this, "Selecciona una ubicación en el mapa", Toast.LENGTH_SHORT).show()
                }
            }

            btnUbicacion.setOnClickListener { moveToCurrentLocation() }
            btnMostrarMapa.setOnClickListener { mostrarMapaYBotones() }

        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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

    /** -------------------- TRANSICIONES -------------------- **/

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
                    layoutContenido.alpha = 0f
                    layoutContenido.visibility = View.VISIBLE
                    layoutContenido.animate().alpha(1f).setDuration(400).start()
                }
                .start()
        }

        val autocomplete = findViewById<FrameLayout>(R.id.layoutBuscador)
        autocomplete?.animate()
            ?.alpha(0f)
            ?.translationY(50f)
            ?.setInterpolator(interpolator)
            ?.setDuration(600)
            ?.withEndAction { autocomplete.visibility = View.GONE }
            ?.start()

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

        val autocomplete = findViewById<FrameLayout>(R.id.layoutBuscador)
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
            .withEndAction { layoutContenido.visibility = View.GONE }
            .start()
    }
}
