package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SportsMartialArts
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AttendanceWithMember
import com.example.data.model.Belt
import com.example.data.model.CoachProfile
import com.example.data.model.Member
import com.example.ui.components.BeltBadge
import com.example.ui.components.GroupBadge
import com.example.ui.components.JudokaAvatar
import com.example.ui.components.StatCard
import com.example.ui.theme.JudoGold
import com.example.ui.theme.JudoRed
import com.example.ui.theme.JudoSuccess
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    allMembers: List<Member>,
    todayAttendance: List<AttendanceWithMember>,
    lastScanned: AttendanceWithMember?,
    coachProfile: CoachProfile = CoachProfile(),
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    onEditCoachProfile: () -> Unit = {},
    onSwitchRole: () -> Unit = {},
    onNavigateToScanner: () -> Unit,
    onNavigateToMemberDetail: (Long) -> Unit,
    onQuickCheckIn: (String) -> Unit,
    onStatClick: (StatFilterMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.FRENCH)
        val dateFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH)
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now).replaceFirstChar { it.uppercase() }
            delay(1000)
        }
    }

    val totalMembers = allMembers.size
    val presentCount = todayAttendance.size
    val absentCount = (totalMembers - presentCount).coerceAtLeast(0)
    val attendanceRate = if (totalMembers > 0) {
        ((presentCount.toDouble() / totalMembers.toDouble()) * 100).toInt()
    } else 0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header with Dojo branding
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth().testTag("home_header_card")
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Subtle background gradient with red martial accent
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        JudoRed.copy(alpha = if (isDarkTheme) 0.25f else 0.12f),
                                        if (isDarkTheme) Color(0xFF1A1B20) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_judo_launcher),
                                    contentDescription = "Emblème Judo Seddouk",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, JudoGold, CircleShape),
                                    contentScale = ContentScale.Crop
                                )

                                Column {
                                    Text(
                                        text = "JUDO SEDDOUK",
                                        color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.2.sp
                                        )
                                    )
                                    Text(
                                        text = "Pointage officiel du dojo",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Switch to Coach Categories Space
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = JudoRed.copy(alpha = 0.15f),
                                    border = CardDefaults.outlinedCardBorder(),
                                    modifier = Modifier.clickable { onSwitchRole() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "🥋 Catégories",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = JudoRed
                                            )
                                        )
                                    }
                                }

                                // Theme Toggle
                                IconButton(
                                    onClick = onToggleTheme,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                                        .testTag("toggle_theme_button")
                                ) {
                                    Icon(
                                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = if (isDarkTheme) "Passer au thème clair" else "Passer au thème sombre",
                                        tint = if (isDarkTheme) Color(0xFFFFD54F) else JudoRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Live Clock Badge
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Text(
                                        text = "🕐 $currentTime",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = JudoRed
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = currentDate,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        // Sensei / Coach Profile Card with Photo
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditCoachProfile() }
                    .testTag("coach_banner_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF212121))
                                .border(2.5.dp, JudoGold, CircleShape)
                        ) {
                            if (coachProfile.photoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = coachProfile.photoUrl,
                                    contentDescription = "Photo du Coach",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.SportsMartialArts,
                                    contentDescription = null,
                                    tint = JudoGold,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = coachProfile.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = JudoGold.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "🥋 COACH",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = JudoGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                            Text(
                                text = "${coachProfile.danGrade} • ${coachProfile.title}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onEditCoachProfile,
                        modifier = Modifier.testTag("edit_coach_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifier profil coach",
                            tint = JudoRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Prominent 1-Handed QR Scanner Button
        item {
            Button(
                onClick = onNavigateToScanner,
                colors = ButtonDefaults.buttonColors(
                    containerColor = JudoRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("scan_qr_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scanner",
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "SCANNER UN QR CODE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }

        // Today's Stats Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Aujourd'hui",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Présents",
                        value = "$presentCount",
                        subtitle = "sur $totalMembers",
                        accentColor = JudoSuccess,
                        modifier = Modifier.weight(1f),
                        onClick = { onStatClick(StatFilterMode.PRESENTS) },
                        testTag = "stat_present_card"
                    )

                    StatCard(
                        title = "Absents",
                        value = "$absentCount",
                        subtitle = "à ce jour",
                        accentColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                        onClick = { onStatClick(StatFilterMode.ABSENTS) },
                        testTag = "stat_absent_card"
                    )

                    StatCard(
                        title = "Taux",
                        value = "$attendanceRate%",
                        subtitle = "régularité",
                        accentColor = JudoRed,
                        modifier = Modifier.weight(1f),
                        onClick = { onStatClick(StatFilterMode.ATTENDANCE_RATE) },
                        testTag = "stat_rate_card"
                    )
                }
            }
        }

        // Last Scanned Member Card
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Dernière personne scannée",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                val displayScanned = lastScanned ?: todayAttendance.firstOrNull()

                if (displayScanned != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToMemberDetail(displayScanned.memberId) }
                            .testTag("last_scanned_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            JudokaAvatar(
                                name = displayScanned.fullName,
                                belt = displayScanned.belt,
                                photoUrl = displayScanned.photoUrl,
                                sizeDp = 52
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayScanned.fullName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    GroupBadge(group = displayScanned.group)
                                    BeltBadge(belt = displayScanned.belt)
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = JudoSuccess.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Présence ✓",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = JudoSuccess,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                Text(
                                    text = displayScanned.timeString,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aucun pointage pour le moment. Cliquez sur « Scanner un QR Code » pour débuter.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }

        // Quick test simulation bar for instant verification without physical cards
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth().testTag("quick_test_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FlashOn,
                                contentDescription = null,
                                tint = JudoRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Simulation rapide de cartes (1 tap)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "Test instantané",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allMembers.take(8)) { member ->
                            val isAlreadyChecked = todayAttendance.any { it.memberId == member.id }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isAlreadyChecked) MaterialTheme.colorScheme.surfaceVariant else JudoRed.copy(alpha = 0.15f),
                                border = if (isAlreadyChecked) null else CardDefaults.outlinedCardBorder(),
                                modifier = Modifier
                                    .clickable { onQuickCheckIn(member.qrCode) }
                                    .testTag("quick_checkin_${member.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = member.firstName,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAlreadyChecked) MaterialTheme.colorScheme.onSurfaceVariant else JudoRed
                                        )
                                    )
                                    if (isAlreadyChecked) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = JudoSuccess,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Today's recent check-in list
        item {
            Text(
                text = "Pointages récents aujourd'hui",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (todayAttendance.isEmpty()) {
            item {
                Text(
                    text = "Aucun judoka pointé aujourd'hui.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        } else {
            items(todayAttendance.take(6)) { item ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToMemberDetail(item.memberId) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = item.timeString,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = JudoRed
                            )
                        )

                        JudokaAvatar(
                            name = item.fullName,
                            belt = item.belt,
                            photoUrl = item.photoUrl,
                            sizeDp = 40
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.fullName,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                GroupBadge(group = item.group)
                                BeltBadge(belt = item.belt)
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Présent",
                            tint = JudoSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
