package pt.ipt.dam2025.phototravel.adaptadores

import coil.load
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil

import androidx.recyclerview.widget.RecyclerView // AppCompatActivity foi removido daqui
import pt.ipt.dam2025.phototravel.R
import pt.ipt.dam2025.phototravel.modelos.FotoDados

// ✅ A classe agora herda APENAS de RecyclerView.Adapter
// O construtor primário foi movido para a declaração da classe.
class FotosAdapter(
    private var listaFotos: List<FotoDados>,
    private val onItemClick: (FotoDados) -> Unit
) : RecyclerView.Adapter<FotosAdapter.FotoViewHolder>() {

    fun atualizarFotos(novasFotos: List<FotoDados>) {
        this.listaFotos = novasFotos
        notifyDataSetChanged() // Notifica o RecyclerView para se redesenhar
    }

    class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imagem_item_foto)

        fun bind(foto: FotoDados) {
            val uri = Uri.parse(foto.uriString)
            imageView.load(uri) {
                crossfade(true)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_foto, parent, false)
        return FotoViewHolder(view)
    }

    override fun getItemCount(): Int = listaFotos.size

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        val foto = listaFotos[position]
        holder.bind(foto)

        holder.itemView.setOnClickListener {
            onItemClick(foto)
        }
    }

    class FotoDiffCallback : DiffUtil.ItemCallback<FotoDados>() {
        override fun areItemsTheSame(oldItem: FotoDados, newItem: FotoDados): Boolean =
            oldItem.uriString == newItem.uriString

        override fun areContentsTheSame(oldItem: FotoDados, newItem: FotoDados): Boolean =
            oldItem == newItem
    }
    }
