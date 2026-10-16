package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveTrainingSession
import com.example.data.model.AttendanceWithMember
import com.example.data.model.CoachScanFeedback
import com.example.data.model.Member
import com.example.ui.components.JudokaAvatar
import com.example.ui.theme.JudoRed
import com.example.ui.theme.JudoSuccess
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen 3 — Pointage (Check-in during training)
 * Essential stats: presents, absents, attendance rate.
 * Big button: "Scanner un judoka" if camera mode.
 * Indicator: "Douchette prête" in external barcode mode.
 * Auto-handled continuous barcode input in douchette mode.
 * Real-time instant visual feedback banner:
 *   - Green: Present & cotisation up-to-date
 *   - Orange: Present but cotisation late
 *   - Red: Invalid QR or unknown judoka
 * Manual check-in button to mark a judoka present manually.
 * Recent check-ins list.
 * Finish session button.
 */
@Composable
fun CoachSessionPointageScreen(
    session: ActiveTrainingSession,
    categoryMembers: List<Member>,
    sessionAttendances: List<AttendanceWithMember>,
    scanFeedback: CoachScanFeedback?,
    onOpenScanner: () -> Unit,
    onQrScanned: (String) -> Unit,
    onManualCheckInMember: (Member) -> Unit,
    onFinishSession: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = categoryMembers.size
    val presentCount = sessionAttendances.size
    val absentCount = (totalCount - presentCount).coerceAtLeast(0)
    val attendanceRate = if (totalCount > 0) ((presentCount.toDouble() / totalCount.toDouble()) * 100).toInt() else 0

    var isManualCheckInDialogOpen by remember { mutableStateOf(false) }
    val douchetteFocusRequester = remember { FocusRequester() }

    // Auto-return to "Prêt à scanner" after 1-second feedback display
    LaunchedEffect(scanFeedback) {
        if (scanFeedback != null) {
            delay(1000)
            onDismissFeedback()
        }
    }

    val presentMemberIds = remember(sessionAttendances) {
        sessionAttendances.map { it.memberId }.toSet()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isManualCheckInDialogOpen) {
                    try { douchetteFocusRequester.requestFocus() } catch (_: Exception) {}
                }
            }
            .padding(16.dp)
            .testTag("coach_pointage_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header: Category & Start Time
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = session.categoryName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black
                        )
                    )
                    Text(
                        text = "Début : ${session.startTime} • Tatami Seddouk",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = JudoRed.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, JudoRed.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(JudoRed)
                        )
                        Text(
                            text = "Séance en cours",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = JudoRed
                            )
                        )
                    }
                }
            }
        }

        // Essential Stats: Présents | Absents | Taux
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatPill(
                title = "Présents",
                value = "$presentCount",
                color = JudoSuccess,
                modifier = Modifier.weight(1f)
            )
            StatPill(
                title = "Absents",
                value = "$absentCount",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
            StatPill(
                title = "Taux",
                value = "$attendanceRate%",
                color = JudoRed,
                modifier = Modifier.weight(1f)
            )
        }

        // 1. & 3. Central Scan Zone with Concentric Red Ripples & Real-Time Feedback
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(
                1.dp,
                when (scanFeedback) {
                    is CoachScanFeedback.Success -> JudoSuccess.copy(alpha = 0.6f)
                    is CoachScanFeedback.AlreadyChecked -> Color(0xFF1976D2).copy(alpha = 0.5f)
                    is CoachScanFeedback.Invalid -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    null -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("coach_center_scanner_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Radar Animation
                ConcentricRipplesRadar(
                    isScanning = scanFeedback == null,
                    accentColor = when (scanFeedback) {
                        is CoachScanFeedback.Success -> JudoSuccess
                        is CoachScanFeedback.AlreadyChecked -> Color(0xFF1976D2)
                        is CoachScanFeedback.Invalid -> MaterialTheme.colorScheme.error
                        null -> JudoRed
                    }
                ) {
                    when (scanFeedback) {
                        is CoachScanFeedback.Success -> {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(JudoSuccess),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Validé",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        is CoachScanFeedback.AlreadyChecked -> {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1976D2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Déjà présent",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        is CoachScanFeedback.Invalid -> {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Erreur",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        null -> {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(JudoRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "QR Code",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }

                // Text status / Athlete feedback
                AnimatedContent(
                    targetState = scanFeedback,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                    },
                    label = "scan_feedback_content"
                ) { feedback ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when (feedback) {
                            is CoachScanFeedback.Success -> {
                                Text(
                                    text = feedback.member.fullName,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 20.sp
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
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            is CoachScanFeedback.AlreadyChecked -> {
                                Text(
                                    text = feedback.member.fullName,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp
                                    ),
                                    color = Color(0xFF1976D2),
                                    textAlign = TextAlign.Center
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1976D2).copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "Athlète déjà marqué présent (à ${feedback.timeString})",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1976D2)
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            is CoachScanFeedback.Invalid -> {
                                Text(
                                    text = "QR code non reconnu",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = feedback.message.ifBlank { "Ce code ne correspond à aucun athlète enregistré" },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                            null -> {
                                Text(
                                    text = "Prêt à scanner",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Présentez le QR code de l’athlète devant la douchette",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
            }
        }

        // Actions Row: "Scanner avec caméra" & "Pointage manuel"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onOpenScanner,
                colors = ButtonDefaults.buttonColors(
                    containerColor = JudoRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .height(52.dp)
                    .testTag("coach_scan_judoka_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Scanner les présences",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            OutlinedButton(
                onClick = { isManualCheckInDialogOpen = true },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("coach_manual_checkin_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = JudoRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Manuel",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        // Recent Scans / Check-ins
        Text(
            text = "Derniers pointages (${sessionAttendances.size})",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (sessionAttendances.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "En attente des premiers judokas sur le tatami...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            } else {
                items(sessionAttendances, key = { it.attendanceId }) { attendance ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                JudokaAvatar(
                                    name = attendance.fullName,
                                    belt = attendance.belt,
                                    photoUrl = attendance.photoUrl,
                                    sizeDp = 38
                                )
                                Column {
                                    Text(
                                        text = attendance.fullName,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = "Pointé à ${attendance.timeString}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = JudoSuccess.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Présent ✓",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = JudoSuccess,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Finish Session Button
        OutlinedButton(
            onClick = onFinishSession,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("coach_finish_session_btn")
        ) {
            Icon(
                imageVector = Icons.Default.StopCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Terminer la séance",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }

    // Modal Dialog: Pointage Manuel (Judoka absent du scan ou carte oubliée)
    if (isManualCheckInDialogOpen) {
        var manualSearchQuery by remember { mutableStateOf("") }
        val absentMembers = remember(categoryMembers, presentMemberIds, manualSearchQuery) {
            categoryMembers
                .filter { it.id !in presentMemberIds }
                .filter {
                    manualSearchQuery.isBlank() || it.fullName.contains(manualSearchQuery, ignoreCase = true)
                }
        }

        AlertDialog(
            onDismissRequest = { isManualCheckInDialogOpen = false },
            title = {
                Text(
                    text = "Pointage manuel",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = manualSearchQuery,
                        onValueChange = { manualSearchQuery = it },
                        placeholder = { Text("Rechercher judoka...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Judokas non encore pointés (${absentMembers.size}) :",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (absentMembers.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Tous les judokas sont déjà pointés !",
                                        style = MaterialTheme.typography.bodySmall.copy(color = JudoSuccess)
                                    )
                                }
                            }
                        } else {
                            items(absentMembers, key = { it.id }) { member ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onManualCheckInMember(member)
                                            isManualCheckInDialogOpen = false
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        JudokaAvatar(name = member.fullName, belt = member.belt, sizeDp = 30)
                                        Column {
                                            Text(member.fullName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Ceinture ${member.beltName.lowercase()}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Text("Pointer +", fontWeight = FontWeight.Bold, color = JudoRed, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isManualCheckInDialogOpen = false }) {
                    Text("Annuler", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
}

/**
 * Screen 4 — Fin de séance (Récapitulatif officiel)
 * Displays:
 * - Total presents and total absents
 * - Start time and end time
 * - Present coaches
 * - Fast list of presents and absents (with tabs)
 * - "Valider et enregistrer" button
 */
@Composable
fun CoachSessionSummaryScreen(
    session: ActiveTrainingSession,
    categoryMembers: List<Member>,
    sessionAttendances: List<AttendanceWithMember>,
    onValidateAndSave: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = categoryMembers.size
    val presentCount = sessionAttendances.size
    val absentCount = (totalCount - presentCount).coerceAtLeast(0)
    val attendanceRate = if (totalCount > 0) ((presentCount.toDouble() / totalCount.toDouble()) * 100).toInt() else 0

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var endTimeString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm", Locale.FRENCH)
        endTimeString = sdf.format(Date())
    }

    val presentMemberIds = remember(sessionAttendances) {
        sessionAttendances.map { it.memberId }.toSet()
    }

    val presentMembers = remember(sessionAttendances) {
        sessionAttendances.map { it.fullName }
    }

    val absentMembers = remember(categoryMembers, presentMemberIds) {
        categoryMembers.filter { it.id !in presentMemberIds }.map { it.fullName }
    }

    val presentCoaches = remember(session) {
        val list = mutableListOf<String>()
        if (session.coach1Present) list.add(session.coach1Name)
        if (session.coach2Present) list.add(session.coach2Name)
        if (list.isEmpty()) list.add("Aucun coach consigné")
        list.joinToString(" • ")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
            .testTag("coach_session_summary_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(JudoSuccess.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = JudoSuccess,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Séance terminée",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black
                    )
                )

                Text(
                    text = "Récapitulatif officiel • ${session.categoryName}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Summary Stats Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Coachs présents",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = presentCoaches,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Horaire",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = "${session.startTime} - $endTimeString",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        )

                        // 3 Stats Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatPill(
                                title = "Présents",
                                value = "$presentCount",
                                color = JudoSuccess,
                                modifier = Modifier.weight(1f)
                            )
                            StatPill(
                                title = "Absents",
                                value = "$absentCount",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            StatPill(
                                title = "Taux",
                                value = "$attendanceRate%",
                                color = JudoRed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs for Presents and Absents list
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    contentColor = JudoRed,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Présents ($presentCount)", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Absents ($absentCount)", fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable List of presents / absents
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val currentList = if (selectedTabIndex == 0) presentMembers else absentMembers
                    if (currentList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (selectedTabIndex == 0) "Aucun judoka présent" else "Aucun judoka absent !",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    } else {
                        items(currentList) { name ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(
                                        text = if (selectedTabIndex == 0) "Présent ✓" else "Absent",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (selectedTabIndex == 0) JudoSuccess else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom validate and save button
            Button(
                onClick = { onValidateAndSave(endTimeString) },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JudoRed,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("coach_validate_summary_btn")
            ) {
                Text(
                    text = "Valider et enregistrer",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                )
            }
        }
    }
}

@Composable
fun ConcentricRipplesRadar(
    isScanning: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = JudoRed,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_concentric_transition")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_progress"
    )

    Box(
        modifier = modifier.size(175.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val minDim = size.minDimension
            val baseRadius = 38.dp.toPx()
            val maxRadius = (minDim / 2f) - 6.dp.toPx()
            val spread = maxRadius - baseRadius

            if (isScanning) {
                // 3 concentric expanding rings with subtle glow
                for (i in 0 until 3) {
                    val ringPhase = (progress + (i / 3f)) % 1f
                    val currentRadius = baseRadius + spread * ringPhase
                    val ringAlpha = ((1f - ringPhase) * 0.75f).coerceIn(0f, 1f)

                    // Outer soft glow
                    drawCircle(
                        color = accentColor.copy(alpha = ringAlpha * 0.25f),
                        radius = currentRadius,
                        center = center,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    // Sharp fine ring
                    drawCircle(
                        color = accentColor.copy(alpha = ringAlpha),
                        radius = currentRadius,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            // Central circle backdrop & border
            drawCircle(
                color = accentColor.copy(alpha = 0.12f),
                radius = baseRadius + 3.dp.toPx(),
                center = center
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.22f),
                radius = baseRadius,
                center = center
            )
            drawCircle(
                color = accentColor,
                radius = baseRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Inner emblem/icon
        content()
    }
}

@Composable
private fun StatPill(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = color
                )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
