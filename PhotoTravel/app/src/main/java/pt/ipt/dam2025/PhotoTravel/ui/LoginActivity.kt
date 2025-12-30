package pt.ipt.dam2025.PhotoTravel.ui

import pt.ipt.dam2025.phototravel.MainActivity
import android.content.Intent // ADICIONAR ESTA IMPORTAÇÃO
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pt.ipt.dam2025.PhotoTravel.R
import pt.ipt.dam2025.PhotoTravel.data.model.LoginRequest
import pt.ipt.dam2025.PhotoTravel.data.remote.RetrofitInstance

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val email = findViewById<EditText>(R.id.emailEditText)
        val password = findViewById<EditText>(R.id.passwordEditText)
        val button = findViewById<Button>(R.id.loginButton)

        button.setOnClickListener {
            val emailText = email.text.toString().trim() // Use .trim() para remover espaços
            val passwordText = password.text.toString().trim()

            // Lançar a corrotina dentro do escopo do ciclo de vida da Activity
            lifecycleScope.launch {
                try {
                    val response = RetrofitInstance.api.login(
                        LoginRequest(emailText, passwordText)
                    )

                    if (response.isSuccessful) {
                        // Sucesso no login
                        Toast.makeText(this@LoginActivity, "Login bem-sucedido!", Toast.LENGTH_SHORT).show()

                        // NAVEGAR PARA A MAINACTIVITY
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish() // Opcional: fecha a LoginActivity para que o utilizador não possa voltar a ela com o botão "Back"

                    } else {
                        // Erro retornado pelo servidor (ex: credenciais erradas)
                        Toast.makeText(this@LoginActivity, "Erro no login: Credenciais inválidas", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    // Erro de rede ou outro problema (ex: sem internet, servidor offline)
                    Toast.makeText(this@LoginActivity, "Erro de ligação: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
