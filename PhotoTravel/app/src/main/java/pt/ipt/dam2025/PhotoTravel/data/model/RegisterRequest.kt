package pt.ipt.dam2025.PhotoTravel.data.model

data class RegisterRequest(
    val email: String,
    val password: String,
    val passwordConfirmacao: String
)