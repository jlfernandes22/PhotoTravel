package pt.ipt.dam2025.phototravel.modelos

import com.google.gson.annotations.SerializedName

data class ColecaoDados(
    val id: Int = 0,
  
    @SerializedName("titulo")
    val titulo: String? = null,

    var nomePersonalizado: String? = null,

    val capaUri: String? = null,

    var listaFotos: List<FotoDados> = emptyList()
)