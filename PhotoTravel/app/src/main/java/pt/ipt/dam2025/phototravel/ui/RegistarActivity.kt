package pt.ipt.dam2025.phototravel.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pt.ipt.dam2025.phototravel.data.remote.RetrofitInstance
import pt.ipt.dam2025.phototravel.R
import pt.ipt.dam2025.phototravel.MainActivity
import pt.ipt.dam2025.phototravel.data.model.RegisterRequest

class RegistarActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registar)

        val email = findViewById<EditText>(R.id.emailRegistarEditText)
        val password = findViewById<EditText>(R.id.passwordRegistarEditText)
        val passwordConfirmada = findViewById<EditText>(R.id.passwordRegistar2EditText)
        val button = findViewById<Button>(R.id.registarButton)
        val retornarLoginButton = findViewById<Button>(R.id.retornarLoginButton)

        button.setOnClickListener {
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()
            val passwordText2 = passwordConfirmada.text.toString().trim()

            if (passwordText != passwordText2) {
                Toast.makeText(this, "As passwords não coincidem!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                try {
                    val response = RetrofitInstance.api.register(
                        RegisterRequest(emailText, passwordText, passwordText2)
                    )

                    if (response.isSuccessful) {
                        Toast.makeText(this@RegistarActivity, "Registo bem-sucedido!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@RegistarActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@RegistarActivity, "Erro no registo", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@RegistarActivity, "Erro de ligação: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        retornarLoginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
