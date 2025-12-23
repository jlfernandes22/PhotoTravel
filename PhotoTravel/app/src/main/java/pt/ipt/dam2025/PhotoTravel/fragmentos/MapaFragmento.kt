package pt.ipt.dam2025.phototravel.fragmentos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import pt.ipt.dam2025.phototravel.R
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import androidx.core.graphics.scale
import com.google.gson.internal.bind.TypeAdapters.URI
import pt.ipt.dam2025.phototravel.BuildConfig.API_KEY
import androidx.core.net.toUri
import pt.ipt.dam2025.PhotoTravel.PartilhaDadosViewModel
import kotlin.properties.ReadOnlyProperty
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.snapshotter.MapSnapshotter
import androidx.core.graphics.createBitmap


/**
 * A simple [Fragment] subclass.
 * Use the [MapaFragmento.newInstance] factory method to
 * create an instance of this fragment.
 */
class MapaFragmento : Fragment() {


    private val viewModel: PartilhaDadosViewModel by activityViewModels()


    // variável para os pins
    private lateinit var pinManager: SymbolManager
    private lateinit var vistaMapa: MapView
    private lateinit var mapLibreMap: MapLibreMap

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
            this.mapLibreMap = map
            val estiloURL = "https://api.maptiler.com/maps/streets/style.json?key=$API_KEY"

            map.setStyle(estiloURL) { estilo ->

                //ativar pins
                pinManager = SymbolManager(vistaMapa, map, estilo )

                pinManager.iconAllowOverlap = true
                pinManager.textAllowOverlap = true


                viewModel.listaFotos.observe(viewLifecycleOwner, Observer { listaDeFotos ->

                    pinManager.deleteAll()

                    for (foto in listaDeFotos) {

                        val bitmapIcone: Bitmap? = carregarFotos(requireContext(), foto.uriString)

                        if(bitmapIcone != null) {
                            val idImagem: String = "img_${foto.titulo}"
                            estilo.addImage(idImagem, bitmapIcone)

                            pinManager.create(
                                SymbolOptions()
                                    .withLatLng(LatLng(foto.longitude,foto.latitude))
                                    .withIconImage(idImagem)
                                    .withIconSize(0.5f)
                            )

                        }
                    }
                })

            }

        }
    }

    /**
     * Função para carregar foto do dispositivo
     */
    fun carregarFotos(context: android.content.Context, uriString: String): Bitmap? {

        try{
            val uri = uriString.toUri()
            val contentResolver = context.contentResolver


            //carregar a imagem original
            val localizacao = ImageDecoder.createSource(contentResolver, uri)
            val bitmapOriginal = ImageDecoder.decodeBitmap(localizacao)

            return bitmapOriginal.scale(150, 150, false)

        }catch (e: Exception){
            val msg = "Erro ao carregar imagem do dispositivo "
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

        }
        return null
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

}
