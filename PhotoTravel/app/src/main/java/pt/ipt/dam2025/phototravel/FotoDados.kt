package pt.ipt.dam2025.phototravel

/**
 * Classe para guardar dados de uma foto.
 */
data class FotoDados(
    val uriString: String,
    val titulo: String,
    val data: String,
    val latitude: Double,
    val longitude: Double
)