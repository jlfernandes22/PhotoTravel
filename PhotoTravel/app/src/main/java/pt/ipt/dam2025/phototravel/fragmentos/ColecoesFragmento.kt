package pt.ipt.dam2025.phototravel.fragmentos

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.PopupMenu
import pt.ipt.dam2025.phototravel.R
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.Observer
import androidx.fragment.app.activityViewModels
import pt.ipt.dam2025.phototravel.modelos.ColecaoDados
import pt.ipt.dam2025.phototravel.adaptadores.ColecoesAdapter
import pt.ipt.dam2025.phototravel.DetalheColecaoActivity
import pt.ipt.dam2025.phototravel.viewmodel.PartilhaDadosViewModel

class ColecoesFragmento : Fragment() {

    private val viewModel: PartilhaDadosViewModel by activityViewModels()
    private lateinit var adapter: ColecoesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_colecoes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerColecoes)
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        // --- INICIALIZAÇÃO CORRETA E COMPLETA DO ADAPTER ---
        adapter = ColecoesAdapter(
            emptyList(),
            // Ação para clique normal (abrir detalhes)
            onItemClick = { colecaoClicada ->
                val intent = Intent(requireContext(), DetalheColecaoActivity::class.java)
                // Passa a data (título original) como ID único para a próxima tela
                intent.putExtra("CHAVE_DATA", colecaoClicada.titulo)
                startActivity(intent)
            },
            // Ação para clique no botão de opções
            onOptionsMenuClick = { viewAnchor, colecao ->
                mostrarMenuOpcoes(viewAnchor, colecao)
            }
        )

        recyclerView.adapter = adapter

        // --- OBSERVAR A LISTA DE COLEÇÕES DIRETAMENTE (MAIS EFICIENTE) ---
        // Agora, o fragmento apenas reage à lista de coleções já processada pelo ViewModel.
        viewModel.listaColecoes.observe(viewLifecycleOwner, Observer { listaDeColecoes ->
            if (listaDeColecoes != null) {
                // Simplesmente entrega a lista ao adapter, que se encarrega de atualizar a grelha
                adapter.atualizarLista(listaDeColecoes.reversed())
            }
        })
    }

    /**
     * Mostra o menu pop-up com as opções para uma coleção (ex: Renomear).
     */
    private fun mostrarMenuOpcoes(view: View, colecao: ColecaoDados) {
        // Usa o contexto do fragmento para garantir que o tema é aplicado corretamente
        val popup = PopupMenu(requireContext(), view)
        // Infla o ficheiro de menu que criámos (res/menu/colecao_opcoes_menu.xml)
        popup.menuInflater.inflate(R.menu.menu, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                // Se o item clicado for "Renomear"
                R.id.menu_renomear -> {
                    // Chama a função para mostrar a caixa de diálogo de renomear
                    mostrarDialogoRenomear(colecao)
                    true // Indica que o clique foi tratado
                }
                else -> false
            }
        }
        popup.show()
    }

    /**
     * Mostra a caixa de diálogo para o utilizador inserir um novo nome para a coleção.
     */
    private fun mostrarDialogoRenomear(colecao: ColecaoDados) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Renomear Coleção")

        // Cria um campo de texto para o utilizador escrever
        val input = EditText(requireContext())
        // Preenche o campo com o nome personalizado, ou a data se não houver nome
        input.setText(colecao.nomePersonalizado ?: colecao.titulo)
        builder.setView(input)

        // Configura o botão "Guardar"
        builder.setPositiveButton("Guardar") { dialog, _ ->
            val novoNome = input.text.toString()
            if (novoNome.isNotBlank()) {
                // Chama a função no ViewModel para guardar a alteração de forma persistente
                viewModel.renomearColecao(colecao.titulo, novoNome)
            }
            dialog.dismiss()
        }

        // Configura o botão "Cancelar"
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }
}
