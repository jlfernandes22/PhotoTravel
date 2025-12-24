package pt.ipt.dam2025.phototravel

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView


class FotosAdapter(private val listaFotos: List<FotoDados>) :
    RecyclerView.Adapter<FotosAdapter.FotoViewHolder>() {
        class FotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imagem: ImageView = view.findViewById(R.id.imgFotoSimples)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_foto_colecoes, parent, false)
            return FotoViewHolder(view)
        }

        override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
            val foto = listaFotos[position]
            try {
                holder.imagem.setImageURI(Uri.parse(foto.uriString))
            } catch (e: Exception) {
                // Ignorar erro se a imagem não carregar
            }
        }

        override fun getItemCount() = listaFotos.size
    }

