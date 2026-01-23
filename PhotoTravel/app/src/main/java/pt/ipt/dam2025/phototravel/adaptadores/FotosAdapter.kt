package pt.ipt.dam2025.phototravel.adaptadores

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import pt.ipt.dam2025.phototravel.R
import pt.ipt.dam2025.phototravel.modelos.FotoDados

class FotosAdapter(
    private var listaFotos: List<FotoDados>,
    private val onItemClick: (FotoDados) -> Unit,
    private val onItemLongClick: (FotoDados) -> Unit
) : RecyclerView.Adapter<FotosAdapter.FotoViewHolder>() {

    fun atualizarFotos(novasFotos: List<FotoDados>) {
        this.listaFotos = novasFotos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_foto, parent, false)
        return FotoViewHolder(view)
    }

    override fun getItemCount(): Int = listaFotos.size

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        val foto = listaFotos[position]

        // Deteta se é Base64 puro e adiciona o prefixo se necessário
        val imagemParaCarregar = if (!foto.uriString.startsWith("http") && 
            !foto.uriString.startsWith("content") && 
            !foto.uriString.startsWith("data:") &&
            !foto.uriString.startsWith("/")) {
            "data:image/jpeg;base64,${foto.uriString}"
        } else {
            foto.uriString
        }

        holder.imageView.load(imagemParaCarregar) {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_gallery)
            error(android.R.drawable.ic_menu_report_image)
        }

        holder.itemView.setOnClickListener {
            onItemClick(foto)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(foto)
            true
        }
    }

    class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imagem_item_foto)
    }
}
