package pt.ipt.dam2025.phototravel

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.Observer

class DetalheColecaoActivity : AppCompatActivity() {

    // Criamos uma nova instância do ViewModel para ler os dados do disco
    private val viewModel: PartilhaDadosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalhe_colecao)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detalhes)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Receber a DATA que veio do clique
        val dataRecebida = intent.getStringExtra("CHAVE_DATA") ?: ""

        findViewById<TextView>(R.id.txtTituloAlbum).text = "Fotos de $dataRecebida"

        val recycler = findViewById<RecyclerView>(R.id.recyclerFotosAlbum)
        recycler.layoutManager = GridLayoutManager(this, 3)

        // 2. Observar os dados e filtrar apenas as fotos desta data
        viewModel.listaFotos.observe(this, Observer { todasAsFotos ->
            if (todasAsFotos != null) {
                val fotosDesteAlbum = todasAsFotos.filter { it.data == dataRecebida }
                recycler.adapter = FotosAdapter(fotosDesteAlbum)
            }
        })
    }
}