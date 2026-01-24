package pt.ipt.dam2025.phototravel.modelos

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class FotoDados(
    val id: Long = 0L,

    @SerializedName(value = "base64", alternate = ["uriString"])
    val uriString: String,

    @SerializedName("titulo")
    val titulo: String? = null,

    var tituloPersonalizado: String? = null,

    var data: String? = null,

    val latitude: Double? = 0.0,
    val longitude: Double? = 0.0,
    val collectionId: Int = 0,


    var sincronizada: Boolean = false
) : Parcelable