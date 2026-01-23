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
    private val onItemLongClick: (FotoDados) -> Unit // ✅ NOVO: Para mover a foto
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

        // Carregar a imagem
        val uri = Uri.parse(foto.uriString)
        holder.imageView.load(uri) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher_background) // opcional
        }

        // Clique normal: Abrir foto
        holder.itemView.setOnClickListener {
            onItemClick(foto)
        }

        // ✅ NOVO: Clique longo: Mover foto
        holder.itemView.setOnLongClickListener {
            onItemLongClick(foto)
            true // 'true' indica que o clique foi consumido
        }
    }

    class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imagem_item_foto)
    }
}