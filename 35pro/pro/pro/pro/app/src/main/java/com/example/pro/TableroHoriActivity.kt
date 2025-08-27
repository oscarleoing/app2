


package com.example.pro

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class TableroHoriActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tablero)

        setButtonClickListeners()
    }

    private fun setButtonClickListeners() {
        findViewById<LinearLayout>(R.id.btnAlumbrado).setOnClickListener {
            openTramite("ALUMBRADO")
        }
        findViewById<LinearLayout>(R.id.btnCalles).setOnClickListener {
            openTramite("CALLES")
        }
        findViewById<LinearLayout>(R.id.btnMIAA).setOnClickListener {
            openTramite("MIAA")
        }
        findViewById<LinearLayout>(R.id.btnApoyos).setOnClickListener {
            openTramite("APOYOS")
        }
        findViewById<LinearLayout>(R.id.btnParques).setOnClickListener {
            openTramite("PARQUES Y JARDINES")
        }
        findViewById<LinearLayout>(R.id.btnCiudad).setOnClickListener {
            openTramite("CIUDAD")
        }
        findViewById<LinearLayout>(R.id.btnConvivencia).setOnClickListener {
            openTramite("CONVIVENCIA")
        }
        findViewById<LinearLayout>(R.id.btnVigilancia).setOnClickListener {
            openTramite("VIGILANCIA")
        }
        findViewById<LinearLayout>(R.id.btnLimpia).setOnClickListener {
            openTramite("LIMPIA")
        }
        findViewById<LinearLayout>(R.id.btnOtros).setOnClickListener {
            openTramite("OTROS")
        }
    }

    private fun openTramite(tipo: String) {
        val intent = Intent(this, TramiteActivity::class.java)
        intent.putExtra("tipo_tramite", tipo)
        startActivity(intent)
    }
}
