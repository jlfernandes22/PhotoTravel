package pt.ipt.dam2025.PhotoTravel.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pt.ipt.dam2025.phototravel.MainActivity // CORREÇÃO: Importar a MainActivity do pacote correto
import pt.ipt.dam2025.PhotoTravel.R
import pt.ipt.dam2025.PhotoTravel.data.model.LoginRequest
import pt.ipt.dam2025.PhotoTravel.data.remote.RetrofitInstance

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val email = findViewById<EditText>(R.id.emailEditText)
        val password = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registarButton = findViewById<Button>(R.id.registarButton)

        // Lógica para o botão de Login (já existente)
        loginButton.setOnClickListener {
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()

            lifecycleScope.launch {
                try {
                    val response = RetrofitInstance.api.login(
                        LoginRequest(emailText, passwordText)
                    )

                    if (response.isSuccessful) {
                        Toast.makeText(this@LoginActivity, "Login bem-sucedido!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Erro no login: Credenciais inválidas", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Erro de ligação: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // ✅ PASSO 2 e 3: Adicionar o OnClickListener para o botão de registo
        registarButton.setOnClickListener {
            // Cria uma Intent para navegar da LoginActivity para a RegistarActivity
            val intent = Intent(this, RegistarActivity::class.java)
            // Inicia a nova atividade
            startActivity(intent)
        }
    }
}
