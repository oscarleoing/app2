package com.example.pro

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.pro.databinding.ActivityMainBinding

class  MainActivity: AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ir al trámite
//binding.btnIrATramite.setOnClickListener {
          //  val intent = Intent(this, TramiteActivity::class.java)
      //      startActivity(intent)
      //  }

        // Ir al tablero
        binding.btnIrATramite.setOnClickListener {
            val intent = Intent(this, MainActivity8::class.java)
            startActivity(intent)
        }

        // Ir al tablero
     //   binding.btnIrAMapa.setOnClickListener {
    //        val intent = Intent(this, FormuMapaActivity::class.java)
     //       startActivity(intent)
     //   }


        // Cerrar sesión
        binding.btnCerrarSesion.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
