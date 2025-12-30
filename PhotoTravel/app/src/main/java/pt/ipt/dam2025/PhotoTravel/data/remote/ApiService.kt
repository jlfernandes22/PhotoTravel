package pt.ipt.dam2025.PhotoTravel.data.remote

import pt.ipt.dam2025.PhotoTravel.data.model.LoginRequest // Verifique o caminho correto para LoginRequest
import pt.ipt.dam2025.PhotoTravel.data.model.LoginResponse // Verifique o caminho correto para LoginResponse
import pt.ipt.dam2025.PhotoTravel.data.model.RegisterRequest
import pt.ipt.dam2025.PhotoTravel.data.model.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>


    @POST("register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>
}
