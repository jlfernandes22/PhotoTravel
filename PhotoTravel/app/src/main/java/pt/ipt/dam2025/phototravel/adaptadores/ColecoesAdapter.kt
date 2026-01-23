package pt.ipt.dam2025.phototravel.adaptadores

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

/**
 * <summary>
 * Adapter para a RecyclerView das Coleções.
 * Responsável por converter a lista de coleções [ColecaoDados] em itens visuais na interface.
 * </summary>
 */
class ColecoesAdapter(
    private var colecoes: List<ColecaoDados>,
    private val onItemClick: (ColecaoDados) -> Unit,
    private val onOptionsMenuClick: (View, ColecaoDados) -> Unit
) : RecyclerView.Adapter<ColecoesAdapter.ViewHolder>() {

    /**
     * <summary>
     * ViewHolder que mapeia os componentes de interface definidos no XML item_colecao.
     * </summary>
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.colecaoImagens)
        val titulo: TextView = view.findViewById(R.id.tituloColecao)
        val data: TextView = view.findViewById(R.id.dataColecoes)
        val opcoesButton: ImageButton = view.findViewById(R.id.opcoes_colecao_button)
    }

    /**
     * <summary>
     * Infla o layout XML para cada item da lista e cria a instância do ViewHolder.
     * </summary>
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_colecao, parent, false)
        return ViewHolder(view)
    }

    /**
     * <summary>
     * Vincula os dados ColecaoDados aos elementos visuais (TextViews, ImageView).
     * Controla também a lógica de exibição da imagem da capa da coleção e eventos de clicar.
     * </summary>
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = colecoes[position]

        //  Define o título da coleção priorizando o nome personalizado
        holder.titulo.text = item.nomePersonalizado ?: item.titulo

        // Atualiza a contagem de fotos
        val numeroDeFotos = item.listaFotos.size
        holder.data.text = "$numeroDeFotos fotos"

        // Limpeza preventiva
        holder.image.setImageDrawable(null)

        // Lógica para carregar a imagem de capa ou a padrão
        if (item.listaFotos.isEmpty()) {
            holder.image.setImageResource(android.R.drawable.ic_menu_gallery)
        } else {
            try {
                val uri = Uri.parse(item.capaUri)
                holder.image.setImageURI(uri)
            } catch (e: Exception) {
                holder.image.setImageResource(android.R.drawable.ic_menu_report_image)
            }
        }

        //  Configuração dos listeners de clicar
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }

        holder.opcoesButton.setOnClickListener { view ->
            onOptionsMenuClick(view, item)
        }
    }

    /**
     * <summary> Retorna o tamanho total da lista de coleções </summary>
     */
    override fun getItemCount() = colecoes.size

    /**
     * <summary>
     * Atualiza a lista interna do adapter e notifica a RecyclerView para redesenhar.
     * </summary>
     * <param name="novaLista">A nova lista de coleções a ser exibida</param>
     */
    fun atualizarLista(novaLista: List<ColecaoDados>) {
        colecoes = novaLista
        notifyDataSetChanged()
    }
}