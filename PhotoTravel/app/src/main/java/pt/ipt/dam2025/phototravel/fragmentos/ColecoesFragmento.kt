package pt.ipt.dam2025.phototravel.fragmentos

import android.app.AlertDialog
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.Observer
import pt.ipt.dam2025.phototravel.modelos.ColecaoDados
import pt.ipt.dam2025.phototravel.adaptadores.ColecoesAdapter
import pt.ipt.dam2025.phototravel.DetalheColecaoActivity
import pt.ipt.dam2025.phototravel.R
import pt.ipt.dam2025.phototravel.viewmodel.PartilhaDadosViewModel
import java.io.IOException
import androidx.fragment.app.activityViewModels
import java.util.Locale

/**
 * <summary>
 * Fragmento responsável por fazer a lista e gerir as coleções de fotos.
 * Permite visualizar, criar, apagar e renomear coleções (manualmente ou via Geocoder).
 * </summary>
 */
class ColecoesFragmento : Fragment() {

    private val viewModel: PartilhaDadosViewModel by activityViewModels()
    private lateinit var adapter: ColecoesAdapter


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_colecoes, container, false)
    }

    /**
     * <summary>
     * Inicializa a interface, configura a RecyclerView em grelha (grid) e observa as mudanças no ViewModel.
     * </summary>
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configura o Floating Action Button para criar novas coleções
        val fab: View = view.findViewById(R.id.fabAddColecao)
        fab.setOnClickListener {
            mostrarDialogoCriarColecao()
        }

        // Configura a RecyclerView com 2 colunas
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerColecoes)
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        adapter = ColecoesAdapter(
            emptyList(),
            onItemClick = { colecaoClicada ->
                //  Navega para os detalhes da coleção ao clicar
                val intent = Intent(requireContext(), DetalheColecaoActivity::class.java)
                intent.putExtra("NOME_COLECAO", colecaoClicada.nomePersonalizado ?: colecaoClicada.titulo)
                intent.putExtra("TITULO_COLECAO", colecaoClicada.titulo)
                startActivity(intent)
            },
            onOptionsMenuClick = { viewAnchor, colecao ->
                mostrarMenuOpcoes(viewAnchor, colecao)
            }
        )
        recyclerView.adapter = adapter
        viewModel.sincronizarDados(requireContext())
        viewModel.listaColecoes.observe(viewLifecycleOwner, Observer { listaDeColecoes ->
            if (listaDeColecoes != null) {
                adapter.atualizarLista(listaDeColecoes.reversed())
            }
        })
    }

    /**
     * <summary>
     * Exibe um PopupMenu com ações rápidas para a coleção selecionada.
     * </summary>
     */
    private fun mostrarMenuOpcoes(view: View, colecao: ColecaoDados) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_colecao, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_renomear -> {
                    mostrarDialogoRenomear(colecao)
                    true
                }
                R.id.menu_renomear_localizacao -> {
                    renomearComLocalizacao(colecao)
                    true
                }
                R.id.menu_apagar -> {
                    mostrarDialogoConfirmacaoApagar(colecao)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    /**
     * <summary>
     * Abre um diálogo com um campo de texto para renomear manualmente a coleção.
     * </summary>
     */
    private fun mostrarDialogoRenomear(colecao: ColecaoDados) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Renomear Coleção")

        val input = EditText(requireContext())
        input.setText(colecao.nomePersonalizado ?: colecao.titulo)
        builder.setView(input)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val novoNome = input.text.toString()
            if (novoNome.isNotBlank()) {
                viewModel.renomearColecao(colecao.titulo ?: "", novoNome)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    /**
     * <summary>
     * Solicita confirmação antes de apagar definitivamente uma coleção .
     * </summary>
     */
    private fun mostrarDialogoConfirmacaoApagar(colecao: ColecaoDados) {
        AlertDialog.Builder(requireContext())
            .setTitle("Apagar Coleção")
            .setMessage("Tem a certeza que quer apagar permanentemente esta coleção e todas as fotos?")
            .setPositiveButton("Apagar") { _, _ ->
                viewModel.apagarColecao(colecao)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * <summary>
     * Diálogo para criação de uma nova coleção vazia.
     * </summary>
     */
    private fun mostrarDialogoCriarColecao() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Nova Coleção")

        val input = EditText(requireContext())
        input.hint = "Ex: Viagem Paris 2025"
        builder.setView(input)

        builder.setPositiveButton("Criar") { dialog, _ ->
            val nome = input.text.toString()
            if (nome.isNotBlank()) {
                viewModel.criarColecaoVazia(nome)
            } else {
                Toast.makeText(context, "Insira um nome válido", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    /**
     * <summary>
     *  Calcula a média das coordenadas das fotos na coleção
     * e utiliza o Geocoder para converter em nome de cidade/localidade.
     * </summary>
     */
    private fun renomearComLocalizacao(colecao: ColecaoDados) {
        val fotosComGps = colecao.listaFotos.filter { it.latitude != null && it.longitude != null }

        if (fotosComGps.isEmpty()) {
            Toast.makeText(context, "Nenhuma foto nesta coleção tem localização.", Toast.LENGTH_SHORT).show()
            return
        }

        // <summary> Obtém o centro geográfico da coleção </summary>
        val latMedia = fotosComGps.map { it.latitude!! }.average()
        val lonMedia = fotosComGps.map { it.longitude!! }.average()

        try {
            // <summary> Tenta obter o nome da localidade via serviço de sistema (Geocoder) </summary>
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val enderecos = geocoder.getFromLocation(latMedia, lonMedia, 1)

            if (enderecos != null && enderecos.isNotEmpty()) {
                val endereco = enderecos[0]
                val nomeDoLocal = endereco.locality ?: endereco.subAdminArea ?: "Localização desconhecida"

                Toast.makeText(context, "Coleção renomeada para: $nomeDoLocal", Toast.LENGTH_LONG).show()
                viewModel.renomearColecao(colecao.titulo ?: "", nomeDoLocal)
            } else {
                Toast.makeText(context, "Não foi possível encontrar um nome para esta localização.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.e("GEOCODER", "Serviço de geocodificação indisponível", e)
            Toast.makeText(context, "Serviço de localização indisponível. Tente mais tarde.", Toast.LENGTH_SHORT).show()
        }
    }
}