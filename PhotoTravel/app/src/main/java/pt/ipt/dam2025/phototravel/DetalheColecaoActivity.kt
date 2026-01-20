package pt.ipt.dam2025.phototravel

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.dam2025.phototravel.adaptadores.FotosAdapter
import pt.ipt.dam2025.phototravel.viewmodel.PartilhaDadosViewModel

class DetalheColecaoActivity : AppCompatActivity() {

    private val viewModel: PartilhaDadosViewModel by viewModels()
    private lateinit var adapter: FotosAdapter
    private var tituloColecao: String? = null

    override fun onResume() {
        super.onResume()
        viewModel.recarregarDados()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhe_colecao)

        tituloColecao = intent.getStringExtra("TITULO_COLECAO")
        if (tituloColecao == null) {
            finish()
            return
        }

        val textViewNomeColecao = findViewById<TextView>(R.id.txtTituloAlbum)
        // Usa o nome personalizado se existir, senão usa o título (ID)
        textViewNomeColecao.text = intent.getStringExtra("NOME_COLECAO") ?: tituloColecao

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerFotosAlbum)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        adapter = FotosAdapter(emptyList()) { fotoClicada ->
            val intent = Intent(this, VerFotoActivity::class.java)
            intent.putExtra("URI_DA_FOTO", fotoClicada.uriString)
            // IMPORTANTE: Passar o título da coleção para saber de onde apagar
            intent.putExtra("TITULO_COLECAO", fotoClicada.data)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // Observar as coleções
        viewModel.listaColecoes.observe(this, Observer { colecoes ->
            val colecaoAtualizada = colecoes.find { it.titulo == tituloColecao }

            if (colecaoAtualizada != null) {
                // Criamos uma cópia da lista para garantir que o Adapter deteta a mudança
                adapter.atualizarFotos(colecaoAtualizada.listaFotos.toList())
            } else {
                finish() // Se a coleção deixou de existir, sai do ecrã
            }
        })
    }


}