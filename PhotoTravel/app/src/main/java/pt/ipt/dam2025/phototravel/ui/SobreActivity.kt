package pt.ipt.dam2025.phototravel.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import pt.ipt.dam2025.phototravel.R

class SobreActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Reutilizamos o layout que já tens (fragmento_sobre)
        setContentView(R.layout.fragmento_sobre)

        // Adiciona botão de voltar na barra de topo (se existir Action Bar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Sobre o Projeto"
    }

    // Faz o botão de voltar funcionar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}