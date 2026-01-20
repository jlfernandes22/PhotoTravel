package pt.ipt.dam2025.phototravel.fragmentos

import android.Manifest
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

class CamaraFragmento : Fragment() {

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private val viewModel: PartilhaDadosViewModel by activityViewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var ultimaLocal: Location? = null
    private var gpsDialogJaSolicitado = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            ultimaLocal = result.lastLocation
            Log.d("GPS", "Localização atualizada: ${ultimaLocal?.latitude}, ${ultimaLocal?.longitude}")
        }
    }

    private val gpsAtivo = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        if (it.resultCode == android.app.Activity.RESULT_OK) {
            rastrearGPS()
        } else {
            Toast.makeText(requireContext(), "GPS é recomendado para georreferenciar fotos.", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            iniciarCamara()
        } else {
            Toast.makeText(requireContext(), "Permissão da câmara é necessária para tirar fotos.", Toast.LENGTH_LONG).show()
        }
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            verificarGpsRastrear()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camara, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.image_capture_button).setOnClickListener { tirarFoto() }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        cameraExecutor = Executors.newSingleThreadExecutor()

        // --- CORREÇÃO PRINCIPAL: PEDIR PERMISSÕES AQUI, UMA SÓ VEZ ---
        verificarPermissoes()
    }

    // --- FUNÇÃO RENOMEADA E CORRIGIDA ---
    private fun verificarPermissoes() {
        val cameraGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val locationGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val permissionsToRequest = mutableListOf<String>()
        if (!cameraGranted) permissionsToRequest.add(Manifest.permission.CAMERA)
        if (!locationGranted) permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // Se já temos as permissões, podemos iniciar tudo
            iniciarCamara()
            verificarGpsRastrear()
        }
    }

    // A função onResume agora é muito mais simples
    override fun onResume() {
        super.onResume()
        // Se já temos permissão da câmara, reinicia a pré-visualização.
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara()
        }
    }

    // O resto das suas funções (tirarFoto, iniciarCamara, etc.) estão corretas e podem ser mantidas como estão.
    // ... (mantenha o resto do seu código a partir daqui)

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
                Log.e("CamaraFragmento", "Falha ao vincular casos de uso", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun desativarCamara() {
        if(::cameraExecutor.isInitialized && !cameraExecutor.isShutdown){
            val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
            cameraProviderFuture.addListener({
                cameraProviderFuture.get().unbindAll()
            }, ContextCompat.getMainExecutor(requireContext()))
        }
    }

    override fun onPause() {
        super.onPause()
        desativarCamara()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }

    private fun tirarFoto() {
        val imageCapture = imageCapture ?: return
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
        val dataDia = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(System.currentTimeMillis())
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
                    Toast.makeText(requireContext(), "Erro ao guardar a foto.", Toast.LENGTH_SHORT).show()
                }

                // Dentro de onImageSaved

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = output.savedUri ?: return

                    val novaFoto = FotoDados(
                        uriString = uri.toString(),
                        titulo = name,
                        data = dataDia,
                        latitude = ultimaLocal?.latitude,
                        longitude = ultimaLocal?.longitude,
                        tituloPersonalizado = null
                    )


                    try {
                        viewModel.adicionarFoto(novaFoto)
                    } catch (e: Exception) {
                    }

                    val msg = if (ultimaLocal != null) "Foto guardada com localização!" else "Foto guardada (sem GPS)."
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }

            }
        )
    }

    private fun rastrearGPS() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) ultimaLocal = location
            }
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).apply {
                setMinUpdateDistanceMeters(5.0f)
            }.build()
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e("GPS", "Erro de segurança ao iniciar rastreio: ${e.message}")
        }
    }

    private fun verificarGpsRastrear() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val task = LocationServices.getSettingsClient(requireContext()).checkLocationSettings(builder.build())

        task.addOnSuccessListener { rastrearGPS() }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && !gpsDialogJaSolicitado) {
                try {
                    gpsDialogJaSolicitado = true
                    gpsAtivo.launch(IntentSenderRequest.Builder(exception.resolution).build())
                } catch (sendEx: Exception) {
                    Log.e("GPS", "Erro ao mostrar diálogo de GPS", sendEx)
                }
            }
        }
    }
}
