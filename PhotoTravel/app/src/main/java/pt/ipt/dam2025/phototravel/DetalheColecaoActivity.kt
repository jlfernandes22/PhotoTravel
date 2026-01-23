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

/**
 * <summary>
 *  conteúdo detalhado de uma coleção específica.
 * </summary>
 */
class DetalheColecaoActivity : AppCompatActivity() {

    private val viewModel: PartilhaDadosViewModel by viewModels()
    private lateinit var adapter: FotosAdapter
    private var tituloColecao: String? = null

    /**
     * <summary>
     * Sincroniza os dados com o armazenamento sempre que a atividade volta ao foco.
     * </summary>
     */
    override fun onResume() {
        super.onResume()
        viewModel.recarregarDados()
    }

    /**
     * <summary>
     * Inicializa a interface, recupera os dados da Intent e configura o FotosAdapter.
     * Define ações para clique curto (visualizar) e clique longo (mover foto).
     * </summary>
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhe_colecao)

        // <summary> Validação do parâmetro de entrada (ID da coleção) </summary>
        tituloColecao = intent.getStringExtra("TITULO_COLECAO")
        if (tituloColecao == null) {
            finish()
            return
        }

        val textViewNomeColecao = findViewById<TextView>(R.id.txtTituloAlbum)
        textViewNomeColecao.text = intent.getStringExtra("NOME_COLECAO") ?: tituloColecao

        //  Configura RecyclerView com grelha de 3 colunas
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerFotosAlbum)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        adapter = FotosAdapter(
            emptyList(),
            onItemClick = { fotoClicada ->
                // Abre a foto selecionada em ecrã inteiro
                val intent = Intent(this, VerFotoActivity::class.java)
                intent.putExtra("URI_DA_FOTO", fotoClicada.uriString)
                intent.putExtra("TITULO_COLECAO", fotoClicada.data)
                startActivity(intent)
            },
            onItemLongClick = { fotoParaMover ->
                //diálogo para mover a foto para outra coleção
                mostrarDialogoMoverFoto(fotoParaMover)
            }
        )
        recyclerView.adapter = adapter


        // Observa mudanças nas coleções e atualiza a lista de fotos filtrada.
        // Se a coleção atual for apagada, a atividade encerra-se automaticamente.

        viewModel.listaColecoes.observe(this, Observer { colecoes ->
            val colecaoAtualizada = colecoes.find { it.titulo == tituloColecao }

            if (colecaoAtualizada != null) {
                adapter.atualizarFotos(colecaoAtualizada.listaFotos.toList())
            } else {
                finish()
            }
        })
    }

    /**
     * <summary>
     * Exibe um AlertDialog com um Spinner (dropdown) contendo as outras coleções disponíveis.
     * Permite ao utilizador transferir a foto selecionada para um destino diferente.
     * </summary>
     * <param name="fotoParaMover">O objeto FotoDados que será reatribuído a outra coleção</param>
     */
    private fun mostrarDialogoMoverFoto(fotoParaMover: FotoDados) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Organizar Foto")

        val view = layoutInflater.inflate(R.layout.dialog_mover_foto, null)
        val spinner = view.findViewById<Spinner>(R.id.spinnerColecoes)

        // Filtra as coleções para não mostrar a coleção atual como destino
        val todasColecoes = viewModel.listaColecoes.value ?: emptyList()
        val colecoesDestino = todasColecoes.filter { it.titulo != tituloColecao }
        val nomes = colecoesDestino.map { it.nomePersonalizado ?: it.titulo }

        //  Configura o adapter visual para o Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nomes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        builder.setView(view)

        builder.setPositiveButton("MOVER") { _, _ ->
            val posicao = spinner.selectedItemPosition
            if (posicao != AdapterView.INVALID_POSITION && colecoesDestino.isNotEmpty()) {
                val destino = colecoesDestino[posicao]
                // <summary> Comunica a mudança ao ViewModel para persistência </summary>
                viewModel.moverFotoParaColecao(fotoParaMover, destino)
                Toast.makeText(this, "Foto movida!", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("CANCELAR", null)
        builder.show()
    }
}