package io.github.fabiocintra.fivelas_counter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import io.github.fabiocintra.fivelas_counter.camera.CameraPreview

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val context = LocalContext.current

            var cameraPermissionGranted by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }

            val permissionLauncher =
                rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->

                    cameraPermissionGranted = granted
                }

            LaunchedEffect(Unit) {

                if (!cameraPermissionGranted) {
                    permissionLauncher.launch(
                        Manifest.permission.CAMERA
                    )
                }
            }

            if (cameraPermissionGranted) {
                CameraPreview()
            }
        }
    }
}