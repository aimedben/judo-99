package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceWithMember
import com.example.data.model.JudoCategory
import com.example.data.model.Member
import com.example.data.model.PastTrainingSession
import com.example.ui.components.JudokaAvatar
import com.example.ui.theme.JudoGold
import com.example.ui.theme.JudoNavy
import com.example.ui.theme.JudoRed
import com.example.ui.theme.JudoSuccess

/**
 * Screen: Athlètes de la catégorie du coach
 * Affiche uniquement les judokas de la catégorie sélectionnée,
 * avec fiche simplifiée, statut de présence aujourd'hui et état de cotisation.
 */
@Composable
fun CoachAthletesScreen(
    category: JudoCategory,
    members: List<Member>,
    todayAttendances: List<AttendanceWithMember>,
    onManualCheckIn: (Member) -> Unit,
    onAddAthlete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMemberForDetail by remember { mutableStateOf<Member?>(null) }

    val checkedInIds = remember(todayAttendances) {
        todayAttendances.map { it.memberId }.toSet()
    }

    val filteredMembers = remember(members, searchQuery) {
        if (searchQuery.isBlank()) members
        else members.filter {
            it.fullName.contains(searchQuery, ignoreCase = true) ||
            it.beltName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("coach_athletes_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Athlètes • ${category.name}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black
                    )
                )
                Text(
                    text = "${filteredMembers.size} judokas inscrits",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = JudoRed.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "${todayAttendances.size} présents auj.",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = JudoRed,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Rechercher un judoka...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = JudoRed
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("coach_athletes_search_field")
        )

        // Athletes List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (filteredMembers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucun judoka trouvé",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            } else {
                items(filteredMembers, key = { it.id }) { member ->
                    val isPresentToday = checkedInIds.contains(member.id)
                    val isCotisationOk = member.isCotisationUpToDate

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMemberForDetail = member }
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                JudokaAvatar(
                                    name = member.fullName,
                                    belt = member.belt,
                                    photoUrl = member.photoUrl,
                                    sizeDp = 44
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = member.fullName,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = "Ceinture ${member.beltName.lowercase().replaceFirstChar { it.uppercase() }}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )

                                        // Badge Cotisation
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isCotisationOk) JudoSuccess.copy(alpha = 0.12f) else Color(0xFFFF9800).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = if (isCotisationOk) "À jour" else "En retard",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isCotisationOk) JudoSuccess else Color(0xFFF57C00),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Presence badge or quick check-in
                            if (isPresentToday) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = JudoSuccess.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = JudoSuccess,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Présent",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = JudoSuccess,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onManualCheckIn(member) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = "Pointer",
                                        style = MaterialTheme.typography.labelMedium.copy(
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

    // Detail dialog for clicked member
    selectedMemberForDetail?.let { member ->
        val isCotisationOk = member.isCotisationUpToDate
        AlertDialog(
            onDismissRequest = { selectedMemberForDetail = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    JudokaAvatar(
                        name = member.fullName,
                        belt = member.belt,
                        photoUrl = member.photoUrl,
                        sizeDp = 40
                    )
                    Column {
                        Text(
                            text = member.fullName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                        )
                        Text(
                            text = "Catégorie ${category.name}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Grade / Ceinture", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(member.beltName, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cotisation", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (isCotisationOk) "À jour ✓" else "En retard ⚠️",
                            fontWeight = FontWeight.Bold,
                            color = if (isCotisationOk) JudoSuccess else Color(0xFFF57C00)
                        )
                    }
                    if (member.birthDate.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Date de naissance", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(member.birthDate, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (member.phone.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Téléphone", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(member.phone, fontWeight = FontWeight.Medium)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Code QR", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(member.qrCode, fontWeight = FontWeight.Bold, color = JudoRed)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMemberForDetail = null }) {
                    Text("Fermer", color = JudoRed)
                }
            }
        )
    }
}

/**
 * Screen: Historique des séances de la catégorie
 * Liste des entraînements passés avec date, nombre de présents/absents et détails au clic.
 */
@Composable
fun CoachHistoryScreen(
    category: JudoCategory,
    pastSessions: List<PastTrainingSession>,
    modifier: Modifier = Modifier
) {
    var selectedSessionForDetail by remember { mutableStateOf<PastTrainingSession?>(null) }

    val categorySessions = remember(pastSessions, category) {
        pastSessions.filter { it.categoryId.equals(category.id, ignoreCase = true) || it.groupCode.equals(category.groupCode, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("coach_history_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Column {
            Text(
                text = "Historique • ${category.name}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black
                )
            )
            Text(
                text = "${categorySessions.size} séances enregistrées",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (categorySessions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucune séance passée enregistrée pour cette catégorie",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            } else {
                items(categorySessions, key = { it.id }) { session ->
                    val total = session.presentCount + session.absentCount
                    val rate = if (total > 0) ((session.presentCount.toDouble() / total.toDouble()) * 100).toInt() else 0

                    val coachesPresent = buildList {
                        if (session.coach1Present) add(session.coach1Name)
                        if (session.coach2Present) add(session.coach2Name)
                        if (isEmpty()) add("Non renseigné")
                    }.joinToString(" • ")

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSessionForDetail = session }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Date and Time Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = JudoRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = session.dateString,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = "${session.startTime} - ${session.endTime}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Coachs : $coachesPresent",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            // Quick Stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = JudoSuccess.copy(alpha = 0.12f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "${session.presentCount}",
                                            fontWeight = FontWeight.Black,
                                            color = JudoSuccess,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "Présents",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "${session.absentCount}",
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "Absents",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = JudoRed.copy(alpha = 0.12f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "$rate%",
                                            fontWeight = FontWeight.Black,
                                            color = JudoRed,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "Taux",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

    // Modal with full details of presents & absents
    selectedSessionForDetail?.let { session ->
        AlertDialog(
            onDismissRequest = { selectedSessionForDetail = null },
            title = {
                Text(
                    text = "Détails de la séance",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${session.dateString} (${session.startTime} - ${session.endTime})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )

                    if (session.presentMemberNames.isNotEmpty()) {
                        Text(
                            text = "Présents (${session.presentMemberNames.size}) :",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = JudoSuccess
                            )
                        )
                        Text(
                            text = session.presentMemberNames.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    if (session.absentMemberNames.isNotEmpty()) {
                        Text(
                            text = "Absents (${session.absentMemberNames.size}) :",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                        Text(
                            text = session.absentMemberNames.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSessionForDetail = null }) {
                    Text("Fermer", color = JudoRed)
                }
            }
        )
    }
}

/**
 * Screen: Profil Coach
 * Fiche du coach connecté, catégorie assignée, statut et bouton de déconnexion / changement de catégorie.
 */
@Composable
fun CoachProfileScreen(
    category: JudoCategory,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onChangeCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
            .testTag("coach_profile_screen"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Profil Coach",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black
                )
            )

            // Coach ID Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(JudoRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🥋",
                            fontSize = 34.sp
                        )
                    }

                    Text(
                        text = category.coach1Name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = JudoRed.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Coach Principal • ${category.name}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = JudoRed,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Text(
                        text = "Co-entraîneur : ${category.coach2Name}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Quick Actions Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    // Theme toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onToggleTheme() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = JudoRed
                            )
                            Text(
                                text = if (isDarkTheme) "Mode Clair" else "Mode Sombre",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                        Text(
                            text = if (isDarkTheme) "Sombre" else "Clair",
                            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    // Change Category
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onChangeCategory() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = JudoRed
                            )
                            Text(
                                text = "Changer de catégorie",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.labelMedium.copy(color = JudoRed, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Logout / Quit Coach session
        OutlinedButton(
            onClick = onChangeCategory,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Quitter l'espace coach",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            )
        }
    }
}
