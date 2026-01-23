package pt.ipt.dam2025.phototravel.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pt.ipt.dam2025.phototravel.data.model.LoginRequest
import pt.ipt.dam2025.phototravel.data.remote.RetrofitInstance
import pt.ipt.dam2025.phototravel.MainActivity
import pt.ipt.dam2025.phototravel.R
import androidx.core.content.edit


class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val email = findViewById<EditText>(R.id.emailEditText)
        val password = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registarButton = findViewById<Button>(R.id.registarButton)

        loginButton.setOnClickListener {
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()

            lifecycleScope.launch {
                try {
                    val response = RetrofitInstance.api.login(
                        LoginRequest(emailText, passwordText)
                    )

                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        if (loginResponse != null) {
                            // 1. Guardar o token
                            val sharedPrefs = getSharedPreferences("PhotoTravelPrefs", MODE_PRIVATE)
                            sharedPrefs.edit().putString("USER_TOKEN", loginResponse.token).apply()

                            Toast.makeText(this@LoginActivity, "Login bem-sucedido!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "Erro no login: Credenciais inválidas", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Erro de ligação: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        registarButton.setOnClickListener {
            val intent = Intent(this, RegistarActivity::class.java)
            startActivity(intent)
        }
    }
}


