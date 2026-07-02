package com.royalshield.app

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.royalshield.app.data.SosRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Manages the camera using CameraX to take an emergency photo and send it to an API.
 * Integrated with Lifecycle for background usage in Service or Activity.
 */
class EmergencyCameraManager(private val context: Context) {

    private var imageCapture: ImageCapture? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /**
     * Initializes CameraX and binds it to the provided LifecycleOwner.
     * Use the front camera by default for SOS evidence.
     */
    fun initCamera(lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                // 1. Configure ImageCapture
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // 2. Select Front Camera
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                // 3. Unbind all and bind to lifecycle
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageCapture
                )

                Log.d("EmergencyCameraManager", "CameraX Initialized and Bound to Lifecycle.")
            } catch (exc: Exception) {
                Log.e("EmergencyCameraManager", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Takes a photo using CameraX and sends it to the server.
     */
    fun takePhotoAndSend() {
        val imageCapture = imageCapture ?: run {
            Log.e("EmergencyCameraManager", "Camera not initialized. Call initCamera() first.")
            return
        }

        // Create file for SOS evidence
        val sosDir = context.getExternalFilesDir("sos_evidence")
        if (sosDir != null && !sosDir.exists()) {
            sosDir.mkdirs()
        }
        val photoFile = File(sosDir, "SOS_${System.currentTimeMillis()}.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    Log.d("EmergencyCameraManager", "Photo saved successfully: $savedUri")
                    
                    // Process the image for cloud upload
                    processAndSendImage(photoFile)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("EmergencyCameraManager", "Photo capture failed: ${exc.message}", exc)
                }
            }
        )
    }

    private fun processAndSendImage(photoFile: File) {
        try {
            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
            
            // 1. Encode to Base64 for Firestore/Cloud
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64Photo = Base64.encodeToString(byteArray, Base64.DEFAULT)

            // 2. Upload to SosRepository
            CoroutineScope(Dispatchers.IO).launch {
                val repository = SosRepository()
                repository.sendSosAlert(
                    latitude = null,
                    longitude = null,
                    photoBase64 = base64Photo
                )
                Log.d("EmergencyCameraManager", "Photo evidence uploaded to cloud via CameraX.")
            }
        } catch (e: Exception) {
            Log.e("EmergencyCameraManager", "Error processing captured image", e)
        }
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }
}