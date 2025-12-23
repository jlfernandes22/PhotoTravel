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
import androidx.fragment.app.activityViewModels
import pt.ipt.dam2025.PhotoTravel.FotoDados
import pt.ipt.dam2025.PhotoTravel.PartilhaDadosViewModel
import pt.ipt.dam2025.phototravel.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors



class CamaraFragmento : Fragment() {

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private val viewModel: PartilhaDadosViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camara, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // listener do botão de tirar foto
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

    /**
     *Função para iniciar a câmara
     */
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

                // Vincula ao viewLifecycleOwner para garantir que segue o ciclo de vida da UI
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e("CamaraFragmento", "Falha ao vincular casos de uso", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /**
     *  Função para desativar a câmara,
     *  assim quando se muda de fragmento a camara é desativada
     *
     */
    private fun desativarCamara() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /**
     * Função para tirar uma foto
     */
    private fun tirarFoto() {
        val imageCapture = imageCapture ?: return

        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoTravel")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                requireContext().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CamaraFragmento", "Erro ao tirar a foto: ${exc.message}", exc)
                    Toast.makeText(requireContext(), "Erro ao guardar a foto.", Toast.LENGTH_SHORT).show()
                }

                /**
                 * Função para avisar que a foto foi guardada com sucesso
                 * E também para criar um objeto FotoDados para guardar os dados da foto
                 */
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Foto guardada com sucesso: ${output.savedUri}"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    Log.d("CamaraFragmento", msg)
                    val novaFoto = FotoDados(
                        uriString = output.savedUri.toString(),
                        titulo = name,
                        data = name,
                        latitude = 0.0, //TODO: adicionar a informação da localização
                        longitude = 0.0 //TODO: adicionar a informação da localização
                    )
                    viewModel.adicionarFotos(novaFoto)

                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        // Inicia a câmara apenas quando o fragmento fica visível (Resumed)
        verificarPermissaoEIniciarCamara()
    }

    override fun onPause() {
        super.onPause()
        // Desativa a câmara explicitamente ao sair do fragmento
        desativarCamara()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }
}
