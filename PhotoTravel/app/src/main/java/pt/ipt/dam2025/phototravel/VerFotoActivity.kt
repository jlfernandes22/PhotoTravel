package pt.ipt.dam2025.phototravel

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.observe
import coil.load
import com.google.android.material.appbar.MaterialToolbar
import pt.ipt.dam2025.phototravel.modelos.ColecaoDados
import pt.ipt.dam2025.phototravel.modelos.FotoDados
import pt.ipt.dam2025.phototravel.viewmodel.PartilhaDadosViewModel
/**
 * <summary>
 * Atividade responsável pela visualização detalhada de uma fotografia.
 * Permite ao utilizador ver a imagem em ecrã cheio, renomear, apagar ou mover a foto para outras coleções.
 * </summary>
 */
class VerFotoActivity : AppCompatActivity() {

    private val viewModel: PartilhaDadosViewModel by viewModels()
    private var uriDaFoto: String? = null
    private var tituloOriginalDaColecao: String? = null
    private var fotoAtual: FotoDados? = null

    /**
     * <summary>
     * Inicializa a interface, carrega a imagem usando a biblioteca Coil e configura a Toolbar.
     * Implementa um observador reativo para fechar o ecrã caso a foto seja eliminada externamente.
     * </summary>
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_foto)

        // Recupera os dados da Intent (Caminho da imagem e Coleção de origem)
        uriDaFoto = intent.getStringExtra("URI_DA_FOTO")
        tituloOriginalDaColecao = intent.getStringExtra("TITULO_COLECAO")

        //  Configuração da Toolbar com botão de retroceder
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar_ver_foto)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val imageView: ImageView = findViewById(R.id.imagem_ecra_cheio)

        if (uriDaFoto != null) {
            // Carregamento assíncrono da imagem
            imageView.load(Uri.parse(uriDaFoto))
            fotoAtual = viewModel.listaFotos.value?.find { it.uriString == uriDaFoto }
        } else {
            imageView.load(R.drawable.ic_launcher_background)
            Toast.makeText(this, "Erro ao carregar a imagem.", Toast.LENGTH_SHORT).show()
            finish()
        }

        // <summary>
        // Monitoriza o LiveData global. Se a foto desaparecer da lista (ex: apagada),
        // encerra a atividade para evitar erros de referência nula.
        // </summary>
        viewModel.listaFotos.observe(this) { listaAtualizada ->
            val fotoAindaExiste = listaAtualizada.any { it.uriString == uriDaFoto }
            if (!fotoAindaExiste) {
                Toast.makeText(this, "Foto eliminada.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /**
     * <summary> Infla o menu de opções específico para a gestão de fotografias. </summary>
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_fotos, menu)
        return true
    }

    /**
     * <summary> Trata os cliques nas opções do menu (Renomear, Apagar, Mover ou Voltar). </summary>
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val foto = fotoAtual ?: run {
            Toast.makeText(this, "Não foi possível identificar a foto.", Toast.LENGTH_SHORT).show()
            return false
        }

        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.menu_renomear_foto -> {
                mostrarDialogoRenomear(foto)
                true
            }
            R.id.menu_apagar_foto -> {
                mostrarDialogoConfirmacaoApagar(foto)
                true
            }
            R.id.menu_mover_foto -> {
                mostrarDialogoMoverFoto(foto)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * <summary>
     * Apresenta um diálogo com um Spinner (dropdown) para transferir a foto para uma coleção diferente.
     * Filtra a lista para não mostrar a coleção onde a foto já reside.
     * </summary>
     */
    private fun mostrarDialogoMoverFoto(fotoParaMover: FotoDados) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Mover para...")

        val view = layoutInflater.inflate(R.layout.dialog_mover_foto, null)
        val spinner = view.findViewById<Spinner>(R.id.spinnerColecoes)

        val todasColecoes = viewModel.listaColecoes.value ?: emptyList()
        val colecaoAtualDaFoto = fotoParaMover.data
        val colecoesDestino = todasColecoes.filter { it.titulo != colecaoAtualDaFoto }

        val nomes = colecoesDestino.map { it.nomePersonalizado ?: it.titulo }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nomes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        builder.setView(view)

        builder.setPositiveButton("MOVER") { _, _ ->
            val posicao = spinner.selectedItemPosition
            if (posicao != AdapterView.INVALID_POSITION && colecoesDestino.isNotEmpty()) {
                val destino = colecoesDestino[posicao]
                viewModel.moverFotoParaColecao(fotoParaMover, destino)
                Toast.makeText(this, "Foto movida com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        builder.setNegativeButton("CANCELAR", null)
        builder.show()
    }

    /**
     * <summary> Abre uma caixa de diálogo para editar o título personalizado da fotografia. </summary>
     */
    private fun mostrarDialogoRenomear(foto: FotoDados) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Renomear Foto")

        val input = EditText(this)
        input.setText(foto.tituloPersonalizado ?: foto.titulo)
        builder.setView(input)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val novoNome = input.text.toString()
            if (novoNome.isNotBlank()) {
                viewModel.renomearFoto(foto, novoNome)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    /**
     * <summary> Exibe um aviso de confirmação antes de invocar a remoção permanente da foto. </summary>
     */
    private fun mostrarDialogoConfirmacaoApagar(foto: FotoDados) {
        AlertDialog.Builder(this)
            .setTitle("Apagar Foto")
            .setMessage("Tem a certeza que quer apagar permanentemente esta foto?")
            .setPositiveButton("Apagar") { _, _ ->
                viewModel.apagarFoto(foto)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}