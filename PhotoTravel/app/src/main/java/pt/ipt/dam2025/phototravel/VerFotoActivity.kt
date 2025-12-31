
package pt.ipt.dam2025.phototravel

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pt.ipt.dam2025.phototravel.modelos.FotoDados
import pt.ipt.dam2025.phototravel.viewmodel.PartilhaDadosViewModel

class VerFotoActivity : AppCompatActivity() {

    private val viewModel: PartilhaDadosViewModel by viewModels()
    private var fotoAtual: FotoDados? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
         enableEdgeToEdge() // Considerar se esta linha é necessária, pode causar problemas de layout
        setContentView(R.layout.activity_ver_foto)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Receber os dados da foto que vieram do clique
        @Suppress("DEPRECATION") // Necessário para compatibilidade com APIs mais antigas
        fotoAtual = intent.getParcelableExtra<FotoDados>("DADOS_DA_FOTO")
        val uriString = intent.getStringExtra("URI_DA_FOTO")

        val imageView = findViewById<ImageView>(R.id.foto_em_ecra_inteiro)
        val optionsButton = findViewById<ImageView>(R.id.menu_fotos) // Assumindo que tem um botão de opções

        // 2. Carregar a imagem se a URI e os dados da foto existirem
        if (uriString != null && fotoAtual != null) {
            val uri = Uri.parse(uriString)
            imageView.setImageURI(uri)
        } else {
            // Se os dados não vierem, fecha a atividade para evitar erros
            finish()
            return
        }

        // 3. Adicionar OnClickListener para o botão de opções
        optionsButton.setOnClickListener { view ->
            // Chama a função do menu, passando a foto atual
            fotoAtual?.let { mostrarMenuOpcoes(view, it) }
        }
    }


    /**
     * Mostra o menu de opções para a foto (Renomear, Apagar, Mover).
     */
    private fun mostrarMenuOpcoes(view: View, foto: FotoDados) {
        val popup = PopupMenu(this, view)
        // Decidir qual menu inflar (ex: menu_fotos.xml)
        popup.menuInflater.inflate(R.menu.menu_fotos, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_renomear_foto -> {
                    mostraDialogoRenomear(foto)
                    true
                }
                R.id.menu_apagar_foto -> {
                    mostraDialogoConfirmacaoApagar(foto)
                    true
                }
                R.id.menu_mover_foto -> {
                    // Lógica para mover a foto para outra coleção
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    /**
     * Mostra a caixa de diálogo para o utilizador renomear a foto.
     */
    private fun mostraDialogoRenomear(foto: FotoDados) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Renomear Foto")

        val input = EditText(this)
        input.setText(foto.tituloPersonalizado ?: foto.titulo)
        builder.setView(input)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val novoNome = input.text.toString()
            if (novoNome.isNotBlank()) {
                viewModel.renomearFoto(titulo = foto.titulo, tituloPersonalizado = novoNome)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    /**
     * Mostra um diálogo de confirmação antes de apagar uma foto.
     */
    private fun mostraDialogoConfirmacaoApagar(foto: FotoDados) {
        AlertDialog.Builder(this)
            .setTitle("Apagar Foto")
            .setMessage("Tem a certeza que quer apagar permanentemente esta foto?")
            .setPositiveButton("Apagar") { _, _ ->
                viewModel.apagarFoto(foto)
                // Após apagar, fecha a atividade para voltar à lista
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
