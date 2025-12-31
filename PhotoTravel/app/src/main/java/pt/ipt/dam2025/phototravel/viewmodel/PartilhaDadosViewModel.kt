package pt.ipt.dam2025.phototravel.viewmodel

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import pt.ipt.dam2025.phototravel.modelos.ColecaoDados
import pt.ipt.dam2025.phototravel.modelos.FotoDados

class PartilhaDadosViewModel(application: Application) : AndroidViewModel(application) {

    // --- LISTA DE FOTOS (COMO ANTES) ---
    private val _listaFotos = MutableLiveData<MutableList<FotoDados>>(mutableListOf())
    val listaFotos: LiveData<MutableList<FotoDados>> get() = _listaFotos

    // --- LISTA DE COLEÇÕES (NOVO) ---
    // Esta é a lista que o ColecoesFragmento vai observar.
    private val _listaColecoes = MutableLiveData<List<ColecaoDados>>()
    val listaColecoes: LiveData<List<ColecaoDados>> get() = _listaColecoes

    private val gson = Gson()
    // Nome do ficheiro para guardar as fotos
    private val prefsFotos = application.getSharedPreferences("TravelPhoto", Context.MODE_PRIVATE)
    // Novo ficheiro, dedicado a guardar os nomes personalizados das coleções
    private val prefsNomes = application.getSharedPreferences("NomesColecoes", Context.MODE_PRIVATE)


    init {
        carregarDadosDoDisco()
        // --- OBSERVAÇÃO AUTOMÁTICA (NOVO) ---
        // Sempre que a lista de fotos mudar, este código corre automaticamente.
        _listaFotos.observeForever { fotos ->
            agruparFotosEmColecoes(fotos)
        }
    }

    fun adicionarFotos(novaFoto: FotoDados) {
        val listaAtual = _listaFotos.value ?: mutableListOf()
        listaAtual.add(novaFoto)
        _listaFotos.value = listaAtual // Isto vai disparar o observeForever acima
        guardarDadosNoDisco(listaAtual)
    }

    private fun guardarDadosNoDisco(lista: List<FotoDados>){
        val jsonString = gson.toJson(lista)
        prefsFotos.edit { putString("fotos_guardadas", jsonString) }
    }

    private fun carregarDadosDoDisco() {
        val jsonString = prefsFotos.getString("fotos_guardadas", null)
        if (jsonString != null) {
            val tipoDaLista = object : TypeToken<MutableList<FotoDados>>() {}.type
            val listaCarregada: MutableList<FotoDados> = gson.fromJson(jsonString, tipoDaLista)
            _listaFotos.value = listaCarregada
        } else {
            // Se não houver fotos, garante que o agrupamento é chamado com uma lista vazia
            agruparFotosEmColecoes(emptyList())
        }
    }

    // --- FUNÇÃO QUE FALTAVA ---
    /**
     * Agrupa a lista de fotos por data, criando uma lista de ColecaoDados.
     * Esta função é o coração da lógica de coleções.
     */
    private fun agruparFotosEmColecoes(fotos: List<FotoDados>?) {
        if (fotos.isNullOrEmpty()) {
            _listaColecoes.value = emptyList() // Define a lista de coleções como vazia
            return
        }

        val fotosAgrupadas = fotos.groupBy { it.data }

        val colecoesProcessadas = fotosAgrupadas.map { (data, fotosDoDia) ->
            ColecaoDados(
                titulo = data, // A data é o ID da coleção
                // Carrega o nome personalizado guardado, ou usa null se não existir
                nomePersonalizado = prefsNomes.getString(data, null),
                capaUri = fotosDoDia.first().uriString,
                listaFotos = fotosDoDia
            )
        }
        _listaColecoes.value = colecoesProcessadas
    }

    // --- FUNÇÃO QUE FALTAVA ---
    /**
     * Guarda o novo nome de uma coleção de forma persistente.
     */
    fun renomearColecao(dataDaColecao: String, novoNome: String) {
        // Guarda o novo nome no SharedPreferences, usando a data como chave
        prefsNomes.edit { putString(dataDaColecao, novoNome) }

        // Força a recriação da lista de coleções para que a UI seja atualizada imediatamente
        agruparFotosEmColecoes(_listaFotos.value)
    }
    fun apagarColecao(colecaoParaApagar: ColecaoDados) {
        val listaAtual = _listaFotos.value ?: return

        // Remove todas as fotos cuja data corresponde à data da coleção a ser apagada
        listaAtual.removeAll { it.data == colecaoParaApagar.titulo }

        // Remove também o nome personalizado associado, se existir
        prefsNomes.edit { remove(colecaoParaApagar.titulo) }

        // Atualiza o LiveData, o que fará com que o observeForever dispare o reagrupamento
        _listaFotos.value = listaAtual

        // Guarda a lista de fotos atualizada no disco
        guardarDadosNoDisco(listaAtual)
    }

    //FOTOS ------------------------------------------------------------------------------
    fun apagarFoto(fotoParaApagar: FotoDados) {val listaAtual = _listaFotos.value ?: return

        val fotoRemovida = listaAtual.remove(fotoParaApagar)

        // Se a foto foi efetivamente removida, atualizamos os dados.
        if (fotoRemovida) {
            // Atualiza o LiveData. Isto vai disparar o observeForever, que por sua vez
            // vai reagrupar as fotos em coleções e atualizar a UI automaticamente.
            _listaFotos.value = listaAtual

            // Guarda a nova lista (sem a foto apagada) no disco.
            guardarDadosNoDisco(listaAtual)
        }
    }

    fun renomearFoto(titulo: String, tituloPersonalizado: String) {
        // Guarda o novo nome no SharedPreferences, usando a data como chave
        prefsNomes.edit { putString(titulo, tituloPersonalizado) }

        // Força a recriação da lista de coleções para que a UI seja atualizada imediatamente
        agruparFotosEmColecoes(_listaFotos.value)
    }


}
