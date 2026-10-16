package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Attendance
import com.example.data.model.AttendanceWithMember
import com.example.data.model.Member
import com.example.ui.components.BeltBadge
import com.example.ui.components.GroupBadge
import com.example.ui.components.JudokaAvatar
import com.example.ui.components.MemberAttendanceCalendar
import com.example.ui.theme.JudoRed
import com.example.ui.theme.JudoSuccess
import com.example.util.QrCodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailScreen(
    member: Member,
    attendanceList: List<Attendance>,
    allClubAttendances: List<AttendanceWithMember> = emptyList(),
    onBack: () -> Unit,
    onManualCheckIn: (Long) -> Unit,
    onRecordAttendanceForDate: (Long, String) -> Unit = { _, _ -> },
    onDeleteAttendanceForDate: (Long, String) -> Unit = { _, _ -> },
    onEditMember: (Member) -> Unit,
    onDeleteMember: (Member) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showQrEnlarged by remember { mutableStateOf(false) }

    val qrBitmap = remember(member.qrCode) {
        QrCodeGenerator.generateQrImageBitmap(member.qrCode, 512)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("member_detail_screen")
    ) {
        val isTabletLandscape = maxWidth >= 720.dp

        if (isTabletLandscape) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MemberTopBar(
                    onBack = onBack,
                    onEdit = { onEditMember(member) },
                    onDelete = { showDeleteConfirm = true }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Left Column: Identity, info, and official QR code
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            MemberProfileCard(
                                member = member,
                                onManualCheckIn = onManualCheckIn
                            )
                        }
                        item {
                            MemberInfoCard(
                                member = member,
                                onCallParent = { phone ->
                                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${phone.replace(" ", "")}")
                                    }
                                    context.startActivity(dialIntent)
                                }
                            )
                        }
                        item {
                            MemberQrCard(
                                member = member,
                                qrBitmap = qrBitmap
                            )
                        }
                    }

                    // Right Column: Attendance Agenda & Session History
                    LazyColumn(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            MemberAttendanceCalendar(
                                member = member,
                                attendanceList = attendanceList,
                                allClubAttendances = allClubAttendances,
                                onRecordAttendanceForDate = onRecordAttendanceForDate,
                                onDeleteAttendanceForDate = onDeleteAttendanceForDate
                            )
                        }
                        item {
                            Text(
                                text = "Historique des séances",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        if (attendanceList.isEmpty()) {
                            item {
                                Text(
                                    text = "Aucune présence enregistrée à ce jour pour ce judoka.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        } else {
                            items(attendanceList) { att ->
                                AttendanceHistoryCard(att = att)
                            }
                        }
                    }
                }
            }
        } else {
            // Phone single-column layout
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    MemberTopBar(
                        onBack = onBack,
                        onEdit = { onEditMember(member) },
                        onDelete = { showDeleteConfirm = true }
                    )
                }
                item {
                    MemberProfileCard(
                        member = member,
                        onManualCheckIn = onManualCheckIn
                    )
                }
                item {
                    MemberInfoCard(
                        member = member,
                        onCallParent = { phone ->
                            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${phone.replace(" ", "")}")
                            }
                            context.startActivity(dialIntent)
                        }
                    )
                }
                item {
                    MemberQrCard(
                        member = member,
                        qrBitmap = qrBitmap
                    )
                }
                item {
                    MemberAttendanceCalendar(
                        member = member,
                        attendanceList = attendanceList,
                        allClubAttendances = allClubAttendances,
                        onRecordAttendanceForDate = onRecordAttendanceForDate,
                        onDeleteAttendanceForDate = onDeleteAttendanceForDate
                    )
                }
                item {
                    Text(
                        text = "Historique des séances",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                if (attendanceList.isEmpty()) {
                    item {
                        Text(
                            text = "Aucune présence enregistrée à ce jour pour ce judoka.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                } else {
                    items(attendanceList) { att ->
                        AttendanceHistoryCard(att = att)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Supprimer le judoka") },
            text = { Text("Êtes-vous sûr de vouloir supprimer ${member.fullName} ? Cette action est irréversible.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteMember(member)
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun MemberTopBar(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("member_detail_back_button")
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
        }

        Text(
            text = "Fiche Judoka",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        Row {
            IconButton(onClick = onEdit) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Modifier")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Supprimer",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun MemberProfileCard(
    member: Member,
    onManualCheckIn: (Long) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            JudokaAvatar(
                name = member.fullName,
                belt = member.belt,
                photoUrl = member.photoUrl,
                sizeDp = 80
            )

            Text(
                text = member.fullName,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GroupBadge(group = member.group)
                BeltBadge(belt = member.belt)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = JudoSuccess.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = member.status,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = JudoSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action: Check-in button
            Button(
                onClick = { onManualCheckIn(member.id) },
                colors = ButtonDefaults.buttonColors(containerColor = JudoRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("manual_checkin_button")
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enregistrer présence manuellement", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MemberInfoCard(
    member: Member,
    onCallParent: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Informations générales",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            DetailRow(title = "Date de naissance", value = member.birthDate)
            DetailRow(title = "Catégorie", value = "${member.group.label} (${member.group.ageRange})")
            DetailRow(title = "Inscription", value = member.registrationDate)

            if (member.notes.isNotBlank()) {
                DetailRow(title = "Notes & observations", value = member.notes)
            }

            // Phone row with call action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Téléphone parent",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = member.phone.ifBlank { "Non renseigné" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }

                if (member.phone.isNotBlank()) {
                    IconButton(
                        onClick = { onCallParent(member.phone) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Appeler parent",
                            tint = JudoSuccess
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberQrCard(
    member: Member,
    qrBitmap: androidx.compose.ui.graphics.ImageBitmap?
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("member_qr_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Carte QR Code officielle",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = member.qrCode,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = JudoRed
                    )
                )
            }

            if (qrBitmap != null) {
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = qrBitmap,
                        contentDescription = "QR Code de ${member.fullName}",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Text("Génération QR impossible", color = MaterialTheme.colorScheme.error)
            }

            Text(
                text = "Scannable depuis un téléphone ou une carte imprimée",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun AttendanceHistoryCard(att: Attendance) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = JudoSuccess,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = att.dateString,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Text(
                text = "${att.timeString} (${att.checkInMethod})",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun DetailRow(title: String, value: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
    }
}
