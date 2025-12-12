package pt.ipt.dam2025.phototravel.fragmentos

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import pt.ipt.dam2025.phototravel.R

class CamaraFragmento : Fragment() {

    private var button: Button? = null
    private var imageView: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camara, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button = view.findViewById(R.id.button)
        imageView = view.findViewById(R.id.imageView)

        button?.setOnClickListener {
            verificarPermissaoEAbrirCamara()
        }
    }

    // Função que verifica se já temos permissão
    private fun verificarPermissaoEAbrirCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Se já tem permissão, abre a câmara
            abrirCamara()
        } else {
            // Se não tem, pede permissão ao utilizador
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Lançador para pedir a permissão (janela do sistema "Permitir que app use a câmara?")
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            abrirCamara()
        } else {
            Toast.makeText(context, "É necessário permitir a câmara para tirar fotos.", Toast.LENGTH_LONG).show()
        }
    }

    private fun abrirCamara() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            resultLauncher.launch(cameraIntent)
        } catch (e: Exception) {
            // Este bloco evita que a app vá para uma "página branca" (crash) se não houver app de câmara
            e.printStackTrace()
            Toast.makeText(context, "Erro ao abrir a câmara: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Recebe a foto tirada
    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            // Recupera a imagem pequena (thumbnail)
            val imageBitmap = data?.extras?.get("data") as? Bitmap
            imageView?.setImageBitmap(imageBitmap)
        }
    }
}
