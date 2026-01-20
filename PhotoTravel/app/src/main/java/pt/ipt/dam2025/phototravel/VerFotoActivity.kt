package pt.ipt.dam2025.phototravel

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.observe
import coil.load
import com.google.android.material.appbar.MaterialToolbar
import pt.ipt.dam2025.phototravel.modelos.ColecaoDados
import pt.ipt.dam2025.phototravel.modelos.FotoDados
import pt.ipt.dam2025.phototravel.viewmodel.PartilhaDadosViewModel

class VerFotoActivity : AppCompatActivity() {

    private val viewModel: PartilhaDadosViewModel by viewModels()
    private var uriDaFoto: String? = null
    // A variável tituloOriginalDaColecao parece não estar a ser usada, pode considerar removê-la se não for necessária.
    private var tituloOriginalDaColecao: String? = null
    private var fotoAtual: FotoDados? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_foto)

        uriDaFoto = intent.getStringExtra("URI_DA_FOTO")
        tituloOriginalDaColecao = intent.getStringExtra("TITULO_COLECAO")

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar_ver_foto)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val imageView: ImageView = findViewById(R.id.imagem_ecra_cheio)

        if (uriDaFoto != null) {
            imageView.load(Uri.parse(uriDaFoto))
            // Encontra a foto inicial quando a atividade é criada
            fotoAtual = viewModel.listaFotos.value?.find { it.uriString == uriDaFoto }
        } else {
            imageView.load(R.drawable.ic_launcher_background)
            Toast.makeText(this, "Erro ao carregar a imagem.", Toast.LENGTH_SHORT).show()
            finish() // Fecha a atividade se não houver URI, pois não há o que mostrar
        }

        // ✅ PASSO 1 (CORREÇÃO): Observar a lista de fotos para reagir a eliminações
        viewModel.listaFotos.observe(this) { listaAtualizada ->
            // Verifica se a foto que estamos a ver ainda existe na lista do ViewModel.
            // Se 'find' devolver null, significa que a foto foi removida.
            val fotoAindaExiste = listaAtualizada.any { it.uriString == uriDaFoto }

            if (!fotoAindaExiste) {
                // Se a foto foi removida (p.ex., por outra parte do código ou após a ação de apagar),
                // fecha esta atividade.
                Toast.makeText(this, "Foto eliminada.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_fotos, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Usa a fotoAtual que já foi definida, garantindo que não é nula.
        val foto = fotoAtual ?: run {
            Toast.makeText(this, "Não foi possível identificar a foto para realizar a ação.", Toast.LENGTH_SHORT).show()
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

    private fun mostrarDialogoMoverFoto(foto: FotoDados) {
        val colecoesDisponiveis = viewModel.listaColecoes.value?.filter { it.titulo != foto.data }

        if (colecoesDisponiveis.isNullOrEmpty()) {
            Toast.makeText(this, "Não existem outras coleções para mover a foto.", Toast.LENGTH_SHORT).show()
            return
        }

        val nomesDasColecoes = colecoesDisponiveis.map { it.nomePersonalizado ?: it.titulo }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Mover foto para...")
            .setItems(nomesDasColecoes) { _, which ->
                val colecaoDestino = colecoesDisponiveis[which]
                viewModel.moverFotoParaColecao(foto, colecaoDestino)
                Toast.makeText(this, "Foto movida para ${colecaoDestino.nomePersonalizado ?: colecaoDestino.titulo}", Toast.LENGTH_SHORT).show()
                // A atividade fecha porque a foto já não pertence a esta coleção
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

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
                // Opcional: Atualizar o título da toolbar se estiver a ser usado
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun mostrarDialogoConfirmacaoApagar(foto: FotoDados) {
        AlertDialog.Builder(this)
            .setTitle("Apagar Foto")
            .setMessage("Tem a certeza que quer apagar permanentemente esta foto?")
            .setPositiveButton("Apagar") { _, _ ->
                // ✅ PASSO 2 (CORREÇÃO): A chamada ao viewModel desencadeia a atualização do LiveData
                viewModel.apagarFoto(foto)
                // A observação do LiveData no onCreate tratará de fechar a atividade.
                // A chamada finish() aqui é redundante, mas inofensiva.
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
