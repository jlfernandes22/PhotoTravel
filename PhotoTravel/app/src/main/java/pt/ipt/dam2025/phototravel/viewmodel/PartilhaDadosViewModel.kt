package pt.ipt.dam2025.phototravel.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import pt.ipt.dam2025.phototravel.modelos.ColecaoDados
import pt.ipt.dam2025.phototravel.modelos.FotoDados

/**
 * <summary>
 * ViewModel central da aplicação PhotoTravel.
 * Gere o estado global das coleções e fotos, permitindo a partilha de dados entre fragmentos e activities.
 * </summary>
 */
class PartilhaDadosViewModel(application: Application) : AndroidViewModel(application) {

    //  LiveData privado e público para a lista de COLEÇÕES
    private val _listaColecoes = MutableLiveData<List<ColecaoDados>>()
    val listaColecoes: LiveData<List<ColecaoDados>> get() = _listaColecoes

    //  LiveData privado e público para a lista global de TODAS AS FOTOS
    private val _listaFotos = MutableLiveData<List<FotoDados>>()
    val listaFotos: LiveData<List<FotoDados>> get() = _listaFotos

    /**
     * <summary>
     * Bloco de inicialização: Carrega os dados guardados no disco assim que o ViewModel é instanciado.
     * </summary>
     */
    init {
        val colecoesIniciais = carregarColecoesDoArmazenamento()
        _listaColecoes.value = colecoesIniciais
        _listaFotos.value = colecoesIniciais.flatMap { it.listaFotos }
    }

    /**
     * <summary>
     * Adiciona uma nova foto a uma coleção (baseada na data ou na coleção "Geral").
     * Gere automaticamente a criação de novas coleções e a definição da imagem de capa.
     * </summary>
     */
    fun adicionarFoto(novaFoto: FotoDados) {
        val colecoesAtuais = _listaColecoes.value?.toMutableList() ?: mutableListOf()
        val nomeColecaoAlvo = novaFoto.data.ifEmpty { "Geral" }
        val fotoParaAdicionar = novaFoto.copy(data = nomeColecaoAlvo)

        var colecaoAlvo = colecoesAtuais.find { it.titulo.equals(nomeColecaoAlvo, ignoreCase = true) }

        if (colecaoAlvo == null) {
            colecaoAlvo = ColecaoDados(
                titulo = nomeColecaoAlvo,
                listaFotos = mutableListOf(fotoParaAdicionar),
                capaUri = fotoParaAdicionar.uriString
            )
            colecoesAtuais.add(colecaoAlvo)
        } else {
            val listaAtualizada = colecaoAlvo.listaFotos.toMutableList()
            listaAtualizada.add(fotoParaAdicionar)

            val novaCapa = colecaoAlvo.capaUri ?: fotoParaAdicionar.uriString

            val indice = colecoesAtuais.indexOf(colecaoAlvo)
            colecoesAtuais[indice] = colecaoAlvo.copy(
                listaFotos = listaAtualizada,
                capaUri = novaCapa
            )
        }

        atualizarEstadosEPersistir(colecoesAtuais)
    }

    /**
     * <summary>
     * Remove uma foto específica. Se a foto for a capa da coleção,
     * define automaticamente a próxima foto disponível como nova capa.
     * </summary>
     */
    fun apagarFoto(fotoParaApagar: FotoDados) {
        val colecoesAtuais = _listaColecoes.value?.map { it.copy() }?.toMutableList() ?: return
        val indice = colecoesAtuais.indexOfFirst { it.titulo == fotoParaApagar.data }

        if (indice != -1) {
            val colecao = colecoesAtuais[indice]
            val fotosNovas = colecao.listaFotos.filter { it.uriString != fotoParaApagar.uriString }

            val novaCapa = when {
                fotosNovas.isEmpty() -> null
                colecao.capaUri == fotoParaApagar.uriString -> fotosNovas.first().uriString
                else -> colecao.capaUri
            }

            colecoesAtuais[indice] = colecao.copy(listaFotos = fotosNovas, capaUri = novaCapa)
            atualizarEstadosEPersistir(colecoesAtuais)
        }
    }

    /**
     * <summary> Cria uma nova coleção vazia sem fotos associadas. </summary>
     */
    fun criarColecaoVazia(nome: String) {
        val colecoesAtuais = _listaColecoes.value?.toMutableList() ?: mutableListOf()
        if (colecoesAtuais.any { it.titulo.equals(nome, ignoreCase = true) }) return

        val novaColecao = ColecaoDados(
            titulo = nome,
            nomePersonalizado = nome,
            capaUri = null,
            listaFotos = emptyList()
        )

        colecoesAtuais.add(novaColecao)
        atualizarEstadosEPersistir(colecoesAtuais)
    }

    /**
     * <summary> Remove uma coleção inteira e todas as fotos nela contidas. </summary>
     */
    fun apagarColecao(colecaoParaApagar: ColecaoDados) {
        val colecoesAtuais = _listaColecoes.value?.toMutableList() ?: mutableListOf()
        colecoesAtuais.removeAll { it.titulo == colecaoParaApagar.titulo }
        atualizarEstadosEPersistir(colecoesAtuais)
    }

    /**
     * <summary>
     * Renomeia uma coleção existente e atualiza o vínculo de todas as fotos internas.
     * </summary>
     */
    fun renomearColecao(nomeAntigoColecao: String, novoTitulo: String) {
        if (novoTitulo.isBlank() || nomeAntigoColecao.isBlank()) return
        val colecoesAtuais = _listaColecoes.value?.toMutableList() ?: return

        val indiceColecao = colecoesAtuais.indexOfFirst { it.titulo.equals(nomeAntigoColecao, ignoreCase = true) }
        if (indiceColecao == -1) return

        val colecaoAntiga = colecoesAtuais[indiceColecao]
        val fotosAtualizadas = colecaoAntiga.listaFotos.map { it.copy(data = novoTitulo) }

        colecoesAtuais[indiceColecao] = colecaoAntiga.copy(
            titulo = novoTitulo,
            listaFotos = fotosAtualizadas
        )

        atualizarEstadosEPersistir(colecoesAtuais)
    }

    /**
     * <summary> Atualiza o título personalizado de uma foto específica dentro de uma coleção. </summary>
     */
    fun renomearFoto(fotoParaRenomear: FotoDados, novoNome: String) {
        val colecoesAtuais = _listaColecoes.value?.toMutableList() ?: return
        val indiceColecao = colecoesAtuais.indexOfFirst { it.titulo == fotoParaRenomear.data }
        if (indiceColecao == -1) return

        val colecaoAlvo = colecoesAtuais[indiceColecao]
        val indiceFoto = colecaoAlvo.listaFotos.indexOfFirst { it.uriString == fotoParaRenomear.uriString }
        if (indiceFoto == -1) return

        val fotosNovasDaColecao = colecaoAlvo.listaFotos.toMutableList()
        fotosNovasDaColecao[indiceFoto] = colecaoAlvo.listaFotos[indiceFoto].copy(tituloPersonalizado = novoNome)

        colecoesAtuais[indiceColecao] = colecaoAlvo.copy(listaFotos = fotosNovasDaColecao)
        atualizarEstadosEPersistir(colecoesAtuais)
    }

    /**
     * <summary>
     * Move uma foto entre coleções, atualizando as capas de origem e destino se necessário.
     * </summary>
     */
    fun moverFotoParaColecao(fotoParaMover: FotoDados, colecaoDestino: ColecaoDados) {
        val colecoesAtuais = _listaColecoes.value?.toMutableList() ?: return

        // Remover da origem
        val indiceOrigem = colecoesAtuais.indexOfFirst { it.titulo == fotoParaMover.data }
        if (indiceOrigem != -1) {
            val colOrigem = colecoesAtuais[indiceOrigem]
            val fotosOrigem = colOrigem.listaFotos.toMutableList().apply { removeAll { it.uriString == fotoParaMover.uriString } }
            val novaCapa = if (fotosOrigem.isEmpty()) null else if (colOrigem.capaUri == fotoParaMover.uriString) fotosOrigem.first().uriString else colOrigem.capaUri
            colecoesAtuais[indiceOrigem] = colOrigem.copy(listaFotos = fotosOrigem, capaUri = novaCapa)
        }

        // Adicionar ao destino
        val indiceDestino = colecoesAtuais.indexOfFirst { it.titulo == colecaoDestino.titulo }
        if (indiceDestino != -1) {
            val fotoMovida = fotoParaMover.copy(data = colecaoDestino.titulo)
            val colDest = colecoesAtuais[indiceDestino]
            val fotosDest = colDest.listaFotos.toMutableList().apply { add(fotoMovida) }
            colecoesAtuais[indiceDestino] = colDest.copy(listaFotos = fotosDest, capaUri = colDest.capaUri ?: fotoMovida.uriString)
        }

        atualizarEstadosEPersistir(colecoesAtuais)
    }

    /**
     * <summary> Função auxiliar para atualizar LiveDatas e persistir dados no armazenamento local. </summary>
     */
    private fun atualizarEstadosEPersistir(lista: List<ColecaoDados>) {
        _listaColecoes.value = lista.toList()
        _listaFotos.value = lista.flatMap { it.listaFotos }
        salvarColecoesNoArmazenamento(lista)
    }

    /**
     * <summary> Deserializa o JSON guardado em SharedPreferences para uma lista de colecoes. </summary>
     */
    private fun carregarColecoesDoArmazenamento(): List<ColecaoDados> {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("PhotoTravelPrefs", Context.MODE_PRIVATE)
        val json = sharedPrefs.getString("LISTA_COLECOES", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ColecaoDados>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) { emptyList() }
    }

    /**
     * <summary> Serializa a lista de coleções para JSON e guarda permanentemente no dispositivo. </summary>
     */
    private fun salvarColecoesNoArmazenamento(colecoes: List<ColecaoDados>) {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("PhotoTravelPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("LISTA_COLECOES", Gson().toJson(colecoes)).apply()
    }

    /**
     * <summary> Sincroniza os dados da interface com o que está guardado no disco. </summary>
     */
    fun recarregarDados() {
        val doDisco = carregarColecoesDoArmazenamento()
        _listaColecoes.value = doDisco.toList()
        _listaFotos.value = doDisco.flatMap { it.listaFotos }
    }
}