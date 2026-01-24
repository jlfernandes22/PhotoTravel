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

        viewModelScope.launch(Dispatchers.IO) {
            val colecoesIniciais = carregarColecoesDoArmazenamento()
            withContext(Dispatchers.Main) {
                _listaColecoes.value = colecoesIniciais
                _listaFotos.value = colecoesIniciais.flatMap { it.listaFotos }
            }
        }
    }

    // ----------------------------------------------------------------------------------
    // SINCRONIZAÇÃO
    // ----------------------------------------------------------------------------------
    fun sincronizarDados(context: Context) {
        Log.d("DEBUG_SYNC", "=== INÍCIO DA SINCRONIZAÇÃO ===")
        val sharedPrefs = context.getSharedPreferences("PhotoTravelPrefs", Context.MODE_PRIVATE)
        val token = sharedPrefs.getString("USER_TOKEN", null)

        if (token == null) {
            Log.e("DEBUG_SYNC", "Token não encontrado. A abortar sincronização.")
            return
        } else {
            Log.d("DEBUG_SYNC", "Token encontrado. A iniciar pedidos à API...")
        }

        viewModelScope.launch {
            try {
                // 1. Pedir Coleções
                Log.d("DEBUG_SYNC", "Pedindo coleções ao servidor...")
                val responseColecoes = RetrofitInstance.api.getCollections("Bearer $token")

                if (responseColecoes.isSuccessful) {
                    val colecoesApi = responseColecoes.body() ?: emptyList()
                    Log.d("DEBUG_SYNC", "Recebidas ${colecoesApi.size} coleções do servidor.")

                    val listaFinal = mutableListOf<ColecaoDados>()

                    for (colecaoRecebida in colecoesApi) {
                        Log.d("DEBUG_SYNC", "Processando coleção ID: ${colecaoRecebida.id} | Título Original: ${colecaoRecebida.titulo}")

                        // 2. Pedir Fotos da Coleção
                        val responseFotos = RetrofitInstance.api.getPhotos("Bearer $token", colecaoRecebida.id)
                        val fotosApi = responseFotos.body() ?: emptyList()
                        Log.d("DEBUG_SYNC", "Recebidas ${fotosApi.size} fotos para esta coleção.")

                        // Lógica do Título
                        var tituloProvisorio = colecaoRecebida.titulo
                        if (tituloProvisorio.isNullOrBlank()) {
                            val primeiraFoto = fotosApi.firstOrNull()
                            tituloProvisorio = if (primeiraFoto?.data?.isNotBlank() == true) primeiraFoto.data else "Sem Título"
                        }
                        val tituloSeguro = tituloProvisorio ?: "Sem Título"

                        // Mapear Fotos
                        val fotosCorrigidas = fotosApi.map { foto ->
                            val nomeUnicoFicheiro = "${foto.id}"
                            val uriFinal = processarImagemDoServidor(context, foto, nomeUnicoFicheiro)

                            // Log detalhado apenas se for uma foto nova ou específica (para não spammar muito)
                            // Log.v("DEBUG_SYNC", " Foto ID ${foto.id} -> URI: $uriFinal")

                            foto.copy(
                                uriString = uriFinal,
                                data = tituloSeguro,
                                titulo = foto.titulo ?: "Foto ${foto.id}",
                                sincronizada = true
                            )
                        }

                        val capaUrlFinal = colecaoRecebida.capaUri ?: fotosCorrigidas.firstOrNull()?.uriString
                        listaFinal.add(colecaoRecebida.copy(titulo = tituloSeguro, listaFotos = fotosCorrigidas, capaUri = capaUrlFinal))
                    }

                    // 3. MERGE (Fotos Locais)
                    Log.d("DEBUG_SYNC", "Iniciando Merge com dados locais...")
                    val colecoesLocaisAntigas = _listaColecoes.value ?: emptyList()
                    Log.d("DEBUG_SYNC", "Temos ${colecoesLocaisAntigas.size} coleções locais em memória.")

                    for (colLocal in colecoesLocaisAntigas) {
                        // Filtra pendentes
                        val fotosPendentes = colLocal.listaFotos.filter {
                            !it.sincronizada && !it.uriString.contains("server_img_")
                        }

                        if (fotosPendentes.isNotEmpty()) {
                            Log.d("DEBUG_SYNC", "Encontradas ${fotosPendentes.size} fotos pendentes na coleção '${colLocal.titulo}' (ID Local: ${colLocal.id})")
                        }

                        val matchNaApi = listaFinal.find { (colLocal.id > 0 && it.id == colLocal.id) || (it.titulo == colLocal.titulo) }

                        if (fotosPendentes.isNotEmpty()) {
                            if (matchNaApi != null) {
                                Log.d("DEBUG_SYNC", "Match encontrado na API (ID ${matchNaApi.id}). A juntar fotos...")
                                val listaCombinada = matchNaApi.listaFotos.toMutableList()

                                var adicionadasCount = 0
                                for (fPendente in fotosPendentes) {
                                    if (listaCombinada.none { it.uriString == fPendente.uriString }) {
                                        listaCombinada.add(fPendente)
                                        adicionadasCount++
                                    }
                                }
                                Log.d("DEBUG_SYNC", " ➕ Adicionadas $adicionadasCount fotos locais à coleção da API.")

                                val index = listaFinal.indexOf(matchNaApi)
                                listaFinal[index] = matchNaApi.copy(
                                    listaFotos = listaCombinada,
                                    capaUri = matchNaApi.capaUri ?: fotosPendentes.first().uriString
                                )
                            } else {
                                Log.d("DEBUG_SYNC", "      🆕 Coleção local '${colLocal.titulo}' não existe na API. Adicionando como nova (ID 0).")
                                listaFinal.add(colLocal.copy(id = 0, listaFotos = fotosPendentes))
                            }
                        } else if (colLocal.id == 0 && colLocal.listaFotos.isEmpty()) {
                            if (listaFinal.none { it.titulo == colLocal.titulo }) {
                                Log.d("DEBUG_SYNC", "Mantendo coleção vazia local '${colLocal.titulo}'.")
                                listaFinal.add(colLocal)
                            }
                        }
                    }

                    // Atualizar UI
                    Log.d("DEBUG_SYNC", "Sincronização terminada. Atualizando LiveData.")
                    Log.d("DEBUG_SYNC", "Total Coleções Final: ${listaFinal.size}")
                    Log.d("DEBUG_SYNC", "Total Fotos Final: ${listaFinal.flatMap { it.listaFotos }.size}")

                    _listaColecoes.value = listaFinal
                    _listaFotos.value = listaFinal.flatMap { it.listaFotos }
                    salvarColecoesNoArmazenamento(listaFinal)

                } else {
                    Log.e("DEBUG_SYNC", "Erro na API (Coleções): Código ${responseColecoes.code()} - ${responseColecoes.message()}")
                }
            } catch (e: Exception) {
                Log.e("DEBUG_SYNC", "EXCEÇÃO FATAL: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private suspend fun processarImagemDoServidor(context: Context, foto: FotoDados, nomeUnico: String): String {
        val rawString = foto.uriString
        if (rawString.startsWith("http") || rawString.startsWith("file:") || rawString.startsWith("content:")) return rawString
        return withContext(Dispatchers.IO) { salvarBase64EmFicheiro(context, rawString, nomeUnico) }
    }

    private fun salvarBase64EmFicheiro(context: Context, base64String: String, nomeUnico: String): String {
        try {
            val nomeFicheiro = "server_img_$nomeUnico.jpg"
            val ficheiro = File(context.filesDir, nomeFicheiro)
            if (ficheiro.exists()) return Uri.fromFile(ficheiro).toString()

            val cleanBase64 = if (base64String.contains(",")) base64String.substringAfter(",") else base64String
            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            val fos = FileOutputStream(ficheiro)
            fos.write(decodedBytes)
            fos.close()
            return Uri.fromFile(ficheiro).toString()
        } catch (e: Exception) { return base64String }
    }

    // ----------------------------------------------------------------------------------
    // FUNÇÕES DE UPLOAD
    // ----------------------------------------------------------------------------------

    fun adicionarFoto(novaFoto: FotoDados) {
        val colecoesAtuais = _listaColecoes.value?.toMutableList() ?: mutableListOf()

        val todasAsFotos = colecoesAtuais.flatMap { it.listaFotos }
        if (todasAsFotos.any { it.uriString == novaFoto.uriString }) return

        val nomeColecaoAlvo = novaFoto.data?.ifEmpty { "Geral" } ?: "Geral"
        val fotoParaAdicionar = novaFoto.copy(data = nomeColecaoAlvo, sincronizada = false) // Garante que começa como false

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
        val token = sharedPrefs.getString("USER_TOKEN", null) ?: return

        viewModelScope.launch {
            try {
                val colecoesAtuais = _listaColecoes.value ?: emptyList()
                val tituloAlvo = foto.data.ifNullOrBlank { "Nova Coleção" }
                var idColecaoAlvo = -1

                // 1. Procura Localmente
                val colecaoLocalMatch = colecoesAtuais.find { it.titulo.equals(tituloAlvo, ignoreCase = true) }
                if (colecaoLocalMatch != null && colecaoLocalMatch.id > 0) {
                    idColecaoAlvo = colecaoLocalMatch.id
                } else {
                    // 2. Procura no Servidor
                    val responseCheck = RetrofitInstance.api.getCollections("Bearer $token")
                    if (responseCheck.isSuccessful) {
                        val colecoesRemotas = responseCheck.body() ?: emptyList()
                        val matchServidor = colecoesRemotas.find { it.titulo.equals(tituloAlvo, ignoreCase = true) }
                        if (matchServidor != null) idColecaoAlvo = matchServidor.id
                    }
                }

                // 3. Cria se não existir
                val idFinal = if (idColecaoAlvo == -1) {
                    val body = mapOf("title" to tituloAlvo, "date" to (foto.data ?: ""))
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

    private fun String?.ifNullOrBlank(defaultValue: () -> String): String {
        return if (this.isNullOrBlank()) defaultValue() else this
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

                    val res = RetrofitInstance.api.uploadPhoto("Bearer $token", bodyImagem, latBody, lonBody, colIdBody)

                    if(res.isSuccessful){
                        Log.d("UPLOAD", "Foto enviada com sucesso! A marcar como sincronizada...")
                        // ✅ CORREÇÃO: Marca a foto local como sincronizada para não duplicar no próximo sync
                        marcarFotoComoSincronizada(foto)
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    /**
     * Atualiza a foto local para 'sincronizada = true'
     */
    private fun marcarFotoComoSincronizada(foto: FotoDados) {
        val colecoesAtuais = _listaColecoes.value?.toMutableList() ?: return

        // Encontra a coleção
        val indiceColecao = colecoesAtuais.indexOfFirst { it.titulo.equals(foto.data, ignoreCase = true) }
        if (indiceColecao == -1) return

        val colecao = colecoesAtuais[indiceColecao]
        val indiceFoto = colecao.listaFotos.indexOfFirst { it.uriString == foto.uriString }
        if (indiceFoto == -1) return

        // Atualiza a foto
        val fotosAtualizadas = colecao.listaFotos.toMutableList()
        fotosAtualizadas[indiceFoto] = fotosAtualizadas[indiceFoto].copy(sincronizada = true)

        colecoesAtuais[indiceColecao] = colecao.copy(listaFotos = fotosAtualizadas)

        _listaColecoes.value = colecoesAtuais
        _listaFotos.value = colecoesAtuais.flatMap { it.listaFotos }
        salvarColecoesNoArmazenamento(colecoesAtuais)
    }

    // (O resto das funções auxiliares mantém-se igual: getFileFromUri, apagarColecao, etc.)
    // Copia-as do ficheiro anterior se necessário.
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

    /**
     * Corre em background
     */
    fun recarregarDados() {
        viewModelScope.launch(Dispatchers.IO) {
            val colecoesDoDisco = carregarColecoesDoArmazenamento()

            // Volta à thread principal apenas para atualizar o ecrã
            withContext(Dispatchers.Main) {
                _listaColecoes.value = colecoesDoDisco.toList()
                _listaFotos.value = colecoesDoDisco.flatMap { it.listaFotos }
            }
        }
    }
    /**
     * Lê o JSON guardado nas SharedPreferences.
     * Pode ser chamada diretamente dentro de um contexto IO.
     */
    private fun carregarColecoesDoArmazenamento(): List<ColecaoDados> {
        val sharedPreferences = getApplication<Application>().getSharedPreferences("PhotoTravelPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val jsonString = sharedPreferences.getString("LISTA_COLECOES", null)

        return if (jsonString != null) {
            val type = object : TypeToken<List<ColecaoDados>>() {}.type
            try {
                gson.fromJson(jsonString, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    private fun salvarColecoesNoArmazenamento(colecoes: List<ColecaoDados>) {
        viewModelScope.launch(Dispatchers.IO) {
            val sharedPreferences = getApplication<Application>().getSharedPreferences("PhotoTravelPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            val gson = Gson()
            val jsonString = gson.toJson(colecoes)
            editor.putString("LISTA_COLECOES", jsonString)
            editor.commit() // commit() é síncrono, mas como estamos em Dispatchers.IO, é seguro e garante escrita.
        }
    }

    // Funções de gestão
    fun apagarColecao(colecaoParaApagar: ColecaoDados) {
        val context = getApplication<Application>().applicationContext

        viewModelScope.launch {
            // 1. Apagar do Servidor (1 pedido único)
            if (colecaoParaApagar.id > 0) {
                val sharedPrefs = context.getSharedPreferences("PhotoTravelPrefs", Context.MODE_PRIVATE)
                val token = sharedPrefs.getString("USER_TOKEN", null)

                if (token != null) {
                    try {
                        // Certifica-te que tens deleteCollection no ApiService
                        val response = RetrofitInstance.api.deleteCollection("Bearer $token", colecaoParaApagar.id)
                        if (response.isSuccessful) {
                            Log.d("DELETE_COL", "Coleção e fotos apagadas do servidor com sucesso.")
                        }
                    } catch (e: Exception) {
                        Log.e("DELETE_COL", "Erro de rede: ${e.message}")
                    }
                }
            }

            // 2. Limpar tudo localmente (memória e ficheiros)
            withContext(Dispatchers.Main) {
                // ... (Código igual: remove da lista e apaga ficheiros file://)
                val colecoesAtuais = _listaColecoes.value?.toMutableList() ?: return@withContext
                colecoesAtuais.removeAll { it.id == colecaoParaApagar.id || it.titulo == colecaoParaApagar.titulo }

                // Apagar ficheiros físicos
                colecaoParaApagar.listaFotos.forEach {
                    if (it.uriString.startsWith("file:")) File(Uri.parse(it.uriString).path ?: "").delete()
                }

                _listaColecoes.value = colecoesAtuais
                _listaFotos.value = colecoesAtuais.flatMap { it.listaFotos }
                salvarColecoesNoArmazenamento(colecoesAtuais)
            }
        }
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

        // Verifica se já existe outra com esse nome (para evitar duplicados visuais)
        if (colecoesAtuais.any { it.titulo.equals(novoTitulo, ignoreCase = true) && !it.titulo.equals(nomeAntigoColecao, ignoreCase = true) }) return

        val indiceColecao = colecoesAtuais.indexOfFirst { it.titulo.equals(nomeAntigoColecao, ignoreCase = true) }
        if (indiceColecao == -1) return

        val colecaoAntiga = colecoesAtuais[indiceColecao]

        // =================================================================================
        // 1. ATUALIZAR NO SERVIDOR
        // =================================================================================
        if (colecaoAntiga.id > 0) {
            viewModelScope.launch {
                val sharedPrefs = getApplication<Application>().getSharedPreferences("PhotoTravelPrefs", Context.MODE_PRIVATE)
                val token = sharedPrefs.getString("USER_TOKEN", null)

                if (token != null) {
                    try {
                        val body = mapOf("newTitle" to novoTitulo)
                        val response = RetrofitInstance.api.updateCollection("Bearer $token", colecaoAntiga.id, body)

                        if (response.isSuccessful) {
                            Log.d("RENAME", "Coleção renomeada no servidor com sucesso.")
                        } else {
                            Log.e("RENAME", "Erro ao renomear no servidor: ${response.code()}")
                        }
                    } catch (e: Exception) {
                        Log.e("RENAME", "Falha de rede ao renomear: ${e.message}")
                    }
                }
            }
        }

        // =================================================================================
        // 2. ATUALIZAR LOCALMENTE
        // =================================================================================

        // Atualiza o campo 'data' nas fotos locais (que é onde a App guarda o nome do álbum)
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
            val novaCapaOrigem = if (fotosOrigemAtualizadas.isEmpty()) null else if (colecaoOrigem.capaUri == fotoParaMover.uriString) fotosOrigemAtualizadas.first().uriString else colecaoOrigem.capaUri
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
        val context = getApplication<Application>().applicationContext

        viewModelScope.launch {
            // 1. Se a foto tem ID > 0, significa que existe no servidor
            if (fotoParaApagar.id > 0) {
                val sharedPrefs = context.getSharedPreferences("PhotoTravelPrefs", Context.MODE_PRIVATE)
                val token = sharedPrefs.getString("USER_TOKEN", null)

                if (token != null) {
                    try {
                        val response = RetrofitInstance.api.deletePhoto("Bearer $token", fotoParaApagar.id)
                        if (response.isSuccessful) {
                            Log.d("DELETE", "Foto ${fotoParaApagar.id} apagada do servidor.")
                        } else {
                            Log.e("DELETE", "Erro ao apagar do servidor: ${response.code()}")
                            // Opcional: Podes decidir 'return@launch' aqui se não quiseres apagar localmente em caso de erro
                        }
                    } catch (e: Exception) {
                        Log.e("DELETE", "Falha na comunicação: ${e.message}")
                    }
                }
            }

            // 2. Apagar Localmente (executa sempre, quer esteja no servidor ou não)
            // (Esta lógica é a mesma que já tinhas, apenas movida para dentro do viewModelScope)

            // É preciso usar 'postValue' ou fazer na Main Thread porque estamos numa corrotina
            withContext(Dispatchers.Main) {
                val colecoesAtuais = _listaColecoes.value?.map { it.copy() }?.toMutableList() ?: return@withContext
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

                    // Apagar ficheiro físico local para poupar espaço
                    try {
                        if (fotoParaApagar.uriString.startsWith("file:")) {
                            val ficheiro = File(Uri.parse(fotoParaApagar.uriString).path ?: "")
                            if (ficheiro.exists()) ficheiro.delete()
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }
    }
}