// Em pt/ipt/dam2025/phototravel/viewmodel/PartilhaDadosViewModel.kt

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

class PartilhaDadosViewModel(application: Application) : AndroidViewModel(application) {

    // 1. LiveData para a lista de COLEÇÕES
    private val _listaColecoes = MutableLiveData<List<ColecaoDados>>()
    val listaColecoes: LiveData<List<ColecaoDados>> get() = _listaColecoes

    // 2. LiveData para a lista de TODAS AS FOTOS
    private val _listaFotos = MutableLiveData<List<FotoDados>>()
    val listaFotos: LiveData<List<FotoDados>> get() = _listaFotos

    init {
        // Carrega os dados persistidos quando o ViewModel é criado
        val colecoesIniciais = carregarColecoesDoArmazenamento()
        _listaColecoes.value = colecoesIniciais
        // Inicializa a lista de fotos a partir das coleções carregadas
        _listaFotos.value = colecoesIniciais.flatMap { it.listaFotos }
    }

    /**
     * Adiciona uma nova foto. Se a coleção não for especificada,
     * é adicionada a uma coleção padrão chamada "Geral".
     */
    fun adicionarFoto(novaFoto: FotoDados) {
        val colecoesAtuais = _listaColecoes.value?.toMutableList() ?: mutableListOf()
        val nomeColecaoAlvo = novaFoto.data.ifEmpty { "Geral" }
        val fotoParaAdicionar = novaFoto.copy(data = nomeColecaoAlvo)

        var colecaoAlvo = colecoesAtuais.find { it.titulo.equals(nomeColecaoAlvo, ignoreCase = true) }

        if (colecaoAlvo == null) {
            // Se a coleção não existe, cria com a foto como capa
            colecaoAlvo = ColecaoDados(
                titulo = nomeColecaoAlvo,
                listaFotos = mutableListOf(fotoParaAdicionar),
                capaUri = fotoParaAdicionar.uriString
            )
            colecoesAtuais.add(colecaoAlvo)
        } else {
            // Se a coleção já existe (pode estar vazia ou ter fotos)
            val listaAtualizada = colecaoAlvo.listaFotos.toMutableList()
            listaAtualizada.add(fotoParaAdicionar)

            // ✅ REGRA DE OURO:
            // Se a capaUri for null (coleção vazia), a nova foto torna-se a capa.
            // Se já existia uma capa, mantemos a que lá estava.
            val novaCapa = if (colecaoAlvo.capaUri == null) {
                fotoParaAdicionar.uriString
            } else {
                colecaoAlvo.capaUri
            }

            val indice = colecoesAtuais.indexOf(colecaoAlvo)
            colecoesAtuais[indice] = colecaoAlvo.copy(
                listaFotos = listaAtualizada,
                capaUri = novaCapa // Atualizamos a capa aqui!
            )
        }

        _listaColecoes.value = colecoesAtuais
        _listaFotos.value = colecoesAtuais.flatMap { it.listaFotos }
        salvarColecoesNoArmazenamento(colecoesAtuais)
    }
    /**
     * Apaga uma foto específica de todas as listas e da persistência.
     */
    /**
     * Apaga uma foto específica de todas as listas e da persistência.
     */fun apagarFoto(fotoParaApagar: FotoDados) {
        val colecoesAtuais = _listaColecoes.value?.map { it.copy() }?.toMutableList() ?: return
        val indice = colecoesAtuais.indexOfFirst { it.titulo == fotoParaApagar.data }

        if (indice != -1) {
            val colecao = colecoesAtuais[indice]
            val fotosNovas = colecao.listaFotos.filter { it.uriString != fotoParaApagar.uriString }

            // Se a foto apagada era a capa, ou se a coleção ficou vazia, atualizamos a capaUri
            val novaCapa = when {
                fotosNovas.isEmpty() -> null
                colecao.capaUri == fotoParaApagar.uriString -> fotosNovas.first().uriString
                else -> colecao.capaUri
            }

            colecoesAtuais[indice] = colecao.copy(
                listaFotos = fotosNovas,
                capaUri = novaCapa
            )

            // ✅ Atualizamos o valor com uma lista TOTALMENTE NOVA (.toList())
            _listaColecoes.value = colecoesAtuais.toList()
            _listaFotos.value = colecoesAtuais.flatMap { it.listaFotos }
            salvarColecoesNoArmazenamento(colecoesAtuais)
        }
    }

    fun criarColecaoVazia(nome: String) {
        val colecoesAtuais = _listaColecoes.value?.toMutableList() ?: mutableListOf()

        // Verifica se já existe uma coleção com esse nome para evitar duplicados
        if (colecoesAtuais.any { it.titulo.equals(nome, ignoreCase = true) }) {
            return
        }

        val novaColecao = ColecaoDados(
            titulo = nome,              // Usamos o nome como ID único (titulo)
            nomePersonalizado = nome,
            capaUri = null,             // Começa sem foto
            listaFotos = emptyList()    // Lista vazia
        )

        colecoesAtuais.add(novaColecao)
        _listaColecoes.value = colecoesAtuais
        salvarColecoesNoArmazenamento(colecoesAtuais)
    }

    /**
     * Apaga uma coleção inteira e todas as fotos contidas nela.
     */
    fun apagarColecao(colecaoParaApagar: ColecaoDados) {
        val colecoesAtuais = _listaColecoes.value?.toMutableList() ?: mutableListOf()

        // Remove a coleção da lista principal
        colecoesAtuais.removeAll { it.titulo == colecaoParaApagar.titulo }

        // Atualiza os LiveData e salva as alterações
        _listaColecoes.value = colecoesAtuais
        _listaFotos.value = colecoesAtuais.flatMap { it.listaFotos }
        salvarColecoesNoArmazenamento(colecoesAtuais)
    }



    /**
     * Renomeia uma coleção.
     * Isto requer atualizar o 'data' (ID da coleção) em cada foto dentro dela.
     */
    /**
     * Renomeia uma coleção.
     * Isto requer atualizar o 'data' (ID da coleção) em cada foto dentro dela.
     */
    // CORREÇÃO: O primeiro parâmetro deve ser o nome antigo da coleção (String)
    fun renomearColecao(nomeAntigoColecao: String, novoTitulo: String) {
        if (novoTitulo.isBlank() || nomeAntigoColecao.isBlank()) return // Evita nomes vazios

        val colecoesAtuais = _listaColecoes.value?.toMutableList() ?: return

        // CORREÇÃO: A comparação aqui deve ser entre strings
        // Verifica se o novo título já existe e não é o nome da coleção que estamos a renomear
        if (colecoesAtuais.any { it.titulo.equals(novoTitulo, ignoreCase = true) && !it.titulo.equals(nomeAntigoColecao, ignoreCase = true) }) {
            // Opcional: Informar o utilizador que o nome já existe
            return
        }

        // CORREÇÃO: Usa o nome antigo para encontrar a coleção
        val indiceColecao = colecoesAtuais.indexOfFirst { it.titulo.equals(nomeAntigoColecao, ignoreCase = true) }
        if (indiceColecao == -1) return // Coleção não encontrada

        val colecaoAntiga = colecoesAtuais[indiceColecao]

        // 1. Cria uma nova lista de fotos (imutável) com o 'data' atualizado para o novo título
        val fotosAtualizadas: List<FotoDados> = colecaoAntiga.listaFotos.map { foto ->
            foto.copy(data = novoTitulo)
        }

        // 2. Cria uma nova coleção com o novo título e a lista de fotos atualizada
        // CORREÇÃO: `fotosAtualizadas` já é uma List<FotoDados>, não precisa de casting.
        // O tipo em ColecaoDados deve ser List<FotoDados> para garantir imutabilidade.
        val colecaoNova = colecaoAntiga.copy(
            titulo = novoTitulo,
            listaFotos = fotosAtualizadas
        )

        // 3. Substitui a coleção antiga pela nova na lista principal
        colecoesAtuais[indiceColecao] = colecaoNova

        // 4. Atualiza os LiveData e salva as alterações
        _listaColecoes.value = colecoesAtuais
        _listaFotos.value = colecoesAtuais.flatMap { it.listaFotos }
        salvarColecoesNoArmazenamento(colecoesAtuais)
    }


    /**
     * Renomeia o título personalizado de uma foto.
     */
    fun renomearFoto(fotoParaRenomear: FotoDados, novoNome: String) {
        val colecoesAtuais = _listaColecoes.value?.toMutableList() ?: return

        // Encontra a coleção onde a foto está
        val indiceColecao = colecoesAtuais.indexOfFirst { it.titulo == fotoParaRenomear.data }
        if (indiceColecao == -1) return

        val colecaoAlvo = colecoesAtuais[indiceColecao]

        // Encontra a foto dentro da coleção
        val indiceFoto = colecaoAlvo.listaFotos.indexOfFirst { it.uriString == fotoParaRenomear.uriString }
        if (indiceFoto == -1) return

        // Cria uma cópia da foto com o nome atualizado
        val fotoAtualizada = colecaoAlvo.listaFotos[indiceFoto].copy(tituloPersonalizado = novoNome)

        // Cria uma nova lista de fotos para a coleção, substituindo a foto antiga
        val fotosNovasDaColecao = colecaoAlvo.listaFotos.toMutableList()
        fotosNovasDaColecao[indiceFoto] = fotoAtualizada

        // Cria uma cópia da coleção com a lista de fotos atualizada
        colecoesAtuais[indiceColecao] = colecaoAlvo.copy(listaFotos = fotosNovasDaColecao)

        // Atualiza os LiveData e salva
        _listaColecoes.value = colecoesAtuais
        _listaFotos.value = colecoesAtuais.flatMap { it.listaFotos }
        salvarColecoesNoArmazenamento(colecoesAtuais)
    }

    /**
     * Move uma foto de uma coleção para outra.
     */fun moverFotoParaColecao(fotoParaMover: FotoDados, colecaoDestino: ColecaoDados) {
        val colecoesAtuais = _listaColecoes.value?.toMutableList() ?: return

        // --- Passo 1: Remover da origem ---
        val indiceOrigem = colecoesAtuais.indexOfFirst { it.titulo == fotoParaMover.data }
        if (indiceOrigem != -1) {
            val colecaoOrigem = colecoesAtuais[indiceOrigem]
            val fotosOrigemAtualizadas = colecaoOrigem.listaFotos.toMutableList()
            fotosOrigemAtualizadas.removeAll { it.uriString == fotoParaMover.uriString }

            // Se removemos a foto que era a capa, precisamos de uma nova capa ou null
            val novaCapaOrigem = if (fotosOrigemAtualizadas.isEmpty()) null
            else if (colecaoOrigem.capaUri == fotoParaMover.uriString) fotosOrigemAtualizadas.first().uriString
            else colecaoOrigem.capaUri

            colecoesAtuais[indiceOrigem] = colecaoOrigem.copy(
                listaFotos = fotosOrigemAtualizadas,
                capaUri = novaCapaOrigem
            )
        }

        // --- Passo 2: Adicionar ao destino ---
        val indiceDestino = colecoesAtuais.indexOfFirst { it.titulo == colecaoDestino.titulo }
        if (indiceDestino != -1) {
            val fotoMovida = fotoParaMover.copy(data = colecaoDestino.titulo)
            val colecaoDestinoOriginal = colecoesAtuais[indiceDestino]
            val fotosDestinoAtualizadas = colecaoDestinoOriginal.listaFotos.toMutableList()
            fotosDestinoAtualizadas.add(fotoMovida)

            // ✅ CORREÇÃO AQUI: Se a coleção destino não tinha capa, ela ganha esta foto como capa
            val novaCapaDestino = colecaoDestinoOriginal.capaUri ?: fotoMovida.uriString

            colecoesAtuais[indiceDestino] = colecaoDestinoOriginal.copy(
                listaFotos = fotosDestinoAtualizadas,
                capaUri = novaCapaDestino
            )
        }

        _listaColecoes.value = colecoesAtuais.toList() // .toList() força a atualização do LiveData
        salvarColecoesNoArmazenamento(colecoesAtuais)
    }

    // --- Funções de Persistência (sem alterações) ---

    private fun carregarColecoesDoArmazenamento(): List<ColecaoDados> {
        val sharedPreferences = getApplication<Application>().getSharedPreferences("PhotoTravelPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val jsonString = sharedPreferences.getString("LISTA_COLECOES", null)

        return if (jsonString != null) {
            val type = object : TypeToken<List<ColecaoDados>>() {}.type
            try {
                gson.fromJson(jsonString, type) ?: emptyList()
            } catch (e: Exception) {
                // Em caso de erro na deserialização, retorna uma lista vazia para evitar crash
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    private fun salvarColecoesNoArmazenamento(colecoes: List<ColecaoDados>) {
        val sharedPreferences = getApplication<Application>().getSharedPreferences("PhotoTravelPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val jsonString = gson.toJson(colecoes)
        editor.putString("LISTA_COLECOES", jsonString)
        editor.apply()
    }

    // Dentro do teu PartilhaDadosViewModel.kt, adiciona esta função:

    fun recarregarDados() {
        val colecoesDoDisco = carregarColecoesDoArmazenamento()
        // Ao atribuir uma lista nova, o LiveData dispara obrigatoriamente
        _listaColecoes.value = colecoesDoDisco.toList()
        _listaFotos.value = colecoesDoDisco.flatMap { it.listaFotos }
    }
}
