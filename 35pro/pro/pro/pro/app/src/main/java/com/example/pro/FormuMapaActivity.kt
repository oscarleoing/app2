package com.example.pro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class FormuMapaActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var scrollClasificatoria: View
    private lateinit var formulario: LinearLayout

    private lateinit var subButtons: List<LinearLayout>
    private lateinit var topButtons: List<LinearLayout>

    // Google Maps
    private lateinit var map: GoogleMap
    private var selectedLatLng: LatLng? = null

    // Permiso de ubicación
    private val LOCATION_PERMISSION_REQUEST_CODE = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_formu_mapa)

        // Referencias a vistas
        scrollClasificatoria = findViewById(R.id.scrollClasificatoria)
        formulario = findViewById(R.id.formulario)

        // Sub-botones de Clasificatoria (nivel 2)
        subButtons = listOf(
            findViewById(R.id.btnLuminarias),
            findViewById(R.id.btnMejoras),
            findViewById(R.id.btnEspaciosComunes),
            findViewById(R.id.btnApagon),
            findViewById(R.id.btnArbotantes),
            findViewById(R.id.btnPostes)
        )

        // Botones de la fila superior (nivel 1)
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

        // Estado inicial
        ocultarClasificatoria()
        desactivarFormulario()

        // Configurar listeners
        setTopButtonClickListeners()
        setClasificatoriaClickListeners()

        // Configurar Google Maps
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /** Configura los clics de botones de nivel 1 */
    private fun setTopButtonClickListeners() {
        for (btn in topButtons) {
            btn.setOnClickListener {
                if (it.id == R.id.btnAlumbrado) {
                    mostrarClasificatoria()
                } else {
                    ocultarClasificatoria()
                    desactivarFormulario()
                }
                highlightTopButton(it as LinearLayout)
            }
        }
    }

    private fun highlightTopButton(selected: LinearLayout) {
        for (btn in topButtons) {
            btn.alpha = if (btn == selected) 1f else 0.5f
        }
    }

    private fun setClasificatoriaClickListeners() {
        for (btn in subButtons) {
            btn.setOnClickListener {
                highlightSubButton(btn)
                activarFormulario()
            }
        }
    }

    private fun highlightSubButton(selected: LinearLayout) {
        for (btn in subButtons) {
            btn.alpha = if (btn == selected) 1f else 0.5f
        }
    }

    private fun mostrarClasificatoria() {
        scrollClasificatoria.visibility = View.VISIBLE
    }

    private fun ocultarClasificatoria() {
        scrollClasificatoria.visibility = View.GONE
        for (btn in subButtons) {
            btn.alpha = 1f
        }
    }

    private fun activarFormulario() {
        formulario.alpha = 1f
        formulario.isEnabled = true
    }

    private fun desactivarFormulario() {
        formulario.alpha = 0.5f
        formulario.isEnabled = false
    }

    /** Google Maps callback */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Posición inicial
        val initialPosition = LatLng(21.884635798282755, -102.28301879306879)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPosition, 13f))

        // Click para marcar punto
        map.setOnMapClickListener { latLng ->
            map.clear()
            map.addMarker(MarkerOptions().position(latLng).title("Ubicación seleccionada"))
            selectedLatLng = latLng
        }

        // Activar mi ubicación si permisos concedidos
        enableMyLocation()
    }

    /** Verifica y solicita permisos de ubicación */
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableMyLocation()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
