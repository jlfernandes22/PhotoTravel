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
import android.app.AlertDialog
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Spinner
import android.widget.Toast
import pt.ipt.dam2025.phototravel.modelos.FotoDados
import pt.ipt.dam2025.phototravel.modelos.ColecaoDados


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

        // Dentro do onCreate da DetalheColecaoActivity
        adapter = FotosAdapter(
            emptyList(),
            onItemClick = { fotoClicada ->
                val intent = Intent(this, VerFotoActivity::class.java)
                intent.putExtra("URI_DA_FOTO", fotoClicada.uriString)
                intent.putExtra("TITULO_COLECAO", fotoClicada.data)
                startActivity(intent)
            },
            onItemLongClick = { fotoParaMover ->
                // ✅ Agora isto vai funcionar!
                mostrarDialogoMoverFoto(fotoParaMover)
            }
        )
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
    // Dentro da DetalheColecaoActivity.kt
    private fun mostrarDialogoMoverFoto(fotoParaMover: FotoDados) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Organizar Foto")

        val view = layoutInflater.inflate(R.layout.dialog_mover_foto, null)
        val spinner = view.findViewById<Spinner>(R.id.spinnerColecoes)

        // 1. Filtrar as coleções
        val todasColecoes = viewModel.listaColecoes.value ?: emptyList()
        val colecoesDestino = todasColecoes.filter { it.titulo != tituloColecao }
        val nomes = colecoesDestino.map { it.nomePersonalizado ?: it.titulo }

        // 2. Criar o Adapter com layout específico para Dropdown
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nomes)

        // Esta linha garante que, ao clicar, a lista apareça como um menu suspenso
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinner.adapter = adapter

        builder.setView(view)

        builder.setPositiveButton("MOVER") { _, _ ->
            val posicao = spinner.selectedItemPosition
            if (posicao != AdapterView.INVALID_POSITION && colecoesDestino.isNotEmpty()) {
                val destino = colecoesDestino[posicao]
                viewModel.moverFotoParaColecao(fotoParaMover, destino)
                Toast.makeText(this, "Foto movida!", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("CANCELAR", null)
        builder.show()
    }
}