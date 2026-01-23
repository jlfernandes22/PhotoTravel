package pt.ipt.dam2025.phototravel.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.phototravel.modelos.ColecaoDados
import pt.ipt.dam2025.phototravel.modelos.FotoDados
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import pt.ipt.dam2025.phototravel.data.remote.RetrofitInstance
import java.io.File
import java.io.FileOutputStream

class PartilhaDadosViewModel(application: Application) : AndroidViewModel(application) {

    private val _listaColecoes = MutableLiveData<List<ColecaoDados>>()
    val listaColecoes: LiveData<List<ColecaoDados>> get() = _listaColecoes

    private val _listaFotos = MutableLiveData<List<FotoDados>>()
    val listaFotos: LiveData<List<FotoDados>> get() = _listaFotos

    init {
        val colecoesIniciais = carregarColecoesDoArmazenamento()
        _listaColecoes.value = colecoesIniciais
        _listaFotos.value = colecoesIniciais.flatMap { it.listaFotos }
    }

    /**
     * Função Principal de Sincronização
     */
    fun sincronizarDados(context: Context) {
        Log.d("DEBUG_SYNC", "--- Início da Sincronização ---")
        val sharedPrefs = context.getSharedPreferences("PhotoTravelPrefs", Context.MODE_PRIVATE)
        val token = sharedPrefs.getString("USER_TOKEN", null)

        if (token == null) return

        viewModelScope.launch {
            try {
                // 1. Obter Coleções
                val responseColecoes = RetrofitInstance.api.getCollections("Bearer $token")

                if (responseColecoes.isSuccessful) {
                    val colecoesApi = responseColecoes.body() ?: emptyList()
                    val listaFinal = mutableListOf<ColecaoDados>()

                    for (colecaoRecebida in colecoesApi) {
                        val tituloSeguro = colecaoRecebida.titulo ?: "Sem Título"

                        // 2. Obter Fotos da Coleção
                        val responseFotos = RetrofitInstance.api.getPhotos("Bearer $token", colecaoRecebida.id)
                        val fotosApi = responseFotos.body() ?: emptyList()

                        // 3. Processar Fotos (Converter Base64 -> Ficheiro)
                        // ✅ CORREÇÃO: Usar mapIndexed para gerar nomes únicos para os ficheiros
                        val fotosCorrigidas = fotosApi.mapIndexed { index, foto ->

                            // Criamos um ID artificial para o nome do ficheiro
                            // Se o servidor mandar ID, usa o ID. Se mandar 0, usa "ColX_PosY"
                            val idParaFicheiro = if (foto.id != 0) {
                                "${foto.id}"
                            } else {
                                "Col${colecaoRecebida.id}_Pos$index"
                            }

                            val uriFinal = processarImagemDoServidor(context, foto, idParaFicheiro)

                            foto.copy(
                                uriString = uriFinal,
                                data = tituloSeguro,
                                titulo = foto.titulo ?: "Foto ${index + 1}"
                            )
                        }

                        // 4. Capa
                        val capaUrlFinal = colecaoRecebida.capaUri ?: fotosCorrigidas.firstOrNull()?.uriString

                        listaFinal.add(colecaoRecebida.copy(
                            titulo = tituloSeguro,
                            listaFotos = fotosCorrigidas,
                            capaUri = capaUrlFinal
                        ))
                    }

                    // 5. MERGE: Juntar fotos tiradas localmente que ainda não subiram (ID == 0)
                    val colecoesLocaisAntigas = _listaColecoes.value ?: emptyList()

                    for (colLocal in colecoesLocaisAntigas) {
                        // Filtra APENAS fotos que têm ID 0 E que são locais (file/content)
                        // Isto evita duplicar fotos que acabaram de vir do servidor com ID 0
                        val fotosNovasLocais = colLocal.listaFotos.filter {
                            it.id == 0 && !it.uriString.contains("server_img_")
                        }

                        val matchNaApi = listaFinal.find {
                            (colLocal.id > 0 && it.id == colLocal.id) || (it.titulo == colLocal.titulo)
                        }

                        if (fotosNovasLocais.isNotEmpty()) {
                            if (matchNaApi != null) {
                                val listaCombinada = matchNaApi.listaFotos.toMutableList()
                                listaCombinada.addAll(fotosNovasLocais)

                                val index = listaFinal.indexOf(matchNaApi)
                                listaFinal[index] = matchNaApi.copy(
                                    listaFotos = listaCombinada,
                                    capaUri = matchNaApi.capaUri ?: fotosNovasLocais.first().uriString
                                )
                            } else {
                                // Se não existe na API, adicionamos como local
                                listaFinal.add(colLocal.copy(id = 0))
                            }
                        } else if (colLocal.id == 0 && colLocal.listaFotos.isEmpty()) {
                            // Mantém coleções vazias criadas localmente
                            if (listaFinal.none { it.titulo == colLocal.titulo }) {
                                listaFinal.add(colLocal)
                            }
                        }
                    }

                    _listaColecoes.value = listaFinal
                    _listaFotos.value = listaFinal.flatMap { it.listaFotos }
                    salvarColecoesNoArmazenamento(listaFinal)

                } else {
                    Log.e("DEBUG_SYNC", "Erro API: ${responseColecoes.code()}")
                }
            } catch (e: Exception) {
                Log.e("DEBUG_SYNC", "ERRO FATAL: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Analisa a string da imagem. Se for Base64, guarda em ficheiro e retorna o caminho file://
     * ✅ CORREÇÃO: Agora aceita 'nomeUnico' (String) em vez de Int
     */
    private suspend fun processarImagemDoServidor(context: Context, foto: FotoDados, nomeUnico: String): String {
        val rawString = foto.uriString

        // Se já for URL ou ficheiro local, devolvemos igual
        if (rawString.startsWith("http") || rawString.startsWith("file:") || rawString.startsWith("content:")) {
            return rawString
        }

        // Se chegámos aqui, é Base64 (mesmo que comece por "data:image" ou não)
        return withContext(Dispatchers.IO) {
            salvarBase64EmFicheiro(context, rawString, nomeUnico)
        }
    }

    /**
     * Guarda a string Base64 num ficheiro JPG na pasta interna da app
     * ✅ CORREÇÃO: Usa 'nomeUnico' para gerar o ficheiro, evitando sobreposições
     */
    private fun salvarBase64EmFicheiro(context: Context, base64String: String, nomeUnico: String): String {
        try {
            val nomeFicheiro = "server_img_$nomeUnico.jpg"
            val ficheiro = File(context.filesDir, nomeFicheiro)

            // Se o ficheiro já existe, não precisamos de converter de novo (Cache)
            if (ficheiro.exists()) {
                return Uri.fromFile(ficheiro).toString()
            }

            // Limpa o cabeçalho se existir ("data:image/jpeg;base64,")
            val cleanBase64 = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }

            // Descodifica e guarda
            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            val fos = FileOutputStream(ficheiro)
            fos.write(decodedBytes)
            fos.close()

            return Uri.fromFile(ficheiro).toString()

        } catch (e: Exception) {
            e.printStackTrace()
            // Em caso de erro, devolvemos a string original para tentar carregar de outra forma
            return base64String
        }
    }

    // ----------------------------------------------------------------------------------
    // RESTANTES FUNÇÕES IGUAIS (Recarregar, Carregar, Salvar, Adicionar, Upload, etc.)
    // ----------------------------------------------------------------------------------

    fun recarregarDados() {
        val colecoesDoDisco = carregarColecoesDoArmazenamento()
        _listaColecoes.value = colecoesDoDisco.toList()
        _listaFotos.value = colecoesDoDisco.flatMap { it.listaFotos }
    }

    private fun carregarColecoesDoArmazenamento(): List<ColecaoDados> {
        val sharedPreferences = getApplication<Application>().getSharedPreferences("PhotoTravelPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val jsonString = sharedPreferences.getString("LISTA_COLECOES", null)
        return if (jsonString != null) {
            val type = object : TypeToken<List<ColecaoDados>>() {}.type
            try { gson.fromJson(jsonString, type) ?: emptyList() } catch (e: Exception) { emptyList() }
        } else { emptyList() }
    }

    private fun salvarColecoesNoArmazenamento(colecoes: List<ColecaoDados>) {
        val sharedPreferences = getApplication<Application>().getSharedPreferences("PhotoTravelPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val jsonString = gson.toJson(colecoes)
        editor.putString("LISTA_COLECOES", jsonString)
        editor.apply()
    }

    fun adicionarFoto(novaFoto: FotoDados) {
        val colecoesAtuais = _listaColecoes.value?.toMutableList() ?: mutableListOf()
        val nomeColecaoAlvo = novaFoto.data?.ifEmpty { "Geral" } ?: "Geral"
        val fotoParaAdicionar = novaFoto.copy(data = nomeColecaoAlvo)

        var colecaoAlvo = colecoesAtuais.find { it.titulo.equals(nomeColecaoAlvo, ignoreCase = true) }

        if (colecaoAlvo == null) {
            colecaoAlvo = ColecaoDados(id = 0, titulo = nomeColecaoAlvo, nomePersonalizado = nomeColecaoAlvo, capaUri = fotoParaAdicionar.uriString, listaFotos = mutableListOf(fotoParaAdicionar))
            colecoesAtuais.add(colecaoAlvo)
        } else {
            val listaAtualizada = colecaoAlvo.listaFotos.toMutableList()
            listaAtualizada.add(fotoParaAdicionar)
            val novaCapa = if (colecaoAlvo.capaUri == null) fotoParaAdicionar.uriString else colecaoAlvo.capaUri
            val indice = colecoesAtuais.indexOf(colecaoAlvo)
            colecoesAtuais[indice] = colecaoAlvo.copy(listaFotos = listaAtualizada, capaUri = novaCapa)
        }
        _listaColecoes.value = colecoesAtuais
        _listaFotos.value = colecoesAtuais.flatMap { it.listaFotos }
        salvarColecoesNoArmazenamento(colecoesAtuais)
    }

    fun enviarFotoComGestaoDeColecao(foto: FotoDados) {
        val context = getApplication<Application>().applicationContext
        val sharedPrefs = context.getSharedPreferences("PhotoTravelPrefs", Context.MODE_PRIVATE)
        val token = sharedPrefs.getString("USER_TOKEN", null)
        if (token == null) return

        viewModelScope.launch {
            try {
                val colecoesAtuais = _listaColecoes.value
                var idColecaoAlvo = -1

                if (colecoesAtuais.isNullOrEmpty()) {
                    val responseCheck = RetrofitInstance.api.getCollections("Bearer $token")
                    if (responseCheck.isSuccessful) {
                        val colecoesRemotas = responseCheck.body()
                        if (!colecoesRemotas.isNullOrEmpty()) idColecaoAlvo = colecoesRemotas[0].id
                    }
                } else {
                    val colecaoValida = colecoesAtuais.firstOrNull { it.id > 0 }
                    if (colecaoValida != null) idColecaoAlvo = colecaoValida.id
                }

                val idFinal = if (idColecaoAlvo == -1) {
                    val body = mapOf("title" to "Minhas Viagens", "date" to (foto.data ?: ""))
                    val responseCriar = RetrofitInstance.api.createCollection("Bearer $token", body)
                    if (responseCriar.isSuccessful && responseCriar.body() != null) {
                        val novaColecao = responseCriar.body()!!
                        sincronizarDados(context)
                        novaColecao.id
                    } else { return@launch }
                } else { idColecaoAlvo }

                enviarFotoParaApi(foto, idFinal)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun enviarFotoParaApi(foto: FotoDados, idColecao: Int) {
        val context = getApplication<Application>().applicationContext
        val sharedPrefs = context.getSharedPreferences("PhotoTravelPrefs", Context.MODE_PRIVATE)
        val token = sharedPrefs.getString("USER_TOKEN", null) ?: return

        viewModelScope.launch {
            try {
                val file = getFileFromUri(context, foto.uriString)
                if (file != null) {
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val bodyImagem = MultipartBody.Part.createFormData("image", file.name, requestFile)
                    val latBody = (foto.latitude ?: 0.0).toString().toRequestBody("text/plain".toMediaTypeOrNull())
                    val lonBody = (foto.longitude ?: 0.0).toString().toRequestBody("text/plain".toMediaTypeOrNull())
                    val colIdBody = idColecao.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                    RetrofitInstance.api.uploadPhoto("Bearer $token", bodyImagem, latBody, lonBody, colIdBody)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun getFileFromUri(context: Context, uriString: String): File? {
        return try {
            val uri = Uri.parse(uriString)
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("upload", ".jpg", context.cacheDir)
            val outputStream = FileOutputStream(tempFile)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            tempFile
        } catch (e: Exception) { null }
    }

    fun apagarColecao(colecaoParaApagar: ColecaoDados) {
        val colecoesAtuais = _listaColecoes.value?.toMutableList() ?: mutableListOf()
        colecoesAtuais.removeAll { it.titulo == colecaoParaApagar.titulo }
        _listaColecoes.value = colecoesAtuais
        _listaFotos.value = colecoesAtuais.flatMap { it.listaFotos }
        salvarColecoesNoArmazenamento(colecoesAtuais)
    }
    fun criarColecaoVazia(nome: String) {
        val colecoesAtuais = _listaColecoes.value?.toMutableList() ?: mutableListOf()
        if (colecoesAtuais.any { it.titulo.equals(nome, ignoreCase = true) }) return
        val novaColecao = ColecaoDados(titulo = nome, nomePersonalizado = nome, capaUri = null, listaFotos = emptyList(), id = 0)
        colecoesAtuais.add(novaColecao)
        _listaColecoes.value = colecoesAtuais
        salvarColecoesNoArmazenamento(colecoesAtuais)
    }

    fun renomearColecao(nomeAntigoColecao: String, novoTitulo: String) {
        if (novoTitulo.isBlank() || nomeAntigoColecao.isBlank()) return
        val colecoesAtuais = _listaColecoes.value?.toMutableList() ?: return
        if (colecoesAtuais.any { it.titulo.equals(novoTitulo, ignoreCase = true) && !it.titulo.equals(nomeAntigoColecao, ignoreCase = true) }) return
        val indiceColecao = colecoesAtuais.indexOfFirst { it.titulo.equals(nomeAntigoColecao, ignoreCase = true) }
        if (indiceColecao == -1) return
        val colecaoAntiga = colecoesAtuais[indiceColecao]
        val fotosAtualizadas = colecaoAntiga.listaFotos.map { it.copy(data = novoTitulo) }
        colecoesAtuais[indiceColecao] = colecaoAntiga.copy(titulo = novoTitulo, listaFotos = fotosAtualizadas)
        _listaColecoes.value = colecoesAtuais
        _listaFotos.value = colecoesAtuais.flatMap { it.listaFotos }
        salvarColecoesNoArmazenamento(colecoesAtuais)
    }

    fun moverFotoParaColecao(fotoParaMover: FotoDados, colecaoDestino: ColecaoDados) {
        val colecoesAtuais = _listaColecoes.value?.toMutableList() ?: return
        val indiceOrigem = colecoesAtuais.indexOfFirst { it.titulo == fotoParaMover.data }
        if (indiceOrigem != -1) {
            val colecaoOrigem = colecoesAtuais[indiceOrigem]
            val fotosOrigemAtualizadas = colecaoOrigem.listaFotos.toMutableList()
            fotosOrigemAtualizadas.removeAll { it.uriString == fotoParaMover.uriString }
            val novaCapaOrigem = if (fotosOrigemAtualizadas.isEmpty()) null
            else if (colecaoOrigem.capaUri == fotoParaMover.uriString) fotosOrigemAtualizadas.first().uriString
            else colecaoOrigem.capaUri
            colecoesAtuais[indiceOrigem] = colecaoOrigem.copy(listaFotos = fotosOrigemAtualizadas, capaUri = novaCapaOrigem)
        }
        val indiceDestino = colecoesAtuais.indexOfFirst { it.titulo == colecaoDestino.titulo }
        if (indiceDestino != -1) {
            val fotoMovida = fotoParaMover.copy(data = colecaoDestino.titulo ?: "")
            val colecaoDestinoOriginal = colecoesAtuais[indiceDestino]
            val fotosDestinoAtualizadas = colecaoDestinoOriginal.listaFotos.toMutableList()
            fotosDestinoAtualizadas.add(fotoMovida)
            val novaCapaDestino = colecaoDestinoOriginal.capaUri ?: fotoMovida.uriString
            colecoesAtuais[indiceDestino] = colecaoDestinoOriginal.copy(listaFotos = fotosDestinoAtualizadas, capaUri = novaCapaDestino)
        }
        _listaColecoes.value = colecoesAtuais.toList()
        _listaFotos.value = colecoesAtuais.flatMap { it.listaFotos }
        salvarColecoesNoArmazenamento(colecoesAtuais)
    }

    fun renomearFoto(fotoParaRenomear: FotoDados, novoNome: String) {
        val colecoesAtuais = _listaColecoes.value?.toMutableList() ?: return
        val indiceColecao = colecoesAtuais.indexOfFirst { it.titulo == fotoParaRenomear.data }
        if (indiceColecao == -1) return
        val colecaoAlvo = colecoesAtuais[indiceColecao]
        val indiceFoto = colecaoAlvo.listaFotos.indexOfFirst { it.uriString == fotoParaRenomear.uriString }
        if (indiceFoto == -1) return
        val fotoAtualizada = colecaoAlvo.listaFotos[indiceFoto].copy(tituloPersonalizado = novoNome)
        val fotosNovasDaColecao = colecaoAlvo.listaFotos.toMutableList()
        fotosNovasDaColecao[indiceFoto] = fotoAtualizada
        colecoesAtuais[indiceColecao] = colecaoAlvo.copy(listaFotos = fotosNovasDaColecao)
        _listaColecoes.value = colecoesAtuais
        _listaFotos.value = colecoesAtuais.flatMap { it.listaFotos }
        salvarColecoesNoArmazenamento(colecoesAtuais)
    }

    fun apagarFoto(fotoParaApagar: FotoDados) {
        val colecoesAtuais = _listaColecoes.value?.map { it.copy() }?.toMutableList() ?: return
        val indice = colecoesAtuais.indexOfFirst { it.titulo == fotoParaApagar.data }
        if (indice != -1) {
            val colecao = colecoesAtuais[indice]
            val fotosNovas = colecao.listaFotos.filter { it.uriString != fotoParaApagar.uriString }
            val novaCapa = when {
                fotosNovas.isEmpty() -> null
                colecao.capaUri == fotoParaApagar.uriString -> fotosNovas.firstOrNull()?.uriString
                else -> colecao.capaUri
            }
            colecoesAtuais[indice] = colecao.copy(listaFotos = fotosNovas, capaUri = novaCapa)
            _listaColecoes.value = colecoesAtuais.toList()
            _listaFotos.value = colecoesAtuais.flatMap { it.listaFotos }
            salvarColecoesNoArmazenamento(colecoesAtuais)
        }
    }
}