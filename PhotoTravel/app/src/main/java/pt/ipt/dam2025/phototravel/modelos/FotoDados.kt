package pt.ipt.dam2025.phototravel.modelos

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// ADICIONE AS ANOTAÇÕES @Parcelize e : Parcelable
@Parcelize
data class FotoDados(
    val uriString: String,
    val titulo: String,
    val tituloPersonalizado: String?,
    val data: String,
    val latitude: Double?,
    val longitude: Double?
) : Parcelable
