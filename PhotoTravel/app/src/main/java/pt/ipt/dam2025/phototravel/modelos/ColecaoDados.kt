package pt.ipt.dam2025.phototravel.modelos

/**
 * Dados que serão usados para cada coleção individualmente
 */
data class ColecaoDados(
    val titulo: String,
    var nomePersonalizado: String? = null,
    val capaUri: String?,
    var listaFotos: List<FotoDados>
)