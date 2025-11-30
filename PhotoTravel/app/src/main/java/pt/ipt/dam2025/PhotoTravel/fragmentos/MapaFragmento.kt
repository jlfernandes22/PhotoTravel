package pt.ipt.dam2025.phototravel.fragmentos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import pt.ipt.dam2025.phototravel.R
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions


/**
 * A simple [Fragment] subclass.
 * Use the [MapaFragmento.newInstance] factory method to
 * create an instance of this fragment.
 */
class MapaFragmento : Fragment() {

    // variável para os pins
    private lateinit var pinManager: SymbolManager
    private lateinit var vistaMapa: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        MapLibre.getInstance(requireContext())
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_mapa, container, false)
    }

    /**
     * Criar o mapa
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vistaMapa = view.findViewById<MapView>(R.id.mapa)

        vistaMapa.onCreate(savedInstanceState)

        vistaMapa.getMapAsync { map ->

            val estiloURL = "https://api.maptiler.com/maps/aquarelle/style.json?key=WFHHB4Zg3NcUnxvMy6uZ"

            map.setStyle(estiloURL) { estilo ->

                //ativar pins
                pinManager = SymbolManager(vistaMapa, map, estilo )

                // desativar sobreposição
                pinManager.iconAllowOverlap = false
                pinManager.textAllowOverlap = false

                //posição do pin
                val parisLocal = LatLng(48.8566, 2.3522) // Paris coordinates
                //criar pin
                val pin = pinManager.create(
                    SymbolOptions().
                    withLatLng(parisLocal).
                    //usar para colocar as imagens
                    withIconImage("marker").
                    withTextField("PARIS").
                    withIconSize(0.5f)
                )
                //posição inicial do mapa
                map.cameraPosition = CameraPosition.Builder().
                        target(LatLng(48.8566, 2.3522)).
                        zoom(10.0).
                        build()

                //  testar toque no pin e mostrar mensagem
                pinManager.addClickListener { pin ->
                    // Show a message when clicked
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Clicked: ${pin.textField}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    true // true para "consumir" o toque
                }
            }

        }
    }


    /**
     * Ciclo de vida do mapa - Início
     */
    override fun onStart() {
        super.onStart()
        vistaMapa.onStart()
    }
    /**
     * Ciclo de vida do mapa - Resumo
     */
    override fun onResume() {
        super.onResume()
        vistaMapa.onResume()
    }
    /**
     * Ciclo de vida do mapa - Pausa
     */
    override fun onPause() {
        super.onPause()
        vistaMapa.onPause()
    }
    /**
     * Ciclo de vida do mapa - Parar
     */
    override fun onStop() {
        super.onStop()
        vistaMapa.onStop()
    }
    /**
     * Ciclo de vida do mapa - Memória
     */
    override fun onLowMemory() {
        super.onLowMemory()
        vistaMapa.onLowMemory()
    }
    /**
     * Ciclo de vida do mapa - Destruir
     */
    override fun onDestroyView() {
        super.onDestroyView()
        vistaMapa.onDestroy()
    }
    /**
     * Ciclo de vida do mapa - Guardar estado
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        vistaMapa.onSaveInstanceState(outState)
    }




/**
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MapaFraguemento.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MapaFraguemento().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    **/
}