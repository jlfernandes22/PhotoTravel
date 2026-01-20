package pt.ipt.dam2025.phototravel.adaptadores

import android.content.Intent // <-- ERRO 1 CORRIGIDO: Importar Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.dam2025.phototravel.R
import pt.ipt.dam2025.phototravel.modelos.ColecaoDados
import pt.ipt.dam2025.phototravel.DetalheColecaoActivity // <-- ERRO 2 CORRIGIDO: Importar a sua atividade

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

        holder.titulo.text = item.nomePersonalizado ?: item.titulo

        // Isto já irá mostrar "0 fotos" automaticamente se a lista estiver vazia
        val numeroDeFotos = item.listaFotos.size
        holder.data.text = "$numeroDeFotos fotos"

        // ✅ Lógica para a imagem de capa
        if (item.listaFotos.isEmpty()) {
            // Se não há fotos, mostra um ícone de "álbum vazio" ou limpa a imagem
            holder.image.setImageResource(android.R.drawable.ic_menu_gallery) // Ícone padrão do Android
            if (item.capaUri != null) {

                holder.image.setImageURI(Uri.parse(item.capaUri))
            }
            // Ou podes usar um drawable teu: holder.image.setImageResource(R.drawable.placeholder_vazio)
        } else {
            try {
                // Se houver fotos, tenta carregar a capa definida
                val uri = Uri.parse(item.capaUri)
                holder.image.setImageURI(uri)
            } catch (e: Exception) {
                holder.image.setImageResource(android.R.drawable.ic_menu_report_image)
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
