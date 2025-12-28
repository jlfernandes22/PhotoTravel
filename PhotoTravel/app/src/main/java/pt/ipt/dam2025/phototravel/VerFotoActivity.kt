package pt.ipt.dam2025.phototravel

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class VerFotoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ver_foto)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Receber a URI da foto que veio do clique
        val uriString = intent.getStringExtra("URI_DA_FOTO")

        if (uriString != null) {
            val imageView = findViewById<ImageView>(R.id.foto_em_ecra_inteiro)
            val uri = Uri.parse(uriString)
            imageView.setImageURI(uri)
        }
    }
}
