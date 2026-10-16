package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ActiveTrainingSession
import com.example.data.model.JudoCategory
import com.example.data.model.PastTrainingSession
import com.example.ui.theme.JudoGold
import com.example.ui.theme.JudoRed
import com.example.ui.theme.JudoSuccess

import com.example.data.model.CoachUser
import com.example.data.model.RecurrenceScope
import com.example.data.model.ScheduledTrainingSession
import com.example.data.model.SessionConflict

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSessionsScreen(
    scheduledSessions: List<ScheduledTrainingSession> = emptyList(),
    pastSessions: List<PastTrainingSession>,
    activeSession: ActiveTrainingSession?,
    categories: List<JudoCategory>,
    coaches: List<CoachUser> = emptyList(),
    onAddScheduledSession: (ScheduledTrainingSession, Int) -> Unit = { _, _ -> },
    onUpdateScheduledSession: (ScheduledTrainingSession) -> Unit = {},
    onDuplicateScheduledSession: (ScheduledTrainingSession) -> Unit = {},
    onDeleteScheduledSession: (String, RecurrenceScope) -> Unit = { _, _ -> },
    onCheckConflicts: (ScheduledTrainingSession, String?) -> List<SessionConflict> = { _, _ -> emptyList() },
    onCorrectAttendance: (String, String, Boolean) -> Unit,
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    var currentMainTab by remember { mutableStateOf(initialTab) }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var selectedSessionDetail by remember { mutableStateOf<PastTrainingSession?>(null) }
    var sessionToCorrect by remember { mutableStateOf<PastTrainingSession?>(null) }

    var isCreateEditDialogOpen by remember { mutableStateOf(false) }
    var sessionToEdit by remember { mutableStateOf<ScheduledTrainingSession?>(null) }
    var preselectedDay by remember { mutableStateOf<String?>(null) }
    var preselectedTime by remember { mutableStateOf<String?>(null) }

    val filteredSessions = remember(pastSessions, selectedCategoryFilter) {
        if (selectedCategoryFilter == "ALL") {
            pastSessions
        } else {
            pastSessions.filter { it.categoryId.equals(selectedCategoryFilter, ignoreCase = true) || it.categoryName.equals(selectedCategoryFilter, ignoreCase = true) }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isTablet) 12.dp else 4.dp, vertical = 6.dp)
                .testTag("admin_sessions_screen"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Mode Tabs: Planning Hebdo vs Historique & Émargements
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = CardDefaults.outlinedCardBorder()
            ) {
                TabRow(
                    selectedTabIndex = currentMainTab,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    Tab(
                        selected = currentMainTab == 0,
                        onClick = { currentMainTab = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.EventNote, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Planning Hebdo (${scheduledSessions.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    )
                    Tab(
                        selected = currentMainTab == 1,
                        onClick = { currentMainTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Historique & Émargements (${pastSessions.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    )
                }
            }

            if (currentMainTab == 0) {
                // TAB 0: Planning Hebdomadaire
                AdminWeeklyScheduleView(
                    sessions = scheduledSessions,
                    categories = categories,
                    coaches = coaches,
                    onAddNewSession = { day, time ->
                        sessionToEdit = null
                        preselectedDay = day
                        preselectedTime = time ?: "17:30"
                        isCreateEditDialogOpen = true
                    },
                    onEditSession = { session ->
                        sessionToEdit = session
                        preselectedDay = session.dayOfWeekName
                        preselectedTime = session.startTime
                        isCreateEditDialogOpen = true
                    },
                    onDuplicateSession = { session ->
                        onDuplicateScheduledSession(session)
                    },
                    onDeleteSession = { id, scope ->
                        onDeleteScheduledSession(id, scope)
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                // TAB 1: Historique & Émargements
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

        // Active Session Alert if any is running on tatami
        if (activeSession != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = JudoRed.copy(alpha = 0.12f),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(JudoRed)
                    )
                    Column {
                        Text(
                            text = "Séance en direct sur le tatami : ${activeSession.categoryName}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = JudoRed)
                        )
                        Text(
                            text = "Démarrée à ${activeSession.startTime} par ${activeSession.coach1Name}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        }

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedCategoryFilter == "ALL",
                    onClick = { selectedCategoryFilter = "ALL" },
                    label = { Text("Toutes (${pastSessions.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = JudoRed,
                        selectedLabelColor = Color.White
                    )
                )
            }
            items(categories) { cat ->
                val count = pastSessions.count { it.categoryId.equals(cat.id, ignoreCase = true) || it.categoryName.equals(cat.name, ignoreCase = true) }
                FilterChip(
                    selected = selectedCategoryFilter == cat.id,
                    onClick = { selectedCategoryFilter = cat.id },
                    label = { Text("${cat.name} ($count)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = JudoRed,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // List of Sessions
        if (filteredSessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucune séance trouvée pour ce filtre.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredSessions) { session ->
                    val total = session.presentCount + session.absentCount
                    val rate = if (total > 0) ((session.presentCount.toDouble() / total) * 100).toInt() else 0

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSessionDetail = session }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(JudoRed.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EventNote,
                                            contentDescription = null,
                                            tint = JudoRed,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "Séance ${session.categoryName}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = session.dateString,
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
                                        text = "$rate% assiduité",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = JudoSuccess
                                        )
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${session.startTime} - ${session.endTime}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.SportsMartialArts, contentDescription = null, modifier = Modifier.size(14.dp), tint = JudoGold)
                                    Text(
                                        text = "Coach : ${session.coach1Name}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = JudoGold,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "🟢 ${session.presentCount} Présents",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "🔴 ${session.absentCount} Absents",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    TextButton(
                                        onClick = { sessionToCorrect = session },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Corriger", fontSize = 12.sp)
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = "Détails",
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Dialog
    if (selectedSessionDetail != null) {
        val session = selectedSessionDetail!!
        var detailTab by remember { mutableStateOf(0) } // 0: Présents, 1: Absents

        Dialog(onDismissRequest = { selectedSessionDetail = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Séance ${session.categoryName}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${session.dateString} • ${session.startTime} - ${session.endTime}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        IconButton(onClick = { selectedSessionDetail = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                        }
                    }

                    // Stat cards row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = JudoSuccess.copy(alpha = 0.15f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${session.presentCount}", fontWeight = FontWeight.Bold, color = JudoSuccess, fontSize = 18.sp)
                                Text(text = "Présents", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${session.absentCount}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 18.sp)
                                Text(text = "Absents", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = JudoGold.copy(alpha = 0.15f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${session.presentCount + session.absentCount}", fontWeight = FontWeight.Bold, color = JudoGold, fontSize = 18.sp)
                                Text(text = "Total inscrits", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Coachs
                    Text(
                        text = "Coachs présents : ${session.coach1Name}${if (session.coach2Present) " • ${session.coach2Name}" else ""}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = JudoGold)
                    )

                    // Tabs Présents vs Absents
                    TabRow(selectedTabIndex = detailTab) {
                        Tab(
                            selected = detailTab == 0,
                            onClick = { detailTab = 0 },
                            text = { Text("Présents (${session.presentMemberNames.size})") }
                        )
                        Tab(
                            selected = detailTab == 1,
                            onClick = { detailTab = 1 },
                            text = { Text("Absents (${session.absentMemberNames.size})") }
                        )
                    }

                    // List of names
                    val listToShow = if (detailTab == 0) session.presentMemberNames else session.absentMemberNames
                    if (listToShow.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                            Text(text = "Aucun membre dans cette liste.", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(listToShow) { name ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                        Icon(
                                            imageVector = if (detailTab == 0) Icons.Default.CheckCircle else Icons.Default.Close,
                                            contentDescription = null,
                                            tint = if (detailTab == 0) JudoSuccess else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { selectedSessionDetail = null },
                            colors = ButtonDefaults.buttonColors(containerColor = JudoRed)
                        ) {
                            Text("Fermer")
                        }
                    }
                }
            }
        }
    }

    // Exceptional Correction Dialog
    if (sessionToCorrect != null) {
        val session = sessionToCorrect!!
        var memberNameInput by remember { mutableStateOf("") }
        var markAsPresent by remember { mutableStateOf(true) }
        var showConfirmAlert by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { sessionToCorrect = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Correction administrative",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Corriger manuellement la présence d'un athlète pour la séance ${session.categoryName} du ${session.dateString}. Cette action est tracée dans le journal d'audit.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    OutlinedTextField(
                        value = memberNameInput,
                        onValueChange = { memberNameInput = it },
                        label = { Text("Nom et prénom de l'athlète") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = markAsPresent,
                            onClick = { markAsPresent = true },
                            label = { Text("Marquer Présent") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = JudoSuccess,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = !markAsPresent,
                            onClick = { markAsPresent = false },
                            label = { Text("Marquer Absent") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.error,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { sessionToCorrect = null }) {
                            Text("Annuler")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (memberNameInput.isNotBlank()) {
                                    showConfirmAlert = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = JudoRed)
                        ) {
                            Text("Appliquer")
                        }
                    }
                }
            }
        }

        if (showConfirmAlert) {
            AlertDialog(
                onDismissRequest = { showConfirmAlert = false },
                title = { Text("Confirmer la modification") },
                text = {
                    Text("Confirmez-vous le passage de $memberNameInput au statut ${if (markAsPresent) "PRÉSENT" else "ABSENT"} pour cette séance ?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onCorrectAttendance(session.id, memberNameInput.trim(), markAsPresent)
                            showConfirmAlert = false
                            sessionToCorrect = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JudoRed)
                    ) {
                        Text("Confirmer la correction")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmAlert = false }) {
                        Text("Annuler")
                    }
                }
            )
        }
    }
    }
}
}

    if (isCreateEditDialogOpen) {
        CreateEditSessionDialog(
            initialSession = sessionToEdit,
            categories = categories,
            coaches = coaches,
            preselectedDayName = preselectedDay,
            preselectedStartTime = preselectedTime,
            onCheckConflicts = onCheckConflicts,
            onSaveSession = { session, repeatWeeks ->
                if (sessionToEdit == null) {
                    onAddScheduledSession(session, repeatWeeks)
                } else {
                    onUpdateScheduledSession(session)
                }
                isCreateEditDialogOpen = false
                sessionToEdit = null
            },
            onDismiss = {
                isCreateEditDialogOpen = false
                sessionToEdit = null
            }
        )
    }
}

