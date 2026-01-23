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

/**
 * <summary>
 * adapter para a RecyclerView das Fotos.
 * Gere a exibição de imagens dentro de uma coleção, utilizando a biblioteca Coil para o carregamento ser eficiente.
 * </summary>
 */
class FotosAdapter(
    private var listaFotos: List<FotoDados>,
    private val onItemClick: (FotoDados) -> Unit,
    private val onItemLongClick: (FotoDados) -> Unit
) : RecyclerView.Adapter<FotosAdapter.FotoViewHolder>() {

    /**
     * <summary>
     * Atualiza o conjunto de dados do adapter e dá refresh à interface.
     * </summary>
     * <param name="novasFotos">Nova lista de fotos [FotoDados]</param>
     */
    fun atualizarFotos(novasFotos: List<FotoDados>) {
        this.listaFotos = novasFotos
        notifyDataSetChanged()
    }

    /**
     * <summary>
     * Infla o layout [R.layout.item_foto] para criar a visualização de cada célula da grelha/lista.
     * </summary>
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_foto, parent, false)
        return FotoViewHolder(view)
    }

    /**
     * <summary> Retorna a quantidade total de fotos na lista atual. </summary>
     */
    override fun getItemCount(): Int = listaFotos.size

    /**
     * <summary>
     * Liga os dados da imagem à View.
     * Implementa o carregamento assíncrono com Coil e gere os eventos de clicar.
     * </summary>
     */
    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        val foto = listaFotos[position]

        // Converte a String em URI e carrega a imagem via Coil com efeito de transição
        val uri = Uri.parse(foto.uriString)
        holder.imageView.load(uri) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher_background)
            error(android.R.drawable.stat_notify_error) // Sugestão: ícone de erro caso a URI falhe
        }

        // Listener para clique simples: Geralmente usado para abrir a foto em ecrã inteiro
        holder.itemView.setOnClickListener {
            onItemClick(foto)
        }


        // Listener para o clique longo: funcionalidade de mover ou menu de contexto.
        // O retorno 'true' impede que o clique simples seja disparado ao mesmo tempo.
        holder.itemView.setOnLongClickListener {
            onItemLongClick(foto)
            true
        }
    }

    /**
     * <summary>
     * Contentor que mantém a referência para a ImageView de cada item, otimizando a performance.
     * </summary>
     */
    class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imagem_item_foto)
    }
}