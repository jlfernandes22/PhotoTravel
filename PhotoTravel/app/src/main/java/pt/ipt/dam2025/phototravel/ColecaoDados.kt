package pt.ipt.dam2025.phototravel

/**
 * Dados que serão usados para cada coleção individualmente
 */
data class ColecaoDados(
    val titulo: String,       // Vai ser a data (ex: "23/12/2025")
    val capaUri: String,      // A foto de capa (usamos a primeira foto do dia)
    val listaFotos: List<FotoDados> // Guardamos a lista original caso queiramos abrir depois
)