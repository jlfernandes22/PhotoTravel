package pt.ipt.dam2025.phototravel.data.model

data class LoginResponse(
    val token: String,
    val user: UserDto
)

data class UserDto(
    val id: Int,
    val email: String
)
