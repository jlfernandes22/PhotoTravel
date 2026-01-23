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

class CamaraFragmento : Fragment() {
    // ... (Variáveis de classe mantêm-se iguais)
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private val viewModel: PartilhaDadosViewModel by activityViewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var ultimaLocal: Location? = null
    private var gpsDialogJaSolicitado = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) { ultimaLocal = result.lastLocation }
    }
    private val gpsAtivo = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        if (it.resultCode == Activity.RESULT_OK) rastrearGPS()
    }
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) iniciarCamara()
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) verificarGpsRastrear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camara, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<ImageButton>(R.id.image_capture_button).setOnClickListener { tirarFoto() }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        cameraExecutor = Executors.newSingleThreadExecutor()
        verificarPermissoes()
    }

    // (Funções de permissão, ciclo de vida e iniciar câmara mantêm-se iguais - copia do teu original ou pede se precisares)
    // ...
    // A função IMPORTANTE é a tirarFoto:

    private fun tirarFoto() {
        val imageCapture = imageCapture ?: return
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
        val dataDia = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoTravel")
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

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = output.savedUri ?: return

                    val novaFoto = FotoDados(
                        id = 0, // ✅ OBRIGATÓRIO: 0 indica nova foto local
                        uriString = uri.toString(),
                        titulo = name,
                        data = dataDia,
                        latitude = ultimaLocal?.latitude,
                        longitude = ultimaLocal?.longitude,
                        tituloPersonalizado = null
                    )

                    viewModel.adicionarFoto(novaFoto)
                    viewModel.enviarFotoComGestaoDeColecao(novaFoto)

                    val msg = if (ultimaLocal != null) "Foto guardada com GPS!" else "Foto guardada (sem GPS)."
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Funções auxiliares (verificarPermissoes, iniciarCamara, rastrearGPS) - Copia do teu ficheiro original
    private fun verificarPermissoes() {
        val cameraGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val locationGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val permissionsToRequest = mutableListOf<String>()
        if (!cameraGranted) permissionsToRequest.add(Manifest.permission.CAMERA)
        if (!locationGranted) permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissionsToRequest.isNotEmpty()) requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        else { iniciarCamara(); verificarGpsRastrear() }
    }
    private fun iniciarCamara() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(view?.findViewById<PreviewView>(R.id.viewFinder)?.surfaceProvider) }
            imageCapture = ImageCapture.Builder().build()
            try { cameraProvider.unbindAll(); cameraProvider.bindToLifecycle(viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture) }
            catch (exc: Exception) {}
        }, ContextCompat.getMainExecutor(requireContext()))
    }
    private fun verificarGpsRastrear() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val task = LocationServices.getSettingsClient(requireContext()).checkLocationSettings(builder.build())
        task.addOnSuccessListener { rastrearGPS() }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && !gpsDialogJaSolicitado) {
                try { gpsDialogJaSolicitado = true; gpsAtivo.launch(IntentSenderRequest.Builder(exception.resolution).build()) } catch (e: Exception) {}
            }
        }
    }
    private fun rastrearGPS() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { if (it != null) ultimaLocal = it }
            val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).apply { setMinUpdateDistanceMeters(5.0f) }.build()
            fusedLocationClient.requestLocationUpdates(req, locationCallback, Looper.getMainLooper())
        } catch (e: Exception) {}
    }
    override fun onResume() { super.onResume(); if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) iniciarCamara() }
    override fun onPause() { super.onPause(); if(::cameraExecutor.isInitialized && !cameraExecutor.isShutdown) ProcessCameraProvider.getInstance(requireContext()).addListener({ProcessCameraProvider.getInstance(requireContext()).get().unbindAll()}, ContextCompat.getMainExecutor(requireContext())); fusedLocationClient.removeLocationUpdates(locationCallback) }
    override fun onDestroyView() { super.onDestroyView(); if (::cameraExecutor.isInitialized) cameraExecutor.shutdown() }
}