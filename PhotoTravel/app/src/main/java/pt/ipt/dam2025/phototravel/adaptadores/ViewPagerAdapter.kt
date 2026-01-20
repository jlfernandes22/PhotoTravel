package pt.ipt.dam2025.phototravel.adaptadores

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import pt.ipt.dam2025.phototravel.MainActivity
import pt.ipt.dam2025.phototravel.fragmentos.CamaraFragmento
import pt.ipt.dam2025.phototravel.fragmentos.ColecoesFragmento
import pt.ipt.dam2025.phototravel.fragmentos.MapaFragmento

class ViewPagerAdapter(activity: MainActivity) : FragmentStateAdapter(activity) {

    /**
     * Definir quantidade de fragmentos
     */
    override fun getItemCount(): Int {
        return 3
    }

    /**
     * Criar fragmento conforme a posição selecionada
     */
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CamaraFragmento()
            1 -> MapaFragmento()
            2 -> ColecoesFragmento()
            else -> CamaraFragmento()
        }
    }




}