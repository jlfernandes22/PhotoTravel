package pt.ipt.dam2025.phototravel

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import pt.ipt.dam2025.PhotoTravel.R

class MainActivity : AppCompatActivity() {


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
        // Para desativar swipe do utilizador, é possível apenas para o mapa
        // mas achamos que não faz sentido para a aplicação
        viewPager.isUserInputEnabled = false

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(janela: TabLayout.Tab?) {
                viewPager.currentItem = janela!!.position

            }

            override fun onTabUnselected(p0: TabLayout.Tab?) {
                // Não é necessário implementar
            }

            override fun onTabReselected(p0: TabLayout.Tab?) {
                // Não é necessário implementar
            }

        })

        viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                tabLayout.getTabAt(position)?.select()
            }

        })

    }
}