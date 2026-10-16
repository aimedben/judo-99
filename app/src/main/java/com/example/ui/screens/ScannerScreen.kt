package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.model.Member
import com.example.ui.theme.JudoGold
import com.example.ui.theme.JudoRed
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

enum class ScannerTargetMode {
    POINTAGE,
    EXTRACTION_MEMBRE
}

@Composable
fun ScannerScreen(
    members: List<Member>,
    initialMode: ScannerTargetMode = ScannerTargetMode.POINTAGE,
    onQrScanned: (String) -> Unit,
    onCardExtractedForCreation: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var activeMode by remember { mutableStateOf(initialMode) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var isTorchOn by remember { mutableStateOf(false) }
    var boostLighting by remember { mutableStateOf(true) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraInfo by remember { mutableStateOf<CameraInfo?>(null) }
    var maxExposureIndex by remember { mutableStateOf(0) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var manualInputText by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }
    var cameraProviderInstance by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProviderInstance?.unbindAll()
                cameraExecutor.shutdown()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(boostLighting, cameraControl) {
        cameraControl?.let { ctrl ->
            val expState = cameraInfo?.exposureState
            if (expState?.isExposureCompensationSupported == true) {
                val target = if (boostLighting) expState.exposureCompensationRange.upper else 0
                ctrl.setExposureCompensationIndex(target)
            }
        }
    }

    LaunchedEffect(isTorchOn, cameraControl) {
        cameraControl?.enableTorch(isTorchOn)
    }

    fun handleScannedCode(raw: String) {
        if (activeMode == ScannerTargetMode.EXTRACTION_MEMBRE) {
            onCardExtractedForCreation(raw)
        } else {
            onQrScanned(raw)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("scanner_screen")
    ) {
        if (hasCameraPermission) {
            // Camera Preview View with TextureView COMPATIBLE mode (avoids SurfaceView abandoned buffer queues in Compose)
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                update = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProviderInstance = cameraProvider
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val barcodeScanner = BarcodeScanning.getClient()

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            @OptIn(ExperimentalGetImage::class)
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                barcodeScanner.process(inputImage)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            val rawValue = barcode.rawValue
                                            if (!rawValue.isNullOrBlank()) {
                                                handleScannedCode(rawValue)
                                                break
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

                        val cameraSelector = if (useFrontCamera) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            cameraControl = camera.cameraControl
                            cameraInfo = camera.cameraInfo
                            val expState = camera.cameraInfo.exposureState
                            if (expState.isExposureCompensationSupported) {
                                maxExposureIndex = expState.exposureCompensationRange.upper
                                val targetIndex = if (boostLighting) maxExposureIndex else 0
                                camera.cameraControl.setExposureCompensationIndex(targetIndex)
                            }
                            if (isTorchOn) {
                                camera.cameraControl.enableTorch(true)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(context))
                },
                onRelease = {
                    try {
                        cameraProviderInstance?.unbindAll()
                    } catch (_: Exception) {}
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Camera permission fallback
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = JudoRed,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "Accès à la caméra requis",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "L'application a besoin de la caméra pour scanner instantanément les cartes des judokas.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = JudoRed),
                            modifier = Modifier.fillMaxWidth().testTag("grant_camera_permission_button")
                        ) {
                            Text("Autoriser la caméra")
                        }
                    }
                }
            }
        }

        // Viewfinder Scanner Reticle Overlay
        val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
        val laserProgress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "laser_pos"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val boxSize = size.minDimension * 0.70f
            val left = (size.width - boxSize) / 2f
            val top = (size.height - boxSize) / 2.3f
            val rect = Rect(left, top, left + boxSize, top + boxSize)

            // Dim surroundings (lighter alpha 0.28f so camera preview remains bright and clear in dim dojos)
            val path = Path().apply {
                addRect(Rect(0f, 0f, size.width, size.height))
                addRoundRect(RoundRect(rect, CornerRadius(24f, 24f)))
            }
            drawPath(path, Color.Black.copy(alpha = 0.28f))

            // Corner target accents
            val cornerLength = 36f
            val strokeWidth = 5f
            val cornerColor = if (activeMode == ScannerTargetMode.EXTRACTION_MEMBRE) JudoGold else JudoRed

            // Top-Left
            drawLine(cornerColor, Offset(rect.left, rect.top), Offset(rect.left + cornerLength, rect.top), strokeWidth)
            drawLine(cornerColor, Offset(rect.left, rect.top), Offset(rect.left, rect.top + cornerLength), strokeWidth)

            // Top-Right
            drawLine(cornerColor, Offset(rect.right, rect.top), Offset(rect.right - cornerLength, rect.top), strokeWidth)
            drawLine(cornerColor, Offset(rect.right, rect.top), Offset(rect.right, rect.top + cornerLength), strokeWidth)

            // Bottom-Left
            drawLine(cornerColor, Offset(rect.left, rect.bottom), Offset(rect.left + cornerLength, rect.bottom), strokeWidth)
            drawLine(cornerColor, Offset(rect.left, rect.bottom), Offset(rect.left, rect.bottom - cornerLength), strokeWidth)

            // Bottom-Right
            drawLine(cornerColor, Offset(rect.right, rect.bottom), Offset(rect.right - cornerLength, rect.bottom), strokeWidth)
            drawLine(cornerColor, Offset(rect.right, rect.bottom), Offset(rect.right, rect.bottom - cornerLength), strokeWidth)

            // Animated Laser Line
            val laserY = rect.top + (rect.height * laserProgress)
            drawLine(
                color = cornerColor,
                start = Offset(rect.left + 8f, laserY),
                end = Offset(rect.right - 8f, laserY),
                strokeWidth = 3.5f
            )
        }

        // Top Navigation, Flash Bar & Mode Selector
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .testTag("scanner_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = Color.White
                    )
                }

                Text(
                    text = if (activeMode == ScannerTargetMode.EXTRACTION_MEMBRE) "Scanner Carte Sport" else "Scanner Pointage",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bouton boost éclairage/lumière (EV compensation)
                    Surface(
                        onClick = { boostLighting = !boostLighting },
                        shape = RoundedCornerShape(20.dp),
                        color = if (boostLighting) JudoGold else Color.Black.copy(alpha = 0.6f),
                        contentColor = if (boostLighting) Color.Black else Color.White,
                        modifier = Modifier.testTag("toggle_boost_lighting_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (boostLighting) Color.Black else JudoGold
                            )
                            Text(
                                text = if (boostLighting) "Lumière +" else "Std",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            isTorchOn = !isTorchOn
                            cameraControl?.enableTorch(isTorchOn)
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isTorchOn) JudoGold else Color.Black.copy(alpha = 0.6f))
                            .testTag("toggle_torch_button")
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Lampe torche",
                            tint = if (isTorchOn) Color.Black else Color.White
                        )
                    }

                    IconButton(
                        onClick = { useFrontCamera = !useFrontCamera },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .testTag("flip_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Changer caméra",
                            tint = Color.White
                        )
                    }
                }
            }

            // Mode Selector: Pointage vs Extraction Carte
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = activeMode == ScannerTargetMode.POINTAGE,
                    onClick = { activeMode = ScannerTargetMode.POINTAGE },
                    label = { Text("🥋 Pointage séance") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = JudoRed,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = activeMode == ScannerTargetMode.EXTRACTION_MEMBRE,
                    onClick = { activeMode = ScannerTargetMode.EXTRACTION_MEMBRE },
                    label = { Text("📋 Extraire & Inscrire") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = JudoGold,
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Bottom Controls and Card Simulation / Manual Entry
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (activeMode == ScannerTargetMode.EXTRACTION_MEMBRE)
                    "Cadrez la carte de sport pour extraire automatiquement nom, groupe et ceinture"
                else
                    "Placez le QR Code dans le cadre pour enregistrer la présence",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )

            // Test cards bar or manual text input
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (activeMode == ScannerTargetMode.EXTRACTION_MEMBRE)
                                "Cartes de sport à tester (1 tap)"
                            else
                                "Cartes du club à tester (1 tap)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Text(
                            text = if (showManualInput) "Fermer saisie" else "Saisie manuelle",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = JudoRed,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.clickable { showManualInput = !showManualInput }
                        )
                    }

                    if (showManualInput) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = manualInputText,
                                onValueChange = { manualInputText = it },
                                placeholder = { Text("Code QR ou données de carte...") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("manual_qr_input"),
                                trailingIcon = {
                                    Icon(Icons.Default.QrCode, contentDescription = null)
                                }
                            )

                            Button(
                                onClick = {
                                    if (manualInputText.isNotBlank()) {
                                        handleScannedCode(manualInputText.trim())
                                        manualInputText = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = JudoRed),
                                modifier = Modifier.testTag("submit_manual_qr_button")
                            ) {
                                Text("Scanner")
                            }
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (activeMode == ScannerTargetMode.EXTRACTION_MEMBRE) {
                            // Extraction simulation cards
                            item {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = JudoGold.copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, JudoGold),
                                    modifier = Modifier
                                        .clickable {
                                            val seddoukQr = """
                                                JUDO CLUB SEDDOUK
                                                "
                                                N: 1

                                                Adherent: babi BOURENANE

                                                Ne(e) le: 2013-01-06

                                                Tel tuteur: 0782487120

                                                Saison: 2026/2027
                                                "
                                            """.trimIndent()
                                            handleScannedCode(seddoukQr)
                                        }
                                        .testTag("test_card_seddouk_babi")
                                ) {
                                    Text(
                                        text = "🥋 Carte Seddouk (babi BOURENANE)",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            item {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = JudoGold.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, JudoGold),
                                    modifier = Modifier
                                        .clickable {
                                            val jsonCard = """
                                                {"nom":"Belaid","prenom":"Karim","naissance":"2012-09-14","groupe":"U15","ceinture":"VERTE","tel":"0552 44 88 12","id":"JS-CARD-2026-777"}
                                            """.trimIndent()
                                            handleScannedCode(jsonCard)
                                        }
                                        .testTag("test_card_json")
                                ) {
                                    Text(
                                        text = "⚡ Carte JSON (Karim Belaid)",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            item {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = JudoRed.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, JudoRed),
                                    modifier = Modifier
                                        .clickable {
                                            val delimited = "JS|Mansouri|Sarah|2014-02-10|U13|ORANGE|0667123456"
                                            handleScannedCode(delimited)
                                        }
                                        .testTag("test_card_delimited")
                                ) {
                                    Text(
                                        text = "⚡ Badge Club (Sarah Mansouri)",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            item {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = CardDefaults.outlinedCardBorder(),
                                    modifier = Modifier
                                        .clickable {
                                            val vcard = """
                                                BEGIN:VCARD
                                                FN:Amine Dahmani
                                                TEL:0771 22 33 44
                                                NOTE:U18 Marron
                                                END:VCARD
                                            """.trimIndent()
                                            handleScannedCode(vcard)
                                        }
                                        .testTag("test_card_vcard")
                                ) {
                                    Text(
                                        text = "⚡ vCard (Amine Dahmani)",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        } else {
                            // Attendance simulation cards
                            items(members.take(7)) { member ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = CardDefaults.outlinedCardBorder(),
                                    modifier = Modifier
                                        .clickable { handleScannedCode(member.qrCode) }
                                        .testTag("scan_test_member_${member.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = member.fullName,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = member.qrCode,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 9.sp
                                            )
                                        )
                                    }
                                }
                            }

                            item {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = JudoRed.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, JudoRed),
                                    modifier = Modifier
                                        .clickable { handleScannedCode("JS-CARD-NOUVELLE-999") }
                                        .testTag("scan_test_unassigned_card")
                                ) {
                                    Text(
                                        text = "+ Carte vierge",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = JudoRed
                                        )
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
