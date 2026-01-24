// pt/ipt/dam2025/phototravel/data/remote/RetrofitInstance.kt

package pt.ipt.dam2025.phototravel.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    // 1. Criar o Logger
    private val logging = HttpLoggingInterceptor().apply {
        // "BODY" mostra tudo: cabeçalhos e o JSON de resposta
        level = HttpLoggingInterceptor.Level.BODY
    }

    // 2. Criar o Cliente HTTP com o Logger
    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS) // Aumentar timeout por segurança
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // 3. Criar o Retrofit
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("http://85.139.75.43:25979/") //servidor
            //.baseUrl("http://10.81.18.124:25979/") //local
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}