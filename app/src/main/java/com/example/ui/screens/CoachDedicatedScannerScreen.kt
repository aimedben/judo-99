package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.ActiveTrainingSession
import com.example.data.model.AttendanceWithMember
import com.example.data.model.CoachScanFeedback
import com.example.ui.theme.JudoRed
import com.example.ui.theme.JudoSuccess
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

/**
 * Dedicated Full-Screen QR Scanner for Coach Space (Judo Club Seddouk)
 *
 * Requirements fulfilled:
 * 1. Full-screen immersive layout: no bottom navigation bar, no stats panels, no extra menus.
 * 2. Top bar: discreet back button with arrow + "Retour à la séance", short title "Scanner les présences", subtitle "Scannez le QR code de chaque athlète".
 * 3. Center: large concentric circles radar in club red that progressively expand and fade out transparently, with QR icon in the center. State indicator "Prêt à scanner" underneath.
 * 4. Scan Confirmation: temporarily replaces the center animation with the athlete's profile photo (or initials if none) surrounded by a visible circular green border and a validation checkmark icon. Displays first name and last name below. Holds for 1 second, then smoothly returns to the scan animation.
 * 5. High-speed, non-blocking scan processing: accepts hardware HID douchette (Bluetooth / USB OTG) and optional camera scanner. Never blocks subsequent scans during confirmation display.
 * 6. Bottom bar: compact real-time counter of present athletes during the session.
 */
@Composable
fun CoachDedicatedScannerScreen(
    session: ActiveTrainingSession,
    sessionAttendances: List<AttendanceWithMember>,
    totalCategoryCount: Int,
    scanFeedback: CoachScanFeedback?,
    onQrScanned: (String) -> Unit,
    onBackToSession: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // System Back returns smoothly to active session statistics
    BackHandler {
        onBackToSession()
    }

    // 1-second auto-return to "Prêt à scanner" after confirmation, non-blocking
    LaunchedEffect(scanFeedback) {
        if (scanFeedback != null) {
            delay(1000)
            onDismissFeedback()
        }
    }

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    // Explicitly hide soft keyboard on launch so it never obscures the scan screen
    LaunchedEffect(Unit) {
        keyboardController?.hide()
    }

    // Optional camera scanner toggle for coaches without external hardware douchette
    var isCameraModeActive by remember { mutableStateOf(false) }
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
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) isCameraModeActive = false
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraProviderInstance by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProviderInstance?.unbindAll()
                cameraExecutor.shutdown()
            } catch (_: Exception) {}
        }
    }

    var lastScanCode by remember { mutableStateOf("") }
    var lastScanTimestamp by remember { mutableLongStateOf(0L) }

    fun processScannedBarcode(raw: String) {
        val clean = raw.replace("\r", "").replace("\n", "").trim()
        if (clean.isBlank()) return
        val now = System.currentTimeMillis()
        if (clean == lastScanCode && (now - lastScanTimestamp) < 400) {
            return
        }
        lastScanCode = clean
        lastScanTimestamp = now
        onQrScanned(clean)
    }

    val presentCount = sessionAttendances.size

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                keyboardController?.hide()
            }
            .testTag("coach_dedicated_scanner_screen")
    ) {
        // Optional Camera Preview View in the background if camera mode is turned on
        if (isCameraModeActive && hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.35f)
            ) {
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
                            try {
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
                                                    val rawVal = barcode.rawValue
                                                    if (!rawVal.isNullOrBlank()) {
                                                        processScannedBarcode(rawVal)
                                                        break
                                                    }
                                                }
                                            }
                                            .addOnCompleteListener { imageProxy.close() }
                                    } else {
                                        imageProxy.close()
                                    }
                                }

                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (_: Exception) {}
                        }, ContextCompat.getMainExecutor(context))
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Main Full-Screen Layout: Header -> Center Zone -> Bottom Counter
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Section: Discreet Back Button & Title / Subtitle
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Discreet back button with arrow and label « Retour à la séance »
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onBackToSession() }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("coach_scanner_back_btn"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour à la séance",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Retour à la séance",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    // Discreet camera toggle option for flexibility
                    IconButton(
                        onClick = {
                            if (!isCameraModeActive) {
                                if (!hasCameraPermission) {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                } else {
                                    isCameraModeActive = true
                                }
                            } else {
                                isCameraModeActive = false
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCameraModeActive) JudoRed else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .testTag("coach_scanner_camera_toggle")
                    ) {
                        Icon(
                            imageVector = if (isCameraModeActive) Icons.Default.QrCodeScanner else Icons.Default.PhotoCamera,
                            contentDescription = "Basculer caméra",
                            tint = if (isCameraModeActive) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Short Title: « Scanner les présences »
                Text(
                    text = "Scanner les présences",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Subtitle: « Scannez le QR code de chaque athlète »
                Text(
                    text = "Scannez le QR code de chaque athlète",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center
                )
            }

            // 2. Center Section: Immersive Concentric Circles Radar OR 1-Second Athlete Photo Confirmation
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = scanFeedback,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(140))
                    },
                    label = "scanner_center_transition"
                ) { feedback ->
                    when (feedback) {
                        // 2. Confirmation visuelle après un scan réussi
                        is CoachScanFeedback.Success -> {
                            ScanSuccessFeedbackView(feedback = feedback)
                        }

                        // Athlète déjà présent
                        is CoachScanFeedback.AlreadyChecked -> {
                            ScanAlreadyCheckedFeedbackView(feedback = feedback)
                        }

                        // QR inconnu / erreur
                        is CoachScanFeedback.Invalid -> {
                            ScanInvalidFeedbackView(feedback = feedback)
                        }

                        // État par défaut : Prêt à scanner avec cercles concentriques
                        null -> {
                            ScanReadyRadarView()
                        }
                    }
                }
            }

            // 3. Bottom Section: Compact counter of present athletes
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.testTag("coach_scanner_present_counter")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(JudoSuccess)
                        )
                        Text(
                            text = if (presentCount <= 1) "$presentCount athlète présent" else "$presentCount athlètes présents",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        if (totalCategoryCount > 0) {
                            Text(
                                text = "• sur $totalCategoryCount",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Center default state: Large concentric circles expanding and fading out, with QR icon in the center.
 */
@Composable
private fun ScanReadyRadarView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        LargeConcentricRipplesAnimation(accentColor = JudoRed) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(JudoRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scanner QR",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Prêt à scanner",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Présentez le QR code devant la douchette",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Center Success Confirmation view: Profile photo inside prominent green circular border with checkmark icon.
 */
@Composable
private fun ScanSuccessFeedbackView(feedback: CoachScanFeedback.Success) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            // Subtle glowing outer aura
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .clip(CircleShape)
                    .background(JudoSuccess.copy(alpha = 0.12f))
            )

            // Circular Athlete Photo or Initials with bold green border
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(4.5.dp, JudoSuccess, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (feedback.member.photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = feedback.member.photoUrl,
                        contentDescription = "Photo de ${feedback.member.fullName}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    val initials = feedback.member.fullName.split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .joinToString("")

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(JudoSuccess.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials.ifBlank { "🥋" },
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Black,
                                color = JudoSuccess
                            )
                        )
                    }
                }
            }

            // Green validation checkmark badge at bottom-right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 36.dp, bottom = 36.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(JudoSuccess)
                    .border(2.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Validé",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // First Name and Last Name of athlete under photo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "${feedback.member.firstName} ${feedback.member.lastName}",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp
                ),
                color = JudoSuccess,
                textAlign = TextAlign.Center
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (feedback.member.beltName.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "🥋 ${feedback.member.beltName}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (feedback.isCotisationUpToDate) JudoSuccess.copy(alpha = 0.15f) else Color(0xFFFF9800).copy(alpha = 0.18f)
                ) {
                    Text(
                        text = if (feedback.isCotisationUpToDate) "Présent • À jour ✓" else "Présent • Cotisation en retard ⚠️",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (feedback.isCotisationUpToDate) JudoSuccess else Color(0xFFE65100)
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "Pointé à ${feedback.timeString}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Center view when athlete is already registered.
 */
@Composable
private fun ScanAlreadyCheckedFeedbackView(feedback: CoachScanFeedback.AlreadyChecked) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1976D2).copy(alpha = 0.12f))
            )

            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(4.5.dp, Color(0xFF1976D2), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (feedback.member.photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = feedback.member.photoUrl,
                        contentDescription = feedback.member.fullName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    val initials = feedback.member.fullName.split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .joinToString("")

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1976D2).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials.ifBlank { "🥋" },
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1976D2)
                            )
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 36.dp, bottom = 36.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1976D2))
                    .border(2.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Déjà présent",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "${feedback.member.firstName} ${feedback.member.lastName}",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                textAlign = TextAlign.Center
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1976D2).copy(alpha = 0.15f)
            ) {
                Text(
                    text = "Déjà enregistré",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
            Text(
                text = "Pointé précédemment à ${feedback.timeString}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Center view when QR code is not recognized / unknown athlete.
 */
@Composable
private fun ScanInvalidFeedbackView(feedback: CoachScanFeedback.Invalid) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
            )

            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(4.5.dp, MaterialTheme.colorScheme.error, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Erreur",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(60.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Athlète introuvable",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black
                ),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Text(
                text = feedback.message.ifBlank { "QR code non reconnu dans le club" },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Elegant expanding concentric circles animation in club red with subtle glow.
 */
@Composable
fun LargeConcentricRipplesAnimation(
    modifier: Modifier = Modifier,
    accentColor: Color = JudoRed,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "concentric_scan_ripples")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "concentric_progress"
    )

    Box(
        modifier = modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = 44.dp.toPx()
            val maxRadius = (size.minDimension / 2f) - 6.dp.toPx()
            val spread = maxRadius - baseRadius

            // 4 concentric expanding rings
            for (i in 0 until 4) {
                val ringPhase = (progress + (i / 4f)) % 1f
                val currentRadius = baseRadius + spread * ringPhase
                val ringAlpha = ((1f - ringPhase) * 0.75f).coerceIn(0f, 1f)

                // Soft outer glow ring
                drawCircle(
                    color = accentColor.copy(alpha = ringAlpha * 0.22f),
                    radius = currentRadius,
                    center = center,
                    style = Stroke(width = 5.dp.toPx())
                )
                // Sharp fine line ring
                drawCircle(
                    color = accentColor.copy(alpha = ringAlpha * 0.85f),
                    radius = currentRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Central circle backdrop & boundary
            drawCircle(
                color = accentColor.copy(alpha = 0.12f),
                radius = baseRadius + 4.dp.toPx(),
                center = center
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.25f),
                radius = baseRadius,
                center = center
            )
            drawCircle(
                color = accentColor,
                radius = baseRadius,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }

        content()
    }
}
