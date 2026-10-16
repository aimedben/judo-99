package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
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
import com.example.data.model.AttendanceWithMember
import com.example.data.model.JudoGroup
import com.example.data.model.Member
import com.example.ui.components.BeltBadge
import com.example.ui.components.GroupBadge
import com.example.ui.components.JudokaAvatar
import com.example.ui.theme.JudoRed
import com.example.ui.theme.JudoSuccess

@Composable
fun StatFilteredMembersScreen(
    filterMode: StatFilterMode,
    statPeriod: StatPeriod,
    onPeriodChange: (StatPeriod) -> Unit,
    onFilterModeChange: (StatFilterMode) -> Unit = {},
    allMembers: List<Member>,
    todayAttendance: List<AttendanceWithMember>,
    allAttendance: List<AttendanceWithMember>,
    onSelectMember: (Long) -> Unit,
    onManualCheckIn: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val now = remember { System.currentTimeMillis() }
    val dayMs = 24L * 3600 * 1000L

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }

    // Filter attendances matching the active stat period
    val periodAttendances = remember(statPeriod, allAttendance, todayAttendance) {
        when (statPeriod) {
            StatPeriod.TODAY -> todayAttendance
            StatPeriod.WEEK -> {
                val cutoff = now - (7L * dayMs)
                allAttendance.filter { it.timestamp >= cutoff }
            }
            StatPeriod.MONTH -> {
                val cutoff = now - (30L * dayMs)
                allAttendance.filter { it.timestamp >= cutoff }
            }
            StatPeriod.TWO_MONTHS -> {
                val cutoff = now - (60L * dayMs)
                allAttendance.filter { it.timestamp >= cutoff }
            }
            StatPeriod.SEASON -> allAttendance
        }
    }

    val presentMemberIds = remember(periodAttendances) {
        periodAttendances.map { it.memberId }.toSet()
    }

    val attendanceCountByMember = remember(periodAttendances) {
        periodAttendances.groupBy { it.memberId }.mapValues { it.value.size }
    }

    // Determine target members based on chosen filter mode
    val targetMemberList = remember(filterMode, allMembers, presentMemberIds, attendanceCountByMember) {
        when (filterMode) {
            StatFilterMode.ABSENTS -> allMembers.filter { it.id !in presentMemberIds }
            StatFilterMode.PRESENTS -> allMembers.filter { it.id in presentMemberIds }
            StatFilterMode.ALL_MEMBERS -> allMembers
            StatFilterMode.ATTENDANCE_RATE -> allMembers.sortedByDescending {
                attendanceCountByMember[it.id] ?: 0
            }
        }
    }

    // Apply search query and group category filter
    val filteredList = remember(targetMemberList, searchQuery, selectedCategoryFilter) {
        targetMemberList.filter { member ->
            val matchesQuery = searchQuery.isBlank() ||
                member.fullName.contains(searchQuery, ignoreCase = true) ||
                member.qrCode.contains(searchQuery, ignoreCase = true) ||
                member.beltName.contains(searchQuery, ignoreCase = true) ||
                member.phone.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategoryFilter == "ALL" || member.groupCode == selectedCategoryFilter

            matchesQuery && matchesCategory
        }
    }

    val absentsCount = allMembers.count { it.id !in presentMemberIds }
    val presentsCount = presentMemberIds.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("stat_filtered_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("stat_filter_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour"
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = filterMode.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = filterMode.accentColor
                        )
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = filterMode.accentColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${filteredList.size}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = filterMode.accentColor
                            )
                        )
                    }
                }
                Text(
                    text = "${statPeriod.title} • ${statPeriod.description}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // Period Switcher Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(StatPeriod.entries) { period ->
                val isSelected = period == statPeriod
                FilterChip(
                    selected = isSelected,
                    onClick = { onPeriodChange(period) },
                    label = { Text(period.title, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = JudoRed,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Stat Mode Quick Toggle Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = filterMode == StatFilterMode.ABSENTS,
                    onClick = { onFilterModeChange(StatFilterMode.ABSENTS) },
                    label = { Text("❌ Absents ($absentsCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE53935),
                        selectedLabelColor = Color.White
                    )
                )
            }
            item {
                FilterChip(
                    selected = filterMode == StatFilterMode.PRESENTS,
                    onClick = { onFilterModeChange(StatFilterMode.PRESENTS) },
                    label = { Text("✅ Présents ($presentsCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2E7D32),
                        selectedLabelColor = Color.White
                    )
                )
            }
            item {
                FilterChip(
                    selected = filterMode == StatFilterMode.ALL_MEMBERS,
                    onClick = { onFilterModeChange(StatFilterMode.ALL_MEMBERS) },
                    label = { Text("👥 Tous (${allMembers.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = JudoRed,
                        selectedLabelColor = Color.White
                    )
                )
            }
            item {
                FilterChip(
                    selected = filterMode == StatFilterMode.ATTENDANCE_RATE,
                    onClick = { onFilterModeChange(StatFilterMode.ATTENDANCE_RATE) },
                    label = { Text("📈 Taux d'assiduité") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = JudoRed,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Rechercher nom, prénom, ceinture, tel...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("stat_search_input")
        )

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedCategoryFilter == "ALL",
                    onClick = { selectedCategoryFilter = "ALL" },
                    label = { Text("Tous (${targetMemberList.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = filterMode.accentColor,
                        selectedLabelColor = Color.White
                    )
                )
            }
            items(JudoGroup.entries) { group ->
                val countInGroup = targetMemberList.count { it.groupCode == group.code }
                FilterChip(
                    selected = selectedCategoryFilter == group.code,
                    onClick = { selectedCategoryFilter = group.code },
                    label = { Text("${group.code} ($countInGroup)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = filterMode.accentColor,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // List of Members
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filterMode == StatFilterMode.ABSENTS && targetMemberList.isEmpty()) {
                        Text(
                            text = "🎉 Aucun absent pour ${statPeriod.title.lowercase()} !",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = JudoSuccess
                            )
                        )
                        Text(
                            text = "Tous les judokas inscrits ont participé aux séances sur cette période.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = "Aucun judoka correspondant aux critères.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredList) { member ->
                    val isCheckedInThisPeriod = member.id in presentMemberIds
                    val memberSessionCount = attendanceCountByMember[member.id] ?: 0
                    val isCheckedInToday = todayAttendance.any { it.memberId == member.id }
                    val todayInfo = todayAttendance.find { it.memberId == member.id }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectMember(member.id) }
                            .testTag("stat_member_item_${member.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            JudokaAvatar(
                                name = member.fullName,
                                belt = member.belt,
                                photoUrl = member.photoUrl,
                                sizeDp = 48
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.fullName,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                Spacer(modifier = Modifier.size(2.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    GroupBadge(group = member.group)
                                    BeltBadge(belt = member.belt)
                                }

                                Spacer(modifier = Modifier.size(2.dp))

                                if (isCheckedInThisPeriod) {
                                    Text(
                                        text = if (statPeriod == StatPeriod.TODAY && todayInfo != null) {
                                            "Pointé aujourd'hui à ${todayInfo.timeString} ✓"
                                        } else {
                                            "Présent(e) • $memberSessionCount séances (${statPeriod.shortLabel}) ✓"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = JudoSuccess,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                } else {
                                    Text(
                                        text = if (statPeriod == StatPeriod.TODAY) {
                                            "Non pointé aujourd'hui"
                                        } else {
                                            "Aucune présence (${statPeriod.shortLabel})"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }

                            // Action buttons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Call Parent button if available
                                if (member.phone.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:${member.phone.replace(" ", "")}")
                                            }
                                            context.startActivity(dialIntent)
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .testTag("call_member_${member.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = "Appeler parent",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Quick Check-In button if not yet checked in today!
                                if (!isCheckedInToday) {
                                    Button(
                                        onClick = { onManualCheckIn(member.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = JudoSuccess),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("mark_present_${member.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Pointer",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
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
