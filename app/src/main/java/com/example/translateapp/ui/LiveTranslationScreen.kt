package com.example.translateapp.ui

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.translateapp.ui.theme.TranslateAppTheme
import com.example.translateapp.viewmodel.AppViewModel
import com.example.translateapp.viewmodel.LanguageOption
import com.example.translateapp.viewmodel.SUPPORTED_LANGUAGES
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LiveTranslationScreen(vm: AppViewModel, onDismiss: () -> Unit) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val targetLang by vm.translateTargetLang.collectAsState()

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    LiveTranslationScreenContent(
        isPermissionGranted = cameraPermissionState.status.isGranted,
        targetLang = targetLang,
        onTargetLangChange = { vm.setTranslateTargetLang(it) },
        onTranslateNow = { text ->
            vm.onTranslateInputChange(text)
            onDismiss()
        },
        onDismiss = onDismiss
    )
}

@Composable
fun LiveTranslationScreenContent(
    isPermissionGranted: Boolean,
    targetLang: LanguageOption,
    onTargetLangChange: (LanguageOption) -> Unit,
    onTranslateNow: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (isPermissionGranted) {
        CameraView(
            targetLang = targetLang,
            onTargetLangChange = onTargetLangChange,
            onTranslateNow = onTranslateNow,
            onDismiss = onDismiss
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission is required for Live Translation")
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
private fun CameraView(
    targetLang: LanguageOption,
    onTargetLangChange: (LanguageOption) -> Unit,
    onTranslateNow: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    
    var recognizedTextBlocks by remember { mutableStateOf<List<TextBlockInfo>>(emptyList()) }
    var isFlashEnabled by remember { mutableStateOf(false) }
    val cameraControl = remember { mutableStateOf<CameraControl?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    
                    val preview = androidx.camera.core.Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            recognizer.process(image)
                                .addOnSuccessListener { visionText ->
                                    val blocks = visionText.textBlocks.map { block ->
                                        TextBlockInfo(
                                            text = block.text,
                                            boundingBox = block.boundingBox,
                                        )
                                    }
                                    recognizedTextBlocks = blocks
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                        cameraControl.value = camera.cameraControl
                    } catch (e: Exception) {
                        Log.e("LiveTranslation", "Use case binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay for bounding boxes (simplified AR)
        Canvas(modifier = Modifier.fillMaxSize()) {
            recognizedTextBlocks.forEach { block ->
                block.boundingBox?.let { rect ->
                    drawRect(
                        color = Color.Cyan,
                        topLeft = Offset(rect.left.toFloat(), rect.top.toFloat()),
                        size = Size(rect.width().toFloat(), rect.height().toFloat()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        // Top UI Controls
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            color = Color.Black.copy(alpha = 0.4f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
                
                // Language Selection in AR Mode
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Target:", 
                        style = MaterialTheme.typography.labelMedium, 
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    LanguageDropdown(
                        selected = targetLang,
                        onSelect = onTargetLangChange,
                        options = SUPPORTED_LANGUAGES,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                IconButton(
                    onClick = {
                        isFlashEnabled = !isFlashEnabled
                        cameraControl.value?.enableTorch(isFlashEnabled)
                    }
                ) {
                    Icon(
                        if (isFlashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Flash",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Info Card
        if (recognizedTextBlocks.isNotEmpty()) {
            val mostProminentBlock = recognizedTextBlocks.maxByOrNull { it.text.length }
            mostProminentBlock?.let { block ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Detected Text", style = MaterialTheme.typography.labelSmall)
                            Button(
                                onClick = { onTranslateNow(block.text) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Translate Now", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            block.text, 
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

data class TextBlockInfo(
    val text: String,
    val boundingBox: android.graphics.Rect?
)

@Preview(showBackground = true)
@Composable
fun LiveTranslationScreenPreview() {
    TranslateAppTheme {
        LiveTranslationScreenContent(
            isPermissionGranted = true,
            targetLang = SUPPORTED_LANGUAGES[1], // Spanish
            onTargetLangChange = {},
            onTranslateNow = {},
            onDismiss = {}
        )
    }
}
