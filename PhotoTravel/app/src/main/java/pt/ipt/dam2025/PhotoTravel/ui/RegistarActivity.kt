package pt.ipt.dam2025.PhotoTravel.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pt.ipt.dam2025.PhotoTravel.R
import pt.ipt.dam2025.PhotoTravel.data.model.LoginRequest
import pt.ipt.dam2025.PhotoTravel.data.remote.RetrofitInstance
import pt.ipt.dam2025.phototravel.MainActivity
import pt.ipt.dam2025.PhotoTravel.data.model.RegisterRequest

class RegistarActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registar)

        val email = findViewById<EditText>(R.id.emailRegistarEditText)
        val password = findViewById<EditText>(R.id.passwordRegistarEditText)
        val passwordConfirmada = findViewById<EditText>(R.id.passwordRegistar2EditText)
        val button = findViewById<Button>(R.id.registarButton)

        button.setOnClickListener {
            val emailText = email.text.toString().trim() // Use .trim() para remover espaços
            val passwordText = password.text.toString().trim()
            val passwordText2 = passwordConfirmada.text.toString().trim()

            if (passwordText != passwordText2) {
                Toast.makeText(this, "As passwords não coincidem!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Para a execução aqui
            }
            // Lançar a corrotina dentro do escopo do ciclo de vida da Activity
            lifecycleScope.launch {
                try {
                    val response = RetrofitInstance.api.register(
                        RegisterRequest(emailText, passwordText, passwordText2)
                    )

                    if (response.isSuccessful) {
                        // Sucesso no registo
                        Toast.makeText(this@RegistarActivity, "Registo bem-sucedido!", Toast.LENGTH_SHORT).show()

                        // NAVEGAR PARA O LOGIN
                        val intent = Intent(this@RegistarActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish() // Opcional: fecha a RegistarActivity

                    } else {
                        // Erro retornado pelo servidor
                        Toast.makeText(this@RegistarActivity, "Erro no registo", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    // Erro de rede ou outro problema (ex: sem internet, servidor offline)
                    Toast.makeText(this@RegistarActivity, "Erro de ligação: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}