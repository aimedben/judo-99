package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CoachUser
import com.example.data.model.ConflictType
import com.example.data.model.JudoCategory
import com.example.data.model.RecurrenceScope
import com.example.data.model.ScheduledTrainingSession
import com.example.data.model.SessionConflict
import com.example.data.model.SessionStatus
import com.example.ui.theme.JudoGold
import com.example.ui.theme.JudoGoldLight
import com.example.ui.theme.JudoRed
import com.example.ui.theme.JudoSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditSessionDialog(
    initialSession: ScheduledTrainingSession? = null,
    categories: List<JudoCategory>,
    coaches: List<CoachUser>,
    preselectedCategoryId: String? = null,
    preselectedDayName: String? = null,
    preselectedStartTime: String? = null,
    onCheckConflicts: (ScheduledTrainingSession, String?) -> List<SessionConflict>,
    onSaveSession: (ScheduledTrainingSession, Int) -> Unit,
    onDismiss: () -> Unit
) {
    // 1. Catégorie
    var selectedCategoryId by remember {
        mutableStateOf(
            initialSession?.categoryId
                ?: preselectedCategoryId
                ?: categories.firstOrNull { it.isActive }?.id
                ?: categories.firstOrNull()?.id
                ?: ""
        )
    }
    var isCategoryDropdownOpen by remember { mutableStateOf(false) }

    val activeCategory = categories.find { it.id == selectedCategoryId }

    // 2. Coach(s)
    var selectedCoachNames by remember {
        mutableStateOf(
            initialSession?.coachNames
                ?: if (activeCategory != null) {
                    listOfNotNull(
                        activeCategory.coach1Name.ifBlank { null },
                        activeCategory.coach2Name.ifBlank { null }
                    )
                } else emptyList()
        )
    }

    // 3. Jour et date
    val daysOfWeek = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")
    var selectedDays by remember {
        mutableStateOf(
            if (initialSession != null) setOf(initialSession.dayOfWeekName)
            else if (preselectedDayName != null) setOf(preselectedDayName)
            else setOf("Lundi")
        )
    }
    val primarySelectedDay = selectedDays.firstOrNull() ?: "Mercredi"
    var dateString by remember {
        mutableStateOf(initialSession?.dateString ?: "2026-10-14")
    }

    // 4. Heure de début
    var startTime by remember {
        mutableStateOf(initialSession?.startTime ?: preselectedStartTime ?: "17:30")
    }

    // 5. Heure de fin
    var endTime by remember {
        mutableStateOf(initialSession?.endTime ?: "19:00")
    }

    // 6. Répétition hebdomadaire
    var isRecurringWeekly by remember {
        mutableStateOf(initialSession?.isRecurringWeekly ?: true)
    }

    // 7. Nombre de semaines de répétition
    var repeatWeeksCount by remember { mutableStateOf(12) }
    var recurrenceEndDate by remember {
        mutableStateOf(initialSession?.recurrenceEndDate ?: "2027-01-15")
    }

    // 8. Notes
    var notes by remember {
        mutableStateOf(initialSession?.notes ?: "")
    }

    // Override exception checkbox if non-blocking conflict warning
    var forceOverrideWarning by remember { mutableStateOf(false) }

    // Build candidate session for live conflict checks
    val candidateSession = remember(
        selectedCategoryId,
        selectedCoachNames,
        primarySelectedDay,
        dateString,
        startTime,
        endTime,
        isRecurringWeekly,
        notes
    ) {
        val cat = categories.find { it.id == selectedCategoryId }
        ScheduledTrainingSession(
            id = initialSession?.id ?: "new_temp",
            categoryId = selectedCategoryId,
            categoryName = cat?.name ?: "Catégorie",
            groupCode = cat?.groupCode ?: "U15",
            dayOfWeekName = primarySelectedDay,
            dateString = dateString,
            formattedDisplayDate = "$primarySelectedDay $dateString",
            startTime = startTime,
            endTime = endTime,
            coachNames = selectedCoachNames,
            isRecurringWeekly = isRecurringWeekly,
            recurrenceEndDate = recurrenceEndDate,
            seriesId = initialSession?.seriesId,
            notes = notes,
            status = initialSession?.status ?: SessionStatus.SCHEDULED
        )
    }

    // Live conflict checks
    val conflicts = remember(candidateSession) {
        onCheckConflicts(candidateSession, initialSession?.id)
    }

    val blockingConflict = conflicts.firstOrNull { it.isBlocking }
    val warningConflicts = conflicts.filter { !it.isBlocking }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxWidth(0.94f)
                .imePadding()
                .padding(vertical = 16.dp)
                .testTag("create_edit_session_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(JudoRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsMartialArts,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (initialSession == null) "Planifier une séance" else "Modifier la séance",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                            )
                            Text(
                                text = "Judo Club Seddouk • Planning hebdomadaire",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                // 1. CATÉGORIE
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "1. Catégorie",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    ExposedDropdownMenuBox(
                        expanded = isCategoryDropdownOpen,
                        onExpandedChange = { isCategoryDropdownOpen = !isCategoryDropdownOpen }
                    ) {
                        OutlinedTextField(
                            value = activeCategory?.name ?: "Sélectionner une catégorie",
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = {
                                Icon(Icons.Default.Category, contentDescription = null, tint = JudoRed)
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownOpen)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = isCategoryDropdownOpen,
                            onDismissRequest = { isCategoryDropdownOpen = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(cat.name, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                cat.groupCode,
                                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedCategoryId = cat.id
                                        isCategoryDropdownOpen = false
                                        // Auto-prefill assigned category coaches
                                        val catCoaches = listOfNotNull(cat.coach1Name.ifBlank { null }, cat.coach2Name.ifBlank { null })
                                        if (catCoaches.isNotEmpty()) {
                                            selectedCoachNames = catCoaches
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // 2. COACH(S) AFFECTÉS
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "2. Coach(s) affecté(s)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            coaches.filter { it.isActive }.forEach { coach ->
                                val isSelected = selectedCoachNames.any { it.equals(coach.name, ignoreCase = true) }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedCoachNames = if (isSelected) {
                                                selectedCoachNames.filterNot { it.equals(coach.name, ignoreCase = true) }
                                            } else {
                                                selectedCoachNames + coach.name
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                selectedCoachNames = if (checked) {
                                                    selectedCoachNames + coach.name
                                                } else {
                                                    selectedCoachNames.filterNot { it.equals(coach.name, ignoreCase = true) }
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = JudoRed)
                                        )
                                        Column {
                                            Text(coach.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                            Text(coach.grade, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                        }
                                    }

                                    if (activeCategory != null && (activeCategory.coach1Name.equals(coach.name, ignoreCase = true) || activeCategory.coach2Name.equals(coach.name, ignoreCase = true))) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = JudoGold.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Référent",
                                                style = MaterialTheme.typography.labelSmall.copy(color = JudoGold, fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (selectedCoachNames.isEmpty()) {
                        Text(
                            text = "Veuillez sélectionner au moins un coach pour encadrer la séance.",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.error)
                        )
                    }
                }

                // 3. JOUR DE LA SEMAINE & DATE
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3. Jour(s) de la semaine",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (initialSession == null) {
                            Text(
                                text = "${selectedDays.size} jour(s) sélectionné(s)",
                                style = MaterialTheme.typography.labelSmall.copy(color = JudoRed, fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(daysOfWeek) { day ->
                            val isSelected = selectedDays.contains(day)
                            val shortLabel = when (day) {
                                "Lundi" -> "Lun"
                                "Mardi" -> "Mar"
                                "Mercredi" -> "Mer"
                                "Jeudi" -> "Jeu"
                                "Vendredi" -> "Ven"
                                "Samedi" -> "Sam"
                                "Dimanche" -> "Dim"
                                else -> day.take(3)
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedDays = if (initialSession != null) {
                                        setOf(day)
                                    } else {
                                        if (isSelected) {
                                            if (selectedDays.size > 1) selectedDays - day else selectedDays
                                        } else {
                                            selectedDays + day
                                        }
                                    }
                                },
                                label = { Text(shortLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = JudoRed,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    if (initialSession == null && selectedDays.size > 1) {
                        Text(
                            text = "Séance programmée simultanément chaque : ${selectedDays.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    OutlinedTextField(
                        value = dateString,
                        onValueChange = { dateString = it },
                        label = { Text("Date de début (AAAA-MM-JJ)") },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 4 & 5. HORAIRES (DÉBUT & FIN)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "4 & 5. Horaires (Format 24h)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Début (ex: 17:30)") },
                            leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("Fin (ex: 19:00)") },
                            leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Quick judo time slot presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "17:00" to "18:30",
                            "17:30" to "19:00",
                            "18:00" to "19:30",
                            "18:30" to "20:00"
                        ).forEach { (st, et) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (startTime == st && endTime == et) JudoRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, if (startTime == st && endTime == et) JudoRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        startTime = st
                                        endTime = et
                                    }
                            ) {
                                Text(
                                    text = "$st-$et",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (startTime == st && endTime == et) JudoRed else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    // Duration display badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "Durée calculée : ${candidateSession.durationMinutes} minutes",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // 6 & 7. RÉPÉTITION HEBDOMADAIRE
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.EventRepeat, contentDescription = null, tint = JudoRed)
                                Column {
                                    Text(
                                        text = "6. Répéter chaque semaine",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = if (isRecurringWeekly) "Créneau régulier hebdomadaire" else "Séance unique ponctuelle",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }

                            Switch(
                                checked = isRecurringWeekly,
                                onCheckedChange = { isRecurringWeekly = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = JudoRed)
                            )
                        }

                        if (isRecurringWeekly) {
                            OutlinedTextField(
                                value = recurrenceEndDate,
                                onValueChange = { recurrenceEndDate = it },
                                label = { Text("7. Date de fin de récurrence (Optionnel)") },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 8. NOTES FACULTATIVES
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("8. Thème / Notes de séance (Optionnel)") },
                    placeholder = { Text("ex: Préparation championnat, liaison sol, kata...") },
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // SMART CONFLICT NOTIFICATIONS
                if (blockingConflict != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Column {
                                Text(
                                    text = "Erreur bloquante",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                )
                                Text(
                                    text = blockingConflict.message,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
                                )
                            }
                        }
                    }
                } else if (warningConflicts.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = JudoGold.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, JudoGoldLight.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = JudoGoldLight)
                                Text(
                                    text = "Avertissement de chevauchement horaire",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = JudoGoldLight
                                    )
                                )
                            }
                            warningConflicts.forEach { warning ->
                                Text(
                                    text = "• ${warning.message}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Checkbox(
                                    checked = forceOverrideWarning,
                                    onCheckedChange = { forceOverrideWarning = it },
                                    colors = CheckboxDefaults.colors(checkedColor = JudoGold)
                                )
                                Text(
                                    text = "Confirmer cette exception horaire autorisée",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }

                // ACTIONS (ANNULER / ENREGISTRER)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Annuler")
                    }

                    val canSubmit = blockingConflict == null &&
                            (warningConflicts.isEmpty() || forceOverrideWarning) &&
                            selectedCategoryId.isNotBlank() &&
                            selectedCoachNames.isNotEmpty()

                    Button(
                        onClick = {
                            if (canSubmit) {
                                if (initialSession != null) {
                                    val day = selectedDays.firstOrNull() ?: primarySelectedDay
                                    onSaveSession(
                                        candidateSession.copy(
                                            dayOfWeekName = day,
                                            formattedDisplayDate = "$day $dateString"
                                        ),
                                        if (isRecurringWeekly) repeatWeeksCount else 1
                                    )
                                } else {
                                    selectedDays.forEach { dayName ->
                                        val s = candidateSession.copy(
                                            id = java.util.UUID.randomUUID().toString(),
                                            dayOfWeekName = dayName,
                                            formattedDisplayDate = "$dayName $dateString"
                                        )
                                        onSaveSession(s, if (isRecurringWeekly) repeatWeeksCount else 1)
                                    }
                                }
                                onDismiss()
                            }
                        },
                        enabled = canSubmit,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = JudoRed),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("submit_scheduled_session_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (initialSession == null) {
                                if (selectedDays.size > 1) "Enregistrer (${selectedDays.size} séances)" else "Enregistrer la séance"
                            } else "Mettre à jour",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
