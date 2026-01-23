package pt.ipt.dam2025.phototravel.fragmentos

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Bundle
import android.util.Base64
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.scale
import androidx.core.net.toUri
import pt.ipt.dam2025.phototravel.viewmodel.PartilhaDadosViewModel
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import pt.ipt.dam2025.phototravel.R
import pt.ipt.dam2025.phototravel.BuildConfig

class MapaFragmento : Fragment() {

    private val viewModel: PartilhaDadosViewModel by activityViewModels()
    private lateinit var pinManager: SymbolManager
    private lateinit var vistaMapa: MapView
    private lateinit var mapLibreMap: MapLibreMap
    val apikey = BuildConfig.API_KEY

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        MapLibre.getInstance(requireContext())
        return inflater.inflate(R.layout.fragment_mapa, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vistaMapa = view.findViewById(R.id.mapa)
        vistaMapa.onCreate(savedInstanceState)

        vistaMapa.getMapAsync { map ->
            this.mapLibreMap = map
            val estiloURL = "https://api.maptiler.com/maps/streets/style.json?key=$apikey"

            map.setStyle(estiloURL) { estilo ->
                pinManager = SymbolManager(vistaMapa, map, estilo)
                pinManager.iconAllowOverlap = true
                pinManager.textAllowOverlap = true

                viewModel.listaFotos.observe(viewLifecycleOwner, Observer { listaDeFotos ->
                    pinManager.deleteAll()
                    for (foto in listaDeFotos) {
                        // Agora carrega ficheiros locais (file://) que são rápidos
                        val bitmapIcone: Bitmap? = carregarFotos(requireContext(), foto.uriString)

                        if(bitmapIcone != null) {
                            val idImagem: String = "img_${foto.id}_${foto.titulo}"
                            estilo.addImage(idImagem, bitmapIcone)

                            if((foto.latitude != null) && (foto.longitude != null)){
                                pinManager.create(
                                    SymbolOptions()
                                        .withLatLng(LatLng(foto.latitude, foto.longitude))
                                        .withIconImage(idImagem)
                                        .withIconSize(0.5f)
                                )
                            }
                        }
                    }
                })
            }
        }
    }

    private fun carregarFotos(context: android.content.Context, uriString: String): Bitmap? {
        try {
            // Se ainda vier Base64 (fallback), converte aqui
            if (uriString.startsWith("data:image")) {
                val base64Data = uriString.substringAfter("base64,")
                val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                return bitmap?.scale(150, 150, false)
            }

            // O PADRÃO AGORA: Carrega ficheiro local (file:// ou content://)
            val uri = uriString.toUri()
            val contentResolver = context.contentResolver
            val localizacao = ImageDecoder.createSource(contentResolver, uri)
            val bitmapOriginal = ImageDecoder.decodeBitmap(localizacao)
            return bitmapOriginal.scale(150, 150, false)

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // Ciclo de Vida do Mapa
    override fun onStart() { super.onStart(); vistaMapa.onStart() }
    override fun onResume() { super.onResume(); vistaMapa.onResume() }
    override fun onPause() { super.onPause(); vistaMapa.onPause() }
    override fun onStop() { super.onStop(); vistaMapa.onStop() }
    override fun onLowMemory() { super.onLowMemory(); vistaMapa.onLowMemory() }
    override fun onDestroyView() { super.onDestroyView(); vistaMapa.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); vistaMapa.onSaveInstanceState(outState) }
}