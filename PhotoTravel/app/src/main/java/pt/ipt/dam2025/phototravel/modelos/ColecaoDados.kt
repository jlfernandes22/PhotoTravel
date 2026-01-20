package pt.ipt.dam2025.phototravel.modelos

/**
 * Dados que serão usados para cada coleção individualmente
 */
data class ColecaoDados(
    val titulo: String,                    // Vai ser a data (ex: "23/12/2025")
    var nomePersonalizado: String? = null,
    val capaUri: String?,                  // ✅ ADICIONADO O '?' (Agora pode ser null)
    var listaFotos: List<FotoDados>        // Lista de fotos da coleção
)