package pt.ipt.dam2025.phototravel.adaptadores

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import pt.ipt.dam2025.phototravel.R
import pt.ipt.dam2025.phototravel.modelos.FotoDados
import java.io.File

class FotosAdapter(
    private var listaFotos: List<FotoDados>,
    private val onItemClick: (FotoDados) -> Unit,
    private val onItemLongClick: (FotoDados) -> Unit
) : RecyclerView.Adapter<FotosAdapter.FotoViewHolder>() {

    class FotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imagem_item_foto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        // Certifica-te que o nome do layout (item_foto.xml) está correto
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_foto, parent, false)
        return FotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        val foto = listaFotos[position]

        // --- CORREÇÃO: Simplificação total ---
        // O Coil é inteligente. Ele sabe ler:
        // 1. "http://..." (Internet)
        // 2. "file:///..." (Ficheiros locais do sync)
        // 3. "content://..." (Fotos da câmara/galeria)

        holder.imageView.load(foto.uriString) {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_gallery) // Ícone enquanto carrega
            error(android.R.drawable.ic_menu_report_image)   // Ícone se falhar
        }

        // Clique normal para abrir
        holder.itemView.setOnClickListener {
            onItemClick(foto)
        }

        // Clique longo para mover/apagar
        holder.itemView.setOnLongClickListener {
            onItemLongClick(foto)
            true
        }
    }

    override fun getItemCount(): Int = listaFotos.size

    // Função auxiliar para atualizar a lista sem recriar o adapter
    fun atualizarFotos(novasFotos: List<FotoDados>) {
        this.listaFotos = novasFotos
        notifyDataSetChanged()
    }
}