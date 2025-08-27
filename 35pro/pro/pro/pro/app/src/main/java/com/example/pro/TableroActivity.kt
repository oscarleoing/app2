package com.example.pro

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class TableroActivity : AppCompatActivity() {

    private lateinit var tvClasificatoriaTitulo: TextView
    private lateinit var scrollClasificatoria: View

    // Subcategorías de Alumbrado
    private lateinit var subButtons: List<LinearLayout>

    // Lista de botones superiores
    private lateinit var topButtons: List<LinearLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tablero)

        // Referencias de la sección Clasificatoria
        tvClasificatoriaTitulo = findViewById(R.id.tvClasificatoriaTitulo)
        scrollClasificatoria = findViewById(R.id.scrollClasificatoria)

        // Sub-botones de Clasificatoria
        subButtons = listOf(
            findViewById(R.id.btnLuminarias),
            findViewById(R.id.btnMejoras),
            findViewById(R.id.btnEspaciosComunes),
            findViewById(R.id.btnApagon),
            findViewById(R.id.btnArbotantes),
            findViewById(R.id.btnPostes)
        )

        // Botones de la fila superior
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

        ocultarClasificatoria()
        setTopButtonClickListeners()
        setClasificatoriaClickListeners()
    }

    private fun setTopButtonClickListeners() {
        for (btn in topButtons) {
            btn.setOnClickListener {
                if (it.id == R.id.btnAlumbrado) {
                    mostrarClasificatoria()
                } else {
                    ocultarClasificatoria()
                }
                // Resaltar solo el botón seleccionado
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
            }
        }
    }

    private fun highlightSubButton(selected: LinearLayout) {
        for (btn in subButtons) {
            if (btn == selected) {
                // Activo -> alpha completo
                btn.alpha = 1f
            } else {
                // Inactivo -> alpha medio
                btn.alpha = 0.5f
            }
        }
    }

    private fun mostrarClasificatoria() {
        tvClasificatoriaTitulo.visibility = View.VISIBLE
        scrollClasificatoria.visibility = View.VISIBLE
    }

    private fun ocultarClasificatoria() {
        tvClasificatoriaTitulo.visibility = View.GONE
        scrollClasificatoria.visibility = View.GONE

        // Reset alpha de sub-botones cuando se oculta
        for (btn in subButtons) {
            btn.alpha = 1f
        }
    }
}
