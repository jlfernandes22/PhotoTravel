package pt.ipt.dam2025.phototravel

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import coil.load // Não se esqueça do import do Coil

class VerFotoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_foto)

        // 1. Receber o URI da foto que foi passado pelo Intent
        val uriString = intent.getStringExtra("URI_DA_FOTO")

        // 2. Encontrar a ImageView no layout
        val imageView = findViewById<ImageView>(R.id.imagem_ecra_cheio)

        // 3. Usar o Coil para carregar a imagem, se o URI não for nulo
        if (uriString != null) {
            val uri = Uri.parse(uriString)
            imageView.load(uri)
        }
    }
}
