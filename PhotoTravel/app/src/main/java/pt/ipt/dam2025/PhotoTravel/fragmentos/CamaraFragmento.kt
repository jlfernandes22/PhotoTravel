package pt.ipt.dam2025.phototravel.fragmentos


import android.Manifest
import android.content.pm.PackageManager
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Locale
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import pt.ipt.dam2025.phototravel.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors



class CamaraFragmento : Fragment() {

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camara, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pede permissão e, se concedida, inicia a câmara
        verificarPermissaoEIniciarCamara()

        //  listener do botão de tirar foto
        view.findViewById<ImageButton>(R.id.image_capture_button).setOnClickListener { tirarFoto() }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun verificarPermissaoEIniciarCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            iniciarCamara()
        } else {
            Toast.makeText(requireContext(), "É necessário dar permissão à câmara para tirar fotos.", Toast.LENGTH_LONG).show()
        }
    }

    private fun iniciarCamara() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // pré-visualização
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(view?.findViewById<PreviewView>(R.id.viewFinder)?.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            // Seleciona a câmara traseira (default)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Desvincula tudo antes de vincular novamente
                cameraProvider.unbindAll()

                // Vincula os casos de uso (preview, captura) ao ciclo de vida do fragmento
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e("CamaraFragmento", "Falha ao vincular casos de uso", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun tirarFoto() {
        val imageCapture = imageCapture ?: return // Se a captura não estiver pronta, sai

        // Cria um nome para o ficheiro
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())

        // Define os metadados da imagem para a galeria
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoTravel")
            }
        }

        // Cria o objeto de opções de saída, especificando onde guardar o ficheiro
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                requireContext().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        // Tira a foto com as opções definidas
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CamaraFragmento", "Erro ao tirar a foto: ${exc.message}", exc)
                    Toast.makeText(requireContext(), "Erro ao guardar a foto.", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Foto guardada com sucesso: ${output.savedUri}"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    Log.d("CamaraFragmento", msg)
                }
            }
        )
    } // A função tirarFoto termina AQUI

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }
}