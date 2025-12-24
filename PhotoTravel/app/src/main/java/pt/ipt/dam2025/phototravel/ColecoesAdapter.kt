package pt.ipt.dam2025.phototravel

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class ColecoesAdapter(private var colecoes: List<ColecaoDados>,
                      private val onItemClick: (ColecaoDados) -> Unit) : RecyclerView.Adapter<ColecoesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val image : ImageView = view.findViewById(R.id.colecaoImagens)
        val titulo : TextView = view.findViewById(R.id.tituloColecao)
        val data : TextView = view.findViewById(R.id.dataColecoes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_colecao, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int){
        val item = colecoes[position]

        holder.titulo.text = item.titulo
        holder.data.text = item.titulo // Or maybe item.data if it existed, but ColecaoDados uses titulo for data

        try {
            val uri = Uri.parse(item.capaUri)
            holder.image.setImageURI(uri)
        } catch (e: Exception){
            holder.image.setImageResource(android.R.drawable.ic_menu_report_image)
        }

        holder.itemView.setOnClickListener { onItemClick(item) }

    }

    override fun getItemCount() = colecoes.size

    fun atualizarLista(novaLista: List<ColecaoDados>){
        colecoes = novaLista
        notifyDataSetChanged()
    }

}