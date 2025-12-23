package pt.ipt.dam2025.PhotoTravel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit

class PartilhaDadosViewModel(application: Application) : AndroidViewModel(application) {

    private val _listaFotos = MutableLiveData<MutableList<FotoDados>>(mutableListOf())
    val listaFotos: LiveData<MutableList<FotoDados>> get() = _listaFotos

    private val gson = Gson()
    private val sharedPrefs = application.getSharedPreferences("TrabelPhoto", Context.MODE_PRIVATE)

    init {
        carregarDadosDoDisco()
    }

    fun adicionarFotos(novaFoto: FotoDados) {

        val listaAtual = _listaFotos.value ?: mutableListOf()
        listaAtual.add(novaFoto)
        _listaFotos.value = listaAtual

        guardarDadosNoDisco(listaAtual)


    }

    private fun guardarDadosNoDisco(lista: List<FotoDados>){

        val jsonString = gson.toJson(lista)
        sharedPrefs.edit { putString("fotos_guardadas", jsonString) }


    }

    private fun carregarDadosDoDisco() {

        val jsonString = sharedPrefs.getString("fotos_guardadas", null)
        if (jsonString != null) {
            // Se encontrou dados, transforma Texto -> Lista
            val tipoDaLista = object : TypeToken<MutableList<FotoDados>>() {}.type
            val listaCarregada: MutableList<FotoDados> = gson.fromJson(jsonString, tipoDaLista)

            _listaFotos.value = listaCarregada

        }

    }


}
