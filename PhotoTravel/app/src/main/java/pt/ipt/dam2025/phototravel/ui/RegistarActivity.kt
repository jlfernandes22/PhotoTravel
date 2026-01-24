package pt.ipt.dam2025.phototravel.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject // ✅ IMPORTANTE: Adiciona este import
import pt.ipt.dam2025.phototravel.data.remote.RetrofitInstance
import pt.ipt.dam2025.phototravel.R
import pt.ipt.dam2025.phototravel.ui.LoginActivity // Ajusta o package se necessário
import pt.ipt.dam2025.phototravel.data.model.RegisterRequest

/**
 * <summary>
 * criação de novas contas de utilizador.
 * </summary>
 */
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

            // Validação local básica (opcional, já que o servidor também valida)
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
                        // ✅ CORREÇÃO AQUI: Ler a mensagem de erro do servidor
                        val erroBody = response.errorBody()?.string()

                        if (erroBody != null) {
                            try {
                                // O servidor devolve algo como: {"error": "Email já registado"}
                                val jsonObject = JSONObject(erroBody)
                                val mensagemErro = jsonObject.getString("error")
                                Toast.makeText(this@RegistarActivity, mensagemErro, Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                // Se não conseguir ler o JSON, mostra erro genérico
                                Toast.makeText(this@RegistarActivity, "Erro no registo: ${response.code()}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@RegistarActivity, "Erro desconhecido.", Toast.LENGTH_SHORT).show()
                        }
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