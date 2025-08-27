package com.example.pro

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MapaActivity : AppCompatActivity() {

    private lateinit var scrollClasificatoria: View
    private lateinit var formulario: View

    // Subcategorías de Alumbrado
    private lateinit var subButtons: List<LinearLayout>

    // Botones de la fila superior (categorías principales)
    private lateinit var topButtons: List<LinearLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mapa)

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

    /** Resalta solo el botón superior seleccionado */
    private fun highlightTopButton(selected: LinearLayout) {
        for (btn in topButtons) {
            btn.alpha = if (btn == selected) 1f else 0.5f
        }
    }

    /** Configura los clics de botones de subcategorías (nivel 2) */
    private fun setClasificatoriaClickListeners() {
        for (btn in subButtons) {
            btn.setOnClickListener {
                highlightSubButton(btn)
                activarFormulario() // Activar formulario solo al seleccionar subcategoría
            }
        }
    }

    /** Resalta solo el sub-botón seleccionado */
    private fun highlightSubButton(selected: LinearLayout) {
        for (btn in subButtons) {
            btn.alpha = if (btn == selected) 1f else 0.5f
        }
    }

    /** Mostrar scroll de subcategorías */
    private fun mostrarClasificatoria() {
        scrollClasificatoria.visibility = View.VISIBLE
    }

    /** Ocultar scroll de subcategorías y resetear alpha */
    private fun ocultarClasificatoria() {
        scrollClasificatoria.visibility = View.GONE
        for (btn in subButtons) {
            btn.alpha = 1f
        }
    }

    /** Activar formulario de reporte */
    private fun activarFormulario() {
        formulario.alpha = 1f
        formulario.isEnabled = true
    }

    /** Desactivar formulario de reporte */
    private fun desactivarFormulario() {
        formulario.alpha = 0.5f
        formulario.isEnabled = false
    }
}
