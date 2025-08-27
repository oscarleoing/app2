package com.example.pro

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pro.databinding.ActivityVerImagenBinding
import java.io.File

class VerImagenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerImagenBinding
    private var currentRotation = 0f
    private var imageFile: File? = null
    private var originalBitmap: Bitmap? = null
    private var rotatedBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerImagenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val path = intent.getStringExtra("imagePath")
        Log.d("VerImagenActivity", "Ruta imagen recibida: $path")

        if (path == null) {
            Toast.makeText(this, "Ruta de imagen no disponible", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        imageFile = File(path)
        Log.d("VerImagenActivity", "Archivo existe: ${imageFile?.exists()}")
        if (imageFile == null || !imageFile!!.exists()) {
            Toast.makeText(this, "Archivo de imagen no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        originalBitmap = BitmapFactory.decodeFile(path)
        if (originalBitmap == null) {
            Toast.makeText(this, "No se pudo decodificar la imagen", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        rotatedBitmap = originalBitmap
        binding.imageViewVer.setImageBitmap(rotatedBitmap)

        binding.btnCerrarImagen.setOnClickListener {
            finish()
        }

        binding.btnRotarImagen.setOnClickListener {
            currentRotation += 90f
            currentRotation %= 360f
            rotatedBitmap = rotateBitmap(originalBitmap!!, currentRotation)
            binding.imageViewVer.setImageBitmap(rotatedBitmap)
        }

        binding.btnGuardarImagen.setOnClickListener {
            rotatedBitmap?.let {
                saveBitmapToFile(it, imageFile!!)
                Toast.makeText(this, "Imagen guardada con rotación", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
        }
    }
}
