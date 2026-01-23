package pt.ipt.dam2025.phototravel.modelos

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class FotoDados(
    val id: Int = 0,

    // Lê "base64" do servidor ou mantém uriString local
    @SerializedName(value = "base64", alternate = ["uriString", "imageUrl"])
    val uriString: String,

    @SerializedName("titulo")
    val titulo: String? = null,

    var tituloPersonalizado: String? = null,

    var data: String? = null,

    val latitude: Double? = 0.0,
    val longitude: Double? = 0.0,
    val collectionId: Int = 0
) : Parcelable