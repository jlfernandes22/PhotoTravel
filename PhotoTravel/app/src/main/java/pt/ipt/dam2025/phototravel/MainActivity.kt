package pt.ipt.dam2025.phototravel

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels // Necessário para o 'by viewModels()'
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import pt.ipt.dam2025.phototravel.adaptadores.ViewPagerAdapter
import pt.ipt.dam2025.phototravel.viewmodel.PartilhaDadosViewModel

class MainActivity : AppCompatActivity() {

    // ✅ Adicionado: Instância do ViewModel para gerir o ciclo de vida dos dados
    private val viewModel: PartilhaDadosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val viewPager = findViewById<ViewPager2>(R.id.view_pager2)
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        // Configurações do ViewPager2
        viewPager.isUserInputEnabled = false
        viewPager.offscreenPageLimit = 2

        // Sincronização entre TabLayout e ViewPager2
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(janela: TabLayout.Tab?) {
                viewPager.setCurrentItem(janela!!.position, false)
            }
            override fun onTabUnselected(p0: TabLayout.Tab?) {}
            override fun onTabReselected(p0: TabLayout.Tab?) {}
        })

        viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                tabLayout.getTabAt(position)?.select()
            }
        })

    }


    override fun onResume() {
        super.onResume()
        viewModel.recarregarDados()
    }
}