package pt.ipt.dam2025.phototravel.fragmentos

import android.app.AlertDialog
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
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
import java.io.IOException
import java.util.Locale

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

        adapter = ColecoesAdapter(
            emptyList(),
            onItemClick = { colecaoClicada ->
                val intent = Intent(requireContext(), DetalheColecaoActivity::class.java)

                // --- CORREÇÃO AQUI ---
                // Agora, enviamos a lista de fotos da coleção clicada
                intent.putParcelableArrayListExtra("LISTA_FOTOS", ArrayList(colecaoClicada.listaFotos))
                intent.putExtra("NOME_COLECAO", colecaoClicada.nomePersonalizado ?: colecaoClicada.titulo)

                startActivity(intent)
            },
            onOptionsMenuClick = { viewAnchor, colecao ->
                mostrarMenuOpcoes(viewAnchor, colecao)
            }
        )
        recyclerView.adapter = adapter

        viewModel.listaColecoes.observe(viewLifecycleOwner, Observer { listaDeColecoes ->
            if (listaDeColecoes != null) {
                adapter.atualizarLista(listaDeColecoes.reversed())
            }
        })
    }

    /**
     * Mostra o menu pop-up com as opções para uma coleção (ex: Renomear, Apagar).
     */
    private fun mostrarMenuOpcoes(view: View, colecao: ColecaoDados) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu, popup.menu)

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
     * Mostra a caixa de diálogo para o utilizador inserir um novo nome para a coleção.
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
                viewModel.renomearColecao(colecao.titulo, novoNome)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    /**
     * Mostra um diálogo de confirmação antes de apagar uma coleção.
     */
    private fun mostrarDialogoConfirmacaoApagar(colecao: ColecaoDados) {
        AlertDialog.Builder(requireContext())
            .setTitle("Apagar Coleção")
            .setMessage("Tem a certeza que quer apagar permanentemente esta coleção e todas as fotos?")
            .setPositiveButton("Apagar") { _, _ ->
                // Se o utilizador confirmar, chama a função no ViewModel
                viewModel.apagarColecao(colecao)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    private fun renomearComLocalizacao(colecao: ColecaoDados) {
        // Filtra apenas as fotos que têm coordenadas válidas
        val fotosComGps = colecao.listaFotos.filter { it.latitude != null && it.longitude != null }

        if (fotosComGps.isEmpty()) {
            Toast.makeText(context, "Nenhuma foto nesta coleção tem localização.", Toast.LENGTH_SHORT).show()
            return
        }

        // Calcula a média da latitude e da longitude
        val latMedia = fotosComGps.map { it.latitude!! }.average()
        val lonMedia = fotosComGps.map { it.longitude!! }.average()

        try {
            // Usa o Geocoder para obter o endereço a partir das coordenadas
            val geocoder =
                Geocoder(requireContext(), Locale.getDefault())
            val enderecos = geocoder.getFromLocation(latMedia, lonMedia, 1)

            if (enderecos != null && enderecos.isNotEmpty()) {
                val endereco = enderecos[0]
                // Constrói um nome de local
                val nomeDoLocal = endereco.locality ?: endereco.subAdminArea ?: "Localização desconhecida"

                // Mostra um Toast para feedback e chama o ViewModel para guardar
                Toast.makeText(context, "Coleção renomeada para: $nomeDoLocal", Toast.LENGTH_LONG).show()
                viewModel.renomearColecao(colecao.titulo, nomeDoLocal)
            } else {
                Toast.makeText(context, "Não foi possível encontrar um nome para esta localização.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.e("GEOCODER", "Serviço de geocodificação indisponível", e)
            Toast.makeText(context, "Serviço de localização indisponível. Tente mais tarde.", Toast.LENGTH_SHORT).show()
        }
    }
}
