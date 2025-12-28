package pt.ipt.dam2025.phototravel.adaptadores

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.dam2025.phototravel.R
import pt.ipt.dam2025.phototravel.VerFotoActivity
import pt.ipt.dam2025.phototravel.modelos.FotoDados

class FotosAdapter(private val fotos: List<FotoDados>) : RecyclerView.Adapter<FotosAdapter.ViewHolder>() {

    /**
     * ViewHolder que contém a referência para a ImageView de cada item.
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imagem_item_foto)
    }

    /**
     * Cria uma nova view para um item da lista.
     * É aqui que o layout para cada item é "inflado".
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // CORREÇÃO CRÍTICA: Garante que o layout para uma foto individual é usado.
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_foto, parent, false) // DEVE USAR item_foto.xml
        return ViewHolder(view)
    }

    /**
     * Preenche os dados de um item (a foto) na sua respetiva view.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val foto = fotos[position]
        val uri = Uri.parse(foto.uriString)
        holder.imageView.setImageURI(uri)

        // LÓGICA DO CLIQUE: Abre a foto em ecrã inteiro.
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            // Intenção para abrir a VerFotoActivity
            val intent = Intent(context, VerFotoActivity::class.java)
            // Anexa a URI da foto clicada
            intent.putExtra("URI_DA_FOTO", foto.uriString)
            // Inicia a nova tela
            context.startActivity(intent)
        }
    }

    /**
     * Retorna o número total de itens na lista.
     */
    override fun getItemCount() = fotos.size
}
