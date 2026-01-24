package pt.ipt.dam2025.phototravel.fragmentos

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import pt.ipt.dam2025.phototravel.R
import pt.ipt.dam2025.phototravel.modelos.FotoDados
import pt.ipt.dam2025.phototravel.viewmodel.PartilhaDadosViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * <summary>
 * Fragmento responsável pela funcionalidade de captura de fotografias.
 * Gere o ciclo de vida da câmara (CameraX), a obtenção de coordenadas GPS
 * e o envio inicial da foto para o ViewModel.
 * </summary>
 */
class CamaraFragmento : Fragment() {

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private val viewModel: PartilhaDadosViewModel by activityViewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var ultimaLocal: Location? = null
    private var gpsDialogJaSolicitado = false

    /**
     * <summary>
     * Callback para receber atualizações de localização em tempo real.
     * Guarda a localização mais recente na variável 'ultimaLocal'.
     * </summary>
     */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            ultimaLocal = result.lastLocation
        }
    }

    /**
     * <summary>
     * Trata o resultado do pedido (Intent) para ativar o GPS nas definições.
     * Se o utilizador ativar, inicia o rastreio.
     * </summary>
     */
    private val gpsAtivo = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            rastrearGPS()
        }
    }

    /**
     * <summary>
     * Gere as respostas aos pedidos de permissão de Câmara e Localização.
     * Se concedidas, inicia os respetivos serviços.
     * </summary>
     */
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

        if (cameraGranted) {
            iniciarCamara()
        } else {
            Toast.makeText(requireContext(), "Permissão de câmara necessária.", Toast.LENGTH_SHORT).show()
        }

        if (locationGranted) {
            verificarGpsRastrear()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camara, container, false)
    }

    /**
     * <summary>
     * Inicializa os componentes principais (botões, localização, executor)
     * e dispara a verificação inicial de permissões.
     * </summary>
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar botão de disparo
        view.findViewById<ImageButton>(R.id.image_capture_button).setOnClickListener {
            tirarFoto()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        cameraExecutor = Executors.newSingleThreadExecutor()

        verificarPermissoes()
    }

    /**
     * <summary>
     * Garante que a câmara é reiniciada quando o fragmento volta a estar visível (Resume).
     * </summary>
     */
    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara()
        }
    }

    /**
     * <summary>
     * Ao pausar o fragmento, liberta a câmara e para as atualizações de GPS para poupar bateria.
     * </summary>
     */
    override fun onPause() {
        super.onPause()
        desativarCamara()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    /**
     * <summary>
     * Ao destruir a vista, encerra o executor da câmara para evitar fugas de memória.
     * </summary>
     */
    override fun onDestroyView() {
        super.onDestroyView()
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }



    /**
     * <summary>
     * Captura a fotografia, guarda-a no armazenamento público (Galeria) e notifica o ViewModel.
     * É aqui que é desencadeado o processo de upload (ApiFix).
     * </summary>
     */
    private fun tirarFoto() {
        val imageCapture = imageCapture ?: return

        // Formatos de nome e data
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
        val dataDia = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(System.currentTimeMillis())

        // Metadados do ficheiro
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoTravel")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            requireContext().contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CamaraFragmento", "Erro ao capturar foto: ${exc.message}", exc)
                    Toast.makeText(requireContext(), "Erro ao guardar a foto.", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = output.savedUri ?: return

                    // Cria o objeto de dados da foto
                    val novaFoto = FotoDados(
                        id = 0, // 0 indica que é uma nova foto local (ainda sem ID do servidor)
                        uriString = uri.toString(),
                        titulo = name,
                        data = dataDia,
                        latitude = ultimaLocal?.latitude,
                        longitude = ultimaLocal?.longitude,
                        tituloPersonalizado = null
                    )


                    // 1. Adiciona à lista local
                    viewModel.adicionarFoto(novaFoto)
                    // 2. Inicia o processo de gestão de coleção e upload
                    viewModel.enviarFotoComGestaoDeColecao(novaFoto)

                    val msg = if (ultimaLocal != null) "Foto guardada com GPS!" else "Foto guardada (sem GPS)."
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    /**
     * <summary>
     * Configura o CameraX, vinculando a pré-visualização (Preview) e a captura (ImageCapture)
     * ao ciclo de vida deste fragmento.
     * </summary>
     */
    private fun iniciarCamara() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(view?.findViewById<PreviewView>(R.id.viewFinder)?.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("CamaraFragmento", "Falha ao vincular casos de uso da câmara", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /**
     * <summary>
     * Desvincula os casos de uso da câmara. Útil para libertar recursos ao sair do ecrã.
     * </summary>
     */
    private fun desativarCamara() {
        if(::cameraExecutor.isInitialized && !cameraExecutor.isShutdown){
            val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
            cameraProviderFuture.addListener({
                cameraProviderFuture.get().unbindAll()
            }, ContextCompat.getMainExecutor(requireContext()))
        }
    }


    /**
     * <summary>
     * Verifica se as permissões de Câmara e Localização já foram concedidas.
     * Se não, solicita-as. Se sim, inicia as funcionalidades.
     * </summary>
     */
    private fun verificarPermissoes() {
        val cameraGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val locationGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val permissionsToRequest = mutableListOf<String>()
        if (!cameraGranted) permissionsToRequest.add(Manifest.permission.CAMERA)
        if (!locationGranted) permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            iniciarCamara()
            verificarGpsRastrear()
        }
    }

    /**
     * <summary>
     * Verifica as definições do sistema para garantir que o GPS está ligado e configurado
     * para alta precisão. Se não estiver, pede ao utilizador para ativar.
     * </summary>
     */
    private fun verificarGpsRastrear() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val task = LocationServices.getSettingsClient(requireContext()).checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            rastrearGPS()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && !gpsDialogJaSolicitado) {
                try {
                    gpsDialogJaSolicitado = true
                    gpsAtivo.launch(IntentSenderRequest.Builder(exception.resolution).build())
                } catch (e: Exception) {
                    Log.e("GPS", "Erro ao tentar resolver definições de GPS", e)
                }
            }
        }
    }

    /**
     * <summary>
     * Inicia o pedido de atualizações de localização (GPS) ao FusedLocationProvider.
     * </summary>
     */
    private fun rastrearGPS() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        try {
            // Tenta obter a última localização conhecida imediatamente
            fusedLocationClient.lastLocation.addOnSuccessListener {
                if (it != null) ultimaLocal = it
            }

            // Inicia atualizações regulares (a cada 5 segundos ou 5 metros)
            val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).apply {
                setMinUpdateDistanceMeters(5.0f)
            }.build()

            fusedLocationClient.requestLocationUpdates(req, locationCallback, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e("GPS", "Erro ao iniciar rastreio GPS", e)
        }
    }
}