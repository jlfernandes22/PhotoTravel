package pt.ipt.dam2025.phototravel

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.dam2025.phototravel.adaptadores.FotosAdapter
import pt.ipt.dam2025.phototravel.modelos.FotoDados

class DetalheColecaoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalhe_colecao)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detalhes)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Receber o nome e a lista de fotos do Intent
        val nomeDaColecao = intent.getStringExtra("NOME_COLECAO") ?: "Detalhes"
        val fotosDoAlbum: ArrayList<FotoDados>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("LISTA_FOTOS", FotoDados::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra("LISTA_FOTOS")
        }

        findViewById<TextView>(R.id.txtTituloAlbum).text = nomeDaColecao

        // A sua referência ao RecyclerView está na variável 'recycler'
        val recycler = findViewById<RecyclerView>(R.id.recyclerFotosAlbum)
        recycler.layoutManager = GridLayoutManager(this, 3)

        // 2. Se a lista de fotos não for nula, configura o adapter
        if (fotosDoAlbum != null) {
            // ✅ CORREÇÃO 1: Usar 'fotosDoAlbum' em vez de 'minhaListaDeFotos'
            // ✅ CORREÇÃO 3: Remover o ')' extra no final da linha
            val adapter = FotosAdapter(fotosDoAlbum) { fotoClicada ->
                // ESTE BLOCO DE CÓDIGO É EXECUTADO QUANDO UMA FOTO É CLICADA
                //Toast.makeText(this, "Foto clicada: ${fotoClicada.uriString}", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, VerFotoActivity::class.java)
                intent.putExtra("URI_DA_FOTO", fotoClicada.uriString)
                startActivity(intent)
            }
            recycler.adapter = adapter
        }
    }
}
