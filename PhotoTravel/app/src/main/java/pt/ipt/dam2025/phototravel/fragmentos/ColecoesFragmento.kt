package pt.ipt.dam2025.phototravel.fragmentos

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import pt.ipt.dam2025.phototravel.R
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.Observer
import androidx.fragment.app.activityViewModels
import pt.ipt.dam2025.phototravel.ColecaoDados
import pt.ipt.dam2025.phototravel.ColecoesAdapter
import pt.ipt.dam2025.phototravel.DetalheColecaoActivity
import pt.ipt.dam2025.phototravel.PartilhaDadosViewModel

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

        // --- AQUI ESTÁ A MUDANÇA IMPORTANTE ---
        // Inicializamos o adapter passando a função do que fazer no clique
        adapter = ColecoesAdapter(emptyList()) { colecaoClicada ->

            // Abrir a nova Activity
            val intent = Intent(requireContext(), DetalheColecaoActivity::class.java)
            // Passar a data para saber que álbum abrir
            intent.putExtra("CHAVE_DATA", colecaoClicada.titulo)
            startActivity(intent)
        }

        recyclerView.adapter = adapter

        viewModel.listaFotos.observe(viewLifecycleOwner, Observer { todasAsFotos ->
            if (todasAsFotos != null) {
                val fotosAgrupadasPorData = todasAsFotos.groupBy { it.data }

                val listaDeColecoes = fotosAgrupadasPorData.map { (data, listaDeFotosDoDia) ->
                    ColecaoDados(
                        titulo = data,
                        capaUri = listaDeFotosDoDia.first().uriString,
                        listaFotos = listaDeFotosDoDia
                    )
                }

                adapter.atualizarLista(listaDeColecoes.reversed())
            }
        })
    }
}