package pt.ipt.dam2025.PhotoTravel.data.remote

import retrofit2.Retrofit // Importar a classe Retrofit
import retrofit2.converter.gson.GsonConverterFactory // Importar o conversor Gson


object RetrofitInstance {

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
