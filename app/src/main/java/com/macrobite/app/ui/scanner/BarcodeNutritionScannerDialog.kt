package com.macrobite.app.ui.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.macrobite.app.R
import com.macrobite.app.data.scanner.BarcodeNutritionResolver
import com.macrobite.app.data.scanner.ScannedFoodResult
import com.macrobite.app.domain.model.MealCategory
import com.macrobite.app.domain.model.MealEntry
import com.macrobite.app.ui.theme.CarbsGreen
import com.macrobite.app.ui.theme.FatsCoral
import com.macrobite.app.ui.theme.ProteinBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

enum class ScannerMode {
    BARCODE,
    NUTRITION_LABEL
}

@Composable
fun BarcodeNutritionScannerDialog(
    customBarcodes: Map<String, String> = emptyMap(),
    onDismiss: () -> Unit,
    onFoodScanned: (MealEntry) -> Unit,
    onAnalyzeLabelPhoto: (String, String?) -> Unit,
    onBarcodeUnknown: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            if (!hasCameraPermission) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Camera Permission Needed",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Camera access is needed to scan food barcodes and nutrition labels.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Grant Permission")
                        }
                    }
                }
                return@Surface
            }

            var scannerMode by remember { mutableStateOf(ScannerMode.BARCODE) }
            var isTorchOn by remember { mutableStateOf(false) }
            var cameraControl by remember { mutableStateOf<Camera?>(null) }
            var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

            var isResolving by remember { mutableStateOf(false) }
            var resolvedResult by remember { mutableStateOf<ScannedFoodResult?>(null) }
            var scanStatusMessage by remember { mutableStateOf<String?>(null) }
            var lastScannedBarcode by remember { mutableStateOf<String?>(null) }
            var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
            var analyzerExecutorRef by remember { mutableStateOf<java.util.concurrent.ExecutorService?>(null) }
            var barcodeScannerRef by remember { mutableStateOf<com.google.mlkit.vision.barcode.BarcodeScanner?>(null) }

            // Auto-flash based on ambient light
            DisposableEffect(Unit) {
                val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
                val lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        val lux = event?.values?.firstOrNull() ?: return
                        if (lux < 10f && !isTorchOn && cameraControl != null) {
                            cameraControl?.cameraControl?.enableTorch(true)
                            isTorchOn = true
                        }
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }
                if (lightSensor != null) {
                    sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
                }
                onDispose {
                    sensorManager?.unregisterListener(listener)
                }
            }

            // Cleanup camera resources on dismiss
            DisposableEffect(Unit) {
                onDispose {
                    try { cameraControl?.cameraControl?.enableTorch(false) } catch (_: Throwable) {}
                    try { cameraProviderRef?.unbindAll() } catch (_: Throwable) {}
                    try { analyzerExecutorRef?.shutdown() } catch (_: Throwable) {}
                    try { barcodeScannerRef?.close() } catch (_: Throwable) {}
                }
            }

            val themePrimary = MaterialTheme.colorScheme.primary

            // Scanner Laser Animation
            val infiniteTransition = rememberInfiniteTransition(label = "scanner_anim")
            val laserPosition by infiniteTransition.animateFloat(
                initialValue = 0.05f,
                targetValue = 0.95f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "laser_y"
            )

            val cornerBreathing by infiniteTransition.animateFloat(
                initialValue = 0.65f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "corner_glow"
            )

            Box(modifier = Modifier.fillMaxSize()) {
                // 1. Live Camera Preview with Barcode ImageAnalysis
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        val analysisExecutor = Executors.newSingleThreadExecutor()
                        analyzerExecutorRef = analysisExecutor

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            cameraProviderRef = cameraProvider
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCapture = capture

                            val barcodeScanner = BarcodeScanning.getClient()
                            barcodeScannerRef = barcodeScanner

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null && !isResolving && resolvedResult == null && scannerMode == ScannerMode.BARCODE) {
                                    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    barcodeScanner.process(inputImage)
                                        .addOnSuccessListener { barcodes ->
                                            val validBarcode = barcodes.firstOrNull { it.rawValue != null }?.rawValue
                                            if (!validBarcode.isNullOrBlank() && validBarcode != lastScannedBarcode) {
                                                coroutineScope.launch(Dispatchers.Main) {
                                                    isResolving = true
                                                    scanStatusMessage = "Found barcode: $validBarcode! Fetching nutrition..."
                                                }
                                                coroutineScope.launch {
                                                    // 1. Check Custom Barcodes memory FIRST
                                                    val customJson = customBarcodes[validBarcode]
                                                    var customResult: ScannedFoodResult? = null
                                                    if (!customJson.isNullOrBlank()) {
                                                        try {
                                                            val entry = com.google.gson.Gson().fromJson(customJson, MealEntry::class.java)
                                                            customResult = ScannedFoodResult(
                                                                barcode = validBarcode,
                                                                name = entry.foodName,
                                                                calories = entry.calories,
                                                                protein = entry.protein,
                                                                carbs = entry.carbs,
                                                                fats = entry.fats,
                                                                fiber = entry.fiber,
                                                                sugar = entry.sugar,
                                                                sodium = entry.sodium,
                                                                servingSize = entry.portion,
                                                                brand = "Custom"
                                                            )
                                                        } catch (e: Exception) {
                                                            Log.e("ScannerDialog", "Failed to parse custom barcode", e)
                                                        }
                                                    }

                                                    val result = customResult ?: BarcodeNutritionResolver.resolveBarcode(validBarcode)
                                                    withContext(Dispatchers.Main) {
                                                        isResolving = false
                                                        if (result != null) {
                                                            resolvedResult = result
                                                            scanStatusMessage = null
                                                            try { cameraControl?.cameraControl?.enableTorch(false) } catch (_: Throwable) {}
                                                            isTorchOn = false
                                                        } else {
                                                            lastScannedBarcode = validBarcode
                                                            scanStatusMessage = "Barcode $validBarcode not in database."
                                                            try { cameraControl?.cameraControl?.enableTorch(false) } catch (_: Throwable) {}
                                                            isTorchOn = false
                                                        }
                                                    }
                                                }
                                            }
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
                                val cam = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    capture,
                                    imageAnalysis
                                )
                                cameraControl = cam
                            } catch (e: Exception) {
                                Log.e("ScannerDialog", "Camera binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 2. Futuristic Reticle HUD with Animated Neon Laser
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val reticleW = 280.dp.toPx()
                    val reticleH = 180.dp.toPx()
                    val left = (w - reticleW) / 2f
                    val top = (h - reticleH) / 2f - 40.dp.toPx()
                    val right = left + reticleW
                    val bottom = top + reticleH

                    // Dimmed outer overlay
                    drawRect(
                        color = Color.Black.copy(alpha = 0.55f),
                        size = Size(w, h)
                    )

                    // Clear viewfinder window
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(left, top),
                        size = Size(reticleW, reticleH),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                        blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                    )

                    // Reticle subtle border
                    drawRoundRect(
                        color = themePrimary.copy(alpha = 0.25f),
                        topLeft = Offset(left, top),
                        size = Size(reticleW, reticleH),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // 4 Sci-Fi L-Bracket Corners
                    val cornerLen = 26.dp.toPx()
                    val strokeW = 3.5.dp.toPx()
                    val cornerColor = themePrimary.copy(alpha = cornerBreathing)

                    // Top-Left
                    drawLine(cornerColor, Offset(left, top), Offset(left + cornerLen, top), strokeW)
                    drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLen), strokeW)

                    // Top-Right
                    drawLine(cornerColor, Offset(right, top), Offset(right - cornerLen, top), strokeW)
                    drawLine(cornerColor, Offset(right, top), Offset(right, top + cornerLen), strokeW)

                    // Bottom-Left
                    drawLine(cornerColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeW)
                    drawLine(cornerColor, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeW)

                    // Bottom-Right
                    drawLine(cornerColor, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeW)
                    drawLine(cornerColor, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeW)

                    // Animated Laser Beam & Glow Curtain
                    if (resolvedResult == null && !isResolving) {
                        val currentLaserY = top + (reticleH * laserPosition)
                        val curtainHeight = 35.dp.toPx()

                        // Trailing laser curtain
                        val curtainBrush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                themePrimary.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.85f)
                            ),
                            startY = currentLaserY - curtainHeight,
                            endY = currentLaserY
                        )

                        drawRect(
                            brush = curtainBrush,
                            topLeft = Offset(left + 2.dp.toPx(), currentLaserY - curtainHeight),
                            size = Size(reticleW - 4.dp.toPx(), curtainHeight)
                        )

                        // Main bright laser beam
                        drawLine(
                            color = Color.White,
                            start = Offset(left + 2.dp.toPx(), currentLaserY),
                            end = Offset(right - 2.dp.toPx(), currentLaserY),
                            strokeWidth = 2.5.dp.toPx()
                        )
                    }
                }

                // 3. Top Action Controls (Close, Torch, Mode)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 36.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }

                    // Mode Switcher Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (scannerMode == ScannerMode.BARCODE) themePrimary else Color.Transparent)
                                .clickable { scannerMode = ScannerMode.BARCODE }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "🏷️ Barcode",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (scannerMode == ScannerMode.BARCODE) Color.Black else Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (scannerMode == ScannerMode.NUTRITION_LABEL) themePrimary else Color.Transparent)
                                .clickable { scannerMode = ScannerMode.NUTRITION_LABEL }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "📋 Nutrition Label",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (scannerMode == ScannerMode.NUTRITION_LABEL) Color.Black else Color.White
                            )
                        }
                    }

                    // Flashlight Button
                    IconButton(
                        onClick = {
                            val targetTorch = !isTorchOn
                            cameraControl?.cameraControl?.enableTorch(targetTorch)
                            isTorchOn = targetTorch
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isTorchOn) themePrimary else Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_flashlight),
                            contentDescription = "Torch",
                            tint = if (isTorchOn) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 4. Center-Bottom Status Messages & OCR Snap Button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (resolvedResult != null) 220.dp else 50.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        if (isResolving) {
                            CircularProgressIndicator(
                                color = themePrimary,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        scanStatusMessage?.let { msg ->
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = themePrimary,
                                textAlign = TextAlign.Center
                            )
                        } ?: Text(
                            text = if (scannerMode == ScannerMode.BARCODE) "Align food barcode within frame" else "Align nutrition table and tap scan below",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )

                        // If in Nutrition Label mode OR if barcode is unknown: Show prominent OCR capture button
                        if ((scannerMode == ScannerMode.NUTRITION_LABEL && resolvedResult == null) || (lastScannedBarcode != null && resolvedResult == null)) {
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = {
                                    val photoFile = File(context.cacheDir, "scan_label_${System.currentTimeMillis()}.jpg")
                                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                    imageCapture?.takePicture(
                                        outputOptions,
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                                onAnalyzeLabelPhoto(photoFile.absolutePath, lastScannedBarcode)
                                                onDismiss()
                                            }
                                            override fun onError(exc: ImageCaptureException) {
                                                Log.e("ScannerDialog", "Photo capture failed", exc)
                                            }
                                        }
                                    )
                                },
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_camera),
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (lastScannedBarcode != null) "Scan Nutrition Label to Save" else "Scan Nutrition Facts",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // 5. Scanned Product Result Card (Animated Slide Up)
                AnimatedVisibility(
                    visible = resolvedResult != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    resolvedResult?.let { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                // Product Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        item.brand?.let { b ->
                                            Text(
                                                text = b.uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.5.sp
                                                ),
                                                color = themePrimary
                                            )
                                        }
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(themePrimary.copy(alpha = 0.15f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "${item.calories} kcal",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = themePrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Macros Badges (Protein, Carbs, Fats)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Protein
                                    MacroBadge(
                                        label = "Protein",
                                        value = "%.1fg".format(item.protein),
                                        color = ProteinBlue,
                                        modifier = Modifier.weight(1f)
                                    )
                                    // Carbs
                                    MacroBadge(
                                        label = "Carbs",
                                        value = "%.1fg".format(item.carbs),
                                        color = CarbsGreen,
                                        modifier = Modifier.weight(1f)
                                    )
                                    // Fats
                                    MacroBadge(
                                        label = "Fats",
                                        value = "%.1fg".format(item.fats),
                                        color = FatsCoral,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Action Buttons: Log Food & Scan Another
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    TextButton(
                                        onClick = {
                                            resolvedResult = null
                                            scanStatusMessage = null
                                            lastScannedBarcode = null
                                        },
                                        modifier = Modifier.weight(0.8f)
                                    ) {
                                        Text("Scan Another")
                                    }

                                    Button(
                                        onClick = {
                                            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                                            val entry = MealEntry(
                                                date = today,
                                                category = MealCategory.SNACKS,
                                                foodName = item.name,
                                                portion = item.servingSize,
                                                calories = item.calories,
                                                protein = item.protein,
                                                carbs = item.carbs,
                                                fats = item.fats,
                                                fiber = item.fiber ?: 0f,
                                                sugar = item.sugar ?: 0f,
                                                sodium = item.sodium ?: 0f
                                            )
                                            onFoodScanned(entry)
                                            try { cameraControl?.cameraControl?.enableTorch(false) } catch (_: Throwable) {}
                                            onDismiss()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Text(
                                            text = "Log Meal",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroBadge(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
