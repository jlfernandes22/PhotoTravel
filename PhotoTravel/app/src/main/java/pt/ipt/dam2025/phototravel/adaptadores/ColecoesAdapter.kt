package pt.ipt.dam2025.phototravel.adaptadores

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.dam2025.phototravel.modelos.ColecaoDados
import android.widget.ImageButton
import pt.ipt.dam2025.phototravel.R

class ColecoesAdapter(
    private var colecoes: List<ColecaoDados>,
    private val onItemClick: (ColecaoDados) -> Unit,
    private val onOptionsMenuClick: (View, ColecaoDados) -> Unit // <--- NOVA FUNÇÃO
) : RecyclerView.Adapter<ColecoesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.colecaoImagens)
        val titulo: TextView = view.findViewById(R.id.tituloColecao)
        val data: TextView = view.findViewById(R.id.dataColecoes)
        val opcoesButton: ImageButton =
            view.findViewById(R.id.opcoes_colecao_button) // <--- LIGAÇÃO AO BOTÃO
    }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_colecao, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = colecoes[position]

        // Usa o nome personalizado se existir, senão usa o título (data)
        holder.titulo.text = item.nomePersonalizado ?: item.titulo
        val numeroDeFotos = item.listaFotos.size
        holder.data.text = "$numeroDeFotos fotos"

        try {
            val uri = Uri.parse(item.capaUri)
            holder.image.setImageURI(uri)
        } catch (e: Exception) {
            holder.image.setImageResource(android.R.drawable.ic_menu_report_image)
        }

        holder.itemView.setOnClickListener { onItemClick(item) }

        // Clique no botão de opções chama a nova função
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