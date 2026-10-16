package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewWeek
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CoachUser
import com.example.data.model.JudoCategory
import com.example.data.model.RecurrenceScope
import com.example.data.model.ScheduledTrainingSession
import com.example.data.model.SessionStatus
import com.example.ui.theme.JudoGold
import com.example.ui.theme.JudoRed
import com.example.ui.theme.JudoSuccess

enum class ScheduleViewMode {
    WEEK_AGENDA, // Jour par jour / Agenda responsive compact
    LIST_VIEW,    // Liste chronologique
    TABLE_GRID    // Grille 7 colonnes pour tablette / écran large
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminWeeklyScheduleView(
    sessions: List<ScheduledTrainingSession>,
    categories: List<JudoCategory>,
    coaches: List<CoachUser>,
    selectedCategoryIdFilter: String = "ALL",
    onCategoryFilterChange: (String) -> Unit = {},
    onAddNewSession: (dayName: String?, timeSlot: String?) -> Unit,
    onEditSession: (ScheduledTrainingSession) -> Unit,
    onDuplicateSession: (ScheduledTrainingSession) -> Unit,
    onDeleteSession: (String, RecurrenceScope) -> Unit,
    modifier: Modifier = Modifier
) {
    // 7 jours ordonnés Lundi -> Dimanche
    val daysOfWeek = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")
    val dayLetters = listOf("L", "M", "M", "J", "V", "S", "D")
    val daySubtitles = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")

    // Sélection par défaut du jour actuel
    val todayDayIndex = remember {
        val cal = java.util.Calendar.getInstance()
        when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> 0
            java.util.Calendar.TUESDAY -> 1
            java.util.Calendar.WEDNESDAY -> 2
            java.util.Calendar.THURSDAY -> 3
            java.util.Calendar.FRIDAY -> 4
            java.util.Calendar.SATURDAY -> 5
            java.util.Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    var selectedCoachFilter by remember { mutableStateOf("ALL") }
    var selectedDayIndex by remember { mutableStateOf(todayDayIndex) }
    var weekOffset by remember { mutableStateOf(0) }
    var viewMode by remember { mutableStateOf(ScheduleViewMode.WEEK_AGENDA) }

    var sessionForDetailsDialog by remember { mutableStateOf<ScheduledTrainingSession?>(null) }
    var sessionToDelete by remember { mutableStateOf<ScheduledTrainingSession?>(null) }

    // Filtrage des créneaux
    val filteredSessions = remember(sessions, selectedCategoryIdFilter, selectedCoachFilter) {
        sessions.filter { s ->
            val matchCategory = selectedCategoryIdFilter == "ALL" || s.categoryId.equals(selectedCategoryIdFilter, ignoreCase = true)
            val matchCoach = selectedCoachFilter == "ALL" || s.coachNames.any { it.equals(selectedCoachFilter, ignoreCase = true) }
            matchCategory && matchCoach && s.status != SessionStatus.CANCELLED
        }.sortedWith(compareBy({ daysOfWeek.indexOf(it.dayOfWeekName).let { idx -> if (idx >= 0) idx else 99 } }, { it.startTime }))
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp
        val activeViewMode = if (isTablet && viewMode == ScheduleViewMode.WEEK_AGENDA) ScheduleViewMode.TABLE_GRID else viewMode

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isTablet) 12.dp else 4.dp, vertical = 6.dp)
                .testTag("admin_weekly_schedule_view"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Barre supérieure compacte : Titre et sélecteur de mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Planning Hebdomadaire",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                    )
                    Text(
                        text = when (weekOffset) {
                            0 -> "Semaine en cours • Dojo Seddouk"
                            1 -> "Semaine prochaine (+1)"
                            -1 -> "Semaine précédente (-1)"
                            else -> if (weekOffset > 0) "+$weekOffset semaines" else "$weekOffset semaines"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            viewMode = when (viewMode) {
                                ScheduleViewMode.WEEK_AGENDA -> ScheduleViewMode.LIST_VIEW
                                ScheduleViewMode.LIST_VIEW -> if (isTablet) ScheduleViewMode.TABLE_GRID else ScheduleViewMode.WEEK_AGENDA
                                ScheduleViewMode.TABLE_GRID -> ScheduleViewMode.WEEK_AGENDA
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = when (activeViewMode) {
                                ScheduleViewMode.WEEK_AGENDA -> Icons.Default.ViewAgenda
                                ScheduleViewMode.LIST_VIEW -> Icons.Default.ViewWeek
                                ScheduleViewMode.TABLE_GRID -> Icons.Default.Today
                            },
                            contentDescription = "Changer l'affichage",
                            tint = JudoRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 2. Navigation entre semaines (Compacte & fine)
            val weekDateRange = remember(weekOffset) {
                try {
                    val cal = java.util.Calendar.getInstance()
                    cal.add(java.util.Calendar.WEEK_OF_YEAR, weekOffset)
                    cal.firstDayOfWeek = java.util.Calendar.MONDAY
                    cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                    val startDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
                    val startMonth = java.text.SimpleDateFormat("MMM", java.util.Locale.FRENCH).format(cal.time)
                    cal.add(java.util.Calendar.DAY_OF_WEEK, 6)
                    val endDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
                    val endMonth = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.FRENCH).format(cal.time)
                    "$startDay $startMonth - $endDay $endMonth"
                } catch (_: Exception) {
                    "Semaine en cours"
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { weekOffset -= 1 },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Semaine précédente", modifier = Modifier.size(14.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Today, contentDescription = null, tint = JudoRed, modifier = Modifier.size(15.dp))
                        Text(
                            text = weekDateRange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        if (weekOffset != 0) {
                            OutlinedButton(
                                onClick = { weekOffset = 0 },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("Aujourd'hui", fontSize = 10.sp)
                            }
                        }
                    }

                    IconButton(
                        onClick = { weekOffset += 1 },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Semaine suivante", modifier = Modifier.size(14.dp))
                    }
                }
            }

            // 3. Filtres par catégorie et coach (Compact)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryIdFilter == "ALL",
                        onClick = { onCategoryFilterChange("ALL") },
                        label = { Text("Toutes catégories", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = JudoRed,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.height(30.dp)
                    )
                }
                items(categories.filter { it.isActive }) { cat ->
                    val isSelected = selectedCategoryIdFilter == cat.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategoryFilterChange(if (isSelected) "ALL" else cat.id) },
                        label = { Text(cat.name, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = JudoRed,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.height(30.dp)
                    )
                }
                if (coaches.isNotEmpty()) {
                    item {
                        FilterChip(
                            selected = selectedCoachFilter == "ALL",
                            onClick = { selectedCoachFilter = "ALL" },
                            label = { Text("Tous les coachs", fontSize = 11.sp) },
                            modifier = Modifier.height(30.dp)
                        )
                    }
                    items(coaches.filter { it.isActive }) { coach ->
                        val isSelected = selectedCoachFilter == coach.name
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCoachFilter = if (isSelected) "ALL" else coach.name },
                            label = { Text(coach.name.split(" ").takeLast(1).firstOrNull() ?: coach.name, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(12.dp))
                            },
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }
            }

            // 4. VUES SELON LE MODE
            when (activeViewMode) {
                ScheduleViewMode.WEEK_AGENDA -> {
                    // EN-TÊTE DE L'AGENDA : Une seule lettre pour chaque jour (L, M, M, J, V, S, D)
                    // Colonnes de même largeur, centrées, sans débordement horizontal
                    // Sous-titre "Mar" / "Mer" pour distinguer immédiatement les deux M
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            daysOfWeek.forEachIndexed { index, day ->
                                val isSelected = selectedDayIndex == index
                                val daySessionsCount = filteredSessions.count { it.dayOfWeekName.equals(day, ignoreCase = true) }
                                val letter = dayLetters[index]
                                val subtitle = daySubtitles[index]

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) JudoRed else Color.Transparent,
                                    border = if (isSelected) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedDayIndex = index }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = letter,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                        if (daySessionsCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) JudoGold else JudoRed)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.height(7.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // BARRE D'ACTION JUSTE AU-DESSUS DE L'AGENDA
                    // Nom complet du jour sélectionné (identification formelle évitant toute confusion des deux M)
                    // Bouton « + Ajouter » compact aligné à droite, jamais caché, jamais superposé
                    val currentDay = daysOfWeek[selectedDayIndex]
                    val daySessions = filteredSessions.filter { it.dayOfWeekName.equals(currentDay, ignoreCase = true) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = JudoRed.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = currentDay,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = JudoRed,
                                        fontSize = 14.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = when (daySessions.size) {
                                    0 -> "Aucun créneau"
                                    1 -> "1 créneau"
                                    else -> "${daySessions.size} créneaux"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        // Bouton compact d'ajout, accessible et clairement visible
                        Button(
                            onClick = { onAddNewSession(currentDay, "17:30") },
                            colors = ButtonDefaults.buttonColors(containerColor = JudoRed),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = if (isTablet) 14.dp else 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_schedule_session_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Ajouter un créneau",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isTablet) "Ajouter un créneau" else "Ajouter",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // LISTE DES CRÉNEAUX DU JOUR
                    if (daySessions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "Aucun créneau programmé le $currentDay",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                OutlinedButton(
                                    onClick = { onAddNewSession(currentDay, "17:30") },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Planifier sur ce créneau", fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(daySessions, key = { it.id }) { session ->
                                SessionCardBlock(
                                    session = session,
                                    onClick = { sessionForDetailsDialog = session }
                                )
                            }
                        }
                    }
                }

                ScheduleViewMode.LIST_VIEW -> {
                    // VUE LISTE COMPLÈTE
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tous les créneaux (${filteredSessions.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Button(
                            onClick = { onAddNewSession(daysOfWeek.firstOrNull(), "17:30") },
                            colors = ButtonDefaults.buttonColors(containerColor = JudoRed),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_schedule_session_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Ajouter", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ajouter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (filteredSessions.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Aucune séance trouvée avec les filtres sélectionnés.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredSessions, key = { it.id }) { session ->
                                SessionCardBlock(
                                    session = session,
                                    onClick = { sessionForDetailsDialog = session }
                                )
                            }
                        }
                    }
                }

                ScheduleViewMode.TABLE_GRID -> {
                    // GRILLE POUR TABLETTE / ORDINATEUR
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Grille hebdomadaire complète",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Button(
                            onClick = { onAddNewSession(daysOfWeek.firstOrNull(), "17:30") },
                            colors = ButtonDefaults.buttonColors(containerColor = JudoRed),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_schedule_session_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Ajouter un créneau", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ajouter un créneau", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        daysOfWeek.forEach { day ->
                            val daySessions = filteredSessions.filter { it.dayOfWeekName.equals(day, ignoreCase = true) }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                border = CardDefaults.outlinedCardBorder(),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = JudoRed.copy(alpha = 0.12f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = day,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = JudoRed),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        daySessions.forEach { session ->
                                            SessionMiniGridCard(
                                                session = session,
                                                onClick = { sessionForDetailsDialog = session }
                                            )
                                        }

                                        IconButton(
                                            onClick = { onAddNewSession(day, "17:30") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(28.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Ajouter le $day", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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

    // BOÎTE DE DIALOGUE : DÉTAILS ET ACTIONS DU CRÉNEAU
    val activeDetail = sessionForDetailsDialog
    if (activeDetail != null) {
        AlertDialog(
            onDismissRequest = { sessionForDetailsDialog = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(getCategoryColor(activeDetail.categoryId))
                    )
                    Text(
                        text = "${activeDetail.categoryName} • ${activeDetail.dayOfWeekName}",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Créneau : ${activeDetail.startTime} - ${activeDetail.endTime} (${activeDetail.durationMinutes} min)",
                                fontWeight = FontWeight.Bold
                            )
                            if (activeDetail.coachNames.isNotEmpty()) {
                                Text(
                                    text = "Coach(s) affecté(s) : ${activeDetail.coachNames.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = if (activeDetail.isRecurringWeekly) "Répétition : Chaque semaine (${activeDetail.dayOfWeekName})" else "Séance ponctuelle",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            if (activeDetail.notes.isNotBlank()) {
                                Text(
                                    text = "Programme : ${activeDetail.notes}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = JudoGold, fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            val target = activeDetail
                            sessionForDetailsDialog = null
                            onEditSession(target)
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Modifier")
                    }

                    OutlinedButton(
                        onClick = {
                            val target = activeDetail
                            sessionForDetailsDialog = null
                            onDuplicateSession(target)
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dupliquer")
                    }

                    Button(
                        onClick = {
                            sessionToDelete = activeDetail
                            sessionForDetailsDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Supprimer")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionForDetailsDialog = null }) {
                    Text("Fermer")
                }
            }
        )
    }

    // BOÎTE DE DIALOGUE : CONFIRMATION DE SUPPRESSION
    val deleting = sessionToDelete
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Supprimer ce créneau ?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Êtes-vous sûr de vouloir supprimer le créneau de ${deleting.categoryName} (${deleting.dayOfWeekName} ${deleting.startTime} - ${deleting.endTime}) ?")
                    if (deleting.isRecurringWeekly && deleting.seriesId != null) {
                        Text(
                            "Ce créneau fait partie d'une série hebdomadaire récurrente.",
                            style = MaterialTheme.typography.bodySmall.copy(color = JudoGold, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (deleting.isRecurringWeekly && deleting.seriesId != null) {
                        OutlinedButton(
                            onClick = {
                                onDeleteSession(deleting.id, RecurrenceScope.ALL_SERIES)
                                sessionToDelete = null
                            }
                        ) {
                            Text("Toute la série")
                        }
                    }

                    Button(
                        onClick = {
                            onDeleteSession(deleting.id, RecurrenceScope.THIS_ONLY)
                            sessionToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Confirmer")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Retour")
                }
            }
        )
    }
}

@Composable
private fun SessionCardBlock(
    session: ScheduledTrainingSession,
    onClick: () -> Unit
) {
    val categoryColor = getCategoryColor(session.categoryId)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 36.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(categoryColor)
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = session.categoryName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = categoryColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = session.groupCode,
                                style = MaterialTheme.typography.labelSmall.copy(color = categoryColor, fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${session.startTime} - ${session.endTime} (${session.durationMinutes} min)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        )
                    }

                    if (session.coachNames.isNotEmpty()) {
                        Text(
                            text = "Coach : ${session.coachNames.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (session.isRecurringWeekly) JudoSuccess.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (session.isRecurringWeekly) "Hebdo" else "Unique",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (session.isRecurringWeekly) JudoSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Détails",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SessionMiniGridCard(
    session: ScheduledTrainingSession,
    onClick: () -> Unit
) {
    val categoryColor = getCategoryColor(session.categoryId)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = categoryColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "${session.startTime} - ${session.endTime}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = categoryColor, fontSize = 10.sp)
            )
            Text(
                text = session.categoryName,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = session.coachNames.firstOrNull()?.split(" ")?.takeLast(1)?.firstOrNull() ?: "",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp),
                maxLines = 1
            )
        }
    }
}

fun getCategoryColor(categoryId: String): Color {
    return when (categoryId.lowercase()) {
        "baby" -> Color(0xFFE91E63) // Rose
        "poussins" -> Color(0xFFFF9800) // Orange
        "benjamins" -> Color(0xFF2196F3) // Bleu
        "minimes" -> Color(0xFF4CAF50) // Vert
        "cadets" -> Color(0xFF9C27B0) // Violet
        "adultes" -> Color(0xFF3F51B5) // Indigo
        else -> JudoRed
    }
}

@Composable
fun ScheduleCategoryLegend(
    categories: List<JudoCategory>,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "Légende des créneaux par catégorie :",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(categories.filter { it.isActive }) { cat ->
                    val color = getCategoryColor(cat.id)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Text(cat.name, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
