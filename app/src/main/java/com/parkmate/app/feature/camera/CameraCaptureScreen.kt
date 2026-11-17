package com.parkmate.app.feature.camera

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.parkmate.app.R
import com.parkmate.app.core.ui.theme.ParkImmersiveDark

@Composable
fun CameraCaptureScreen(
    viewModel: CameraFlowViewModel,
    onClose: () -> Unit,
    onPhotoCaptured: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var flashEnabled by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var cameraAvailable by remember { mutableStateOf(true) }

    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importGalleryPhoto(uri, onPhotoCaptured)
        }
    }

    LaunchedEffect(lensFacing, flashEnabled) {
        imageCapture.flashMode = if (flashEnabled) {
            ImageCapture.FLASH_MODE_ON
        } else {
            ImageCapture.FLASH_MODE_OFF
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            runCatching {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val selector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageCapture
                )
                cameraAvailable = true
            }.onFailure {
                cameraAvailable = false
            }
        }, ContextCompat.getMainExecutor(context))
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val triggerCapture = {
        if (!cameraAvailable) {
            viewModel.captureFallbackSamplePhoto(onPhotoCaptured)
        } else {
            previewView.display?.rotation?.let { rot ->
                imageCapture.targetRotation = rot
            }
            val outFile = viewModel.createCaptureFile()
            val outputOptions = ImageCapture.OutputFileOptions.Builder(outFile).build()
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        viewModel.onCameraFileCaptured(outFile.absolutePath, onPhotoCaptured)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        viewModel.captureFallbackSamplePhoto(onPhotoCaptured)
                    }
                }
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ParkImmersiveDark)
    ) {
        val isLandscape = maxWidth > maxHeight

        if (cameraAvailable) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Stylized underground garage viewfinder fallback when running on an emulator without virtual camera
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(Color(0xFF1F2430))
                drawRoundRect(
                    color = Color(0xFF394152),
                    topLeft = Offset(size.width * 0.55f, size.height * 0.18f),
                    size = Size(size.width * 0.28f, size.height * 0.56f),
                    cornerRadius = CornerRadius(18f, 18f)
                )
                drawRect(
                    color = Color(0xFF3B6CF6),
                    topLeft = Offset(size.width * 0.55f, size.height * 0.34f),
                    size = Size(size.width * 0.28f, size.height * 0.09f)
                )
            }
        }

        // Corner frame guides with orientation-aware padding
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (isLandscape) 86.dp else 38.dp,
                    end = if (isLandscape) 144.dp else 38.dp,
                    top = statusBarTop + if (isLandscape) 56.dp else 110.dp,
                    bottom = if (isLandscape) 40.dp else 196.dp
                )
        ) {
            val guideLen = 34.dp.toPx()
            val stroke = Stroke(width = 2.5.dp.toPx())
            val color = Color.White.copy(alpha = 0.78f)
            val w = size.width
            val h = size.height
            if (w > guideLen * 2 && h > guideLen * 2) {
                // Top-left
                drawLine(color, Offset(0f, 0f), Offset(guideLen, 0f), stroke.width)
                drawLine(color, Offset(0f, 0f), Offset(0f, guideLen), stroke.width)
                // Top-right
                drawLine(color, Offset(w - guideLen, 0f), Offset(w, 0f), stroke.width)
                drawLine(color, Offset(w, 0f), Offset(w, guideLen), stroke.width)
                // Bottom-left
                drawLine(color, Offset(0f, h - guideLen), Offset(0f, h), stroke.width)
                drawLine(color, Offset(0f, h), Offset(guideLen, h), stroke.width)
                // Bottom-right
                drawLine(color, Offset(w - guideLen, h), Offset(w, h), stroke.width)
                drawLine(color, Offset(w, h - guideLen), Offset(w, h), stroke.width)
            }
        }

        // Top Bar (Close & Flash toggle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = statusBarTop + 12.dp,
                    start = 16.dp,
                    end = if (isLandscape) 128.dp else 16.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0x610F1115),
                modifier = Modifier.size(48.dp)
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_close_camera),
                        tint = Color.White
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0x610F1115),
                modifier = Modifier.size(48.dp)
            ) {
                IconButton(onClick = { flashEnabled = !flashEnabled }) {
                    Icon(
                        imageVector = if (flashEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                        contentDescription = stringResource(R.string.cd_toggle_flash),
                        tint = Color.White
                    )
                }
            }
        }

        // Hint Pill
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0x85101216),
            modifier = Modifier
                .align(if (isLandscape) Alignment.BottomStart else Alignment.BottomCenter)
                .padding(
                    start = if (isLandscape) 24.dp else 0.dp,
                    bottom = if (isLandscape) 18.dp else 148.dp
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.camera_hint),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (isLandscape) {
            // Vertical right-rail camera controls in Landscape
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .navigationBarsPadding()
                    .padding(end = 24.dp, top = statusBarTop + 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Flip camera button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x610F1115),
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cameraswitch,
                            contentDescription = stringResource(R.string.cd_flip_camera),
                            tint = Color.White
                        )
                    }
                }

                // Shutter Button (72dp)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.White, CircleShape)
                        .clickable { triggerCapture() }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                // Gallery Picker button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF333742),
                    border = BorderStroke(2.dp, Color.White.copy(alpha = 0.75f)),
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoLibrary,
                            contentDescription = stringResource(R.string.cd_open_gallery),
                            tint = Color.White
                        )
                    }
                }
            }
        } else {
            // Bottom Camera Controls in Portrait (Gallery Picker, Shutter, Camera Flip)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF333742),
                    border = BorderStroke(2.dp, Color.White.copy(alpha = 0.75f)),
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoLibrary,
                            contentDescription = stringResource(R.string.cd_open_gallery),
                            tint = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.White, CircleShape)
                        .clickable { triggerCapture() }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x610F1115),
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cameraswitch,
                            contentDescription = stringResource(R.string.cd_flip_camera),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
