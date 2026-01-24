package pt.ipt.dam2025.phototravel.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import pt.ipt.dam2025.phototravel.data.model.LoginRequest
import pt.ipt.dam2025.phototravel.data.model.LoginResponse
import pt.ipt.dam2025.phototravel.data.model.RegisterRequest
import pt.ipt.dam2025.phototravel.modelos.ColecaoDados
import pt.ipt.dam2025.phototravel.modelos.FotoDados
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ---------- AUTH ----------
    @POST("login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<Unit>

    // ---------- COLLECTIONS ----------
    @POST("collections")
    suspend fun createCollection(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Response<ColecaoDados>

    @GET("collections")
    suspend fun getCollections(
        @Header("Authorization") token: String
    ): Response<List<ColecaoDados>>

    // ---------- PHOTOS ----------
    @Multipart
    @POST("photos")
    suspend fun uploadPhoto(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("collectionId") collectionId: RequestBody
    ): Response<Unit>

    @GET("photos")
    suspend fun getPhotos(
        @Header("Authorization") token: String,
        @Query("collectionId") collectionId: Int?
    ): Response<List<FotoDados>>


    @DELETE("photos/{id}")
    suspend fun deletePhoto(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<Unit> // Unit porque o servidor devolve apenas uma mensagem JSON, não dados
}
