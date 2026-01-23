package pt.ipt.dam2025.phototravel.adaptadores

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import pt.ipt.dam2025.phototravel.R
import pt.ipt.dam2025.phototravel.modelos.ColecaoDados
import pt.ipt.dam2025.phototravel.DetalheColecaoActivity

class ColecoesAdapter(
    private var colecoes: List<ColecaoDados>,
    private val onItemClick: (ColecaoDados) -> Unit,
    private val onOptionsMenuClick: (View, ColecaoDados) -> Unit
) : RecyclerView.Adapter<ColecoesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.colecaoImagens)
        val titulo: TextView = view.findViewById(R.id.tituloColecao)
        val data: TextView = view.findViewById(R.id.dataColecoes)
        val opcoesButton: ImageButton = view.findViewById(R.id.opcoes_colecao_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_colecao, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = colecoes[position]

        // 1. Tratamento do Título (Fallback para "Sem Título")
        val tituloExibicao = item.nomePersonalizado ?: item.titulo
        holder.titulo.text = if (tituloExibicao.isNullOrBlank()) "Sem Título" else tituloExibicao

        // 2. Tratamento da Quantidade de Fotos
        val numeroDeFotos = item.listaFotos.size
        holder.data.text = "$numeroDeFotos fotos"

        // 3. Carregamento da Imagem usando Coil (suporta URIs locais e Base64)
        if (!item.capaUri.isNullOrEmpty()) {
            holder.image.load(item.capaUri) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_report_image)
            }
        } else {
            // Fallback se não houver capa: tenta usar a primeira foto ou ícone padrão
            val primeiraFoto = item.listaFotos.firstOrNull()?.uriString
            if (!primeiraFoto.isNullOrEmpty()) {
                holder.image.load(primeiraFoto) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_gallery)
                }
            } else {
                holder.image.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }

        holder.opcoesButton.setOnClickListener { view ->
            onOptionsMenuClick(view, item)
        }
    }

    override fun getItemCount() = colecoes.size

    fun atualizarLista(novaLista: List<ColecaoDados>) {
        colecoes = novaLista
        notifyDataSetChanged()
    }
}
