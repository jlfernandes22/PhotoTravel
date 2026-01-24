package pt.ipt.dam2025.phototravel

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import pt.ipt.dam2025.phototravel.adaptadores.ViewPagerAdapter
import pt.ipt.dam2025.phototravel.viewmodel.PartilhaDadosViewModel

/**
 * <summary>
 * Atividade principal da aplicação.
 * Atua como o hospedeiro (host) para os fragmentos de Coleções, Câmara e Mapa,
 * utilizando um ViewPager2 com TabLayout para navegação.
 * </summary>
 */
class MainActivity : AppCompatActivity() {

    // Instância do ViewModel partilhado que gere o estado dos dados em toda a App
    private val viewModel: PartilhaDadosViewModel by viewModels()

    /**
     * <summary>
     * Inicializa a interface, configura o suporte Edge-to-Edge e estabelece a
     * ligação entre o TabLayout e o ViewPager2.
     * </summary>
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        //  Ajusta o padding da vista para respeitar as barras de sistema (status/navigation)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val viewPager = findViewById<ViewPager2>(R.id.view_pager2)
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        // <summary>
        // Configurações do ViewPager2:
        // Desativa o deslize manual (swipe) para evitar conflitos com o Mapa e mantém as páginas em cache.
        // </summary>
        viewPager.isUserInputEnabled = false
        viewPager.offscreenPageLimit = 2

        /**
         * <summary>
         * Sincroniza o ViewPager2 com a aba selecionada no TabLayout.
         * </summary>
         */
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(janela: TabLayout.Tab?) {
                viewPager.setCurrentItem(janela!!.position, false)
            }
            override fun onTabUnselected(p0: TabLayout.Tab?) {}
            override fun onTabReselected(p0: TabLayout.Tab?) {}
        })

        /**
         * <summary>
         * Sincroniza o TabLayout quando a página do ViewPager2 é alterada .
         * </summary>
         */
        viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                tabLayout.getTabAt(position)?.select()
            }
        })

    }

    /**
     * <summary>
     * Ponto crucial de sincronização: Força o ViewModel a reler os dados do armazenamento
     * sempre que a atividade principal volta ao primeiro plano (ex: após fechar o Detalhe ou Login).
     * </summary>
     */
    override fun onResume() {
        super.onResume()
        viewModel.recarregarDados()
    }
}