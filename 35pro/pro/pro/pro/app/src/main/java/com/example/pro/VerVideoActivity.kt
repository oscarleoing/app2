package com.example.pro

import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pro.databinding.ActivityVerVideoBinding
import java.io.File

class VerVideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerVideoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val path = intent.getStringExtra("videoPath")
        if (path == null) {
            Toast.makeText(this, "Ruta de video no disponible", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val videoFile = File(path)
        if (!videoFile.exists()) {
            Toast.makeText(this, "Archivo de video no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val videoUri = Uri.fromFile(videoFile)
        binding.videoView.setVideoURI(videoUri)

        val mediaController = MediaController(this)
        mediaController.setAnchorView(binding.videoView)
        binding.videoView.setMediaController(mediaController)

        // El video empieza automáticamente
        binding.videoView.start()

        binding.btnCerrarVideo.setOnClickListener {
            finish()
        }
    }
}
