package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.example.data.model.AttendanceWithMember
import com.example.data.model.JudoCategory
import com.example.data.model.Member
import com.example.data.model.PastTrainingSession
import com.example.ui.components.JudokaAvatar
import com.example.ui.theme.JudoGold
import com.example.ui.theme.JudoRed
import com.example.ui.theme.JudoSuccess

@Composable
fun AdminAttendanceOverviewScreen(
    allMembers: List<Member>,
    todayAttendance: List<AttendanceWithMember>,
    allAttendance: List<AttendanceWithMember>,
    categories: List<JudoCategory>,
    pastSessions: List<PastTrainingSession>,
    onSelectMember: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Par Athlète, 1: Par Catégorie, 2: Par Séance
    var selectedPeriod by remember { mutableStateOf(StatPeriod.MONTH) }

    val now = remember { System.currentTimeMillis() }
    val dayMs = 24L * 3600 * 1000L

    val periodAttendances = remember(selectedPeriod, allAttendance, todayAttendance) {
        when (selectedPeriod) {
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_attendance_overview_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Title & Back
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
            }
            Column {
                Text(
                    text = "Présences & Absences",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Analyse détaillée de l'assiduité du dojo",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // Period filter chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(StatPeriod.entries.toTypedArray()) { p ->
                FilterChip(
                    selected = selectedPeriod == p,
                    onClick = { selectedPeriod = p },
                    label = { Text(p.frenchLabel) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = JudoRed,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Tabs : Par Athlète / Par Catégorie / Par Séance
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Par Athlète") },
                icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Par Catégorie") },
                icon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Par Séance") },
                icon = { Icon(Icons.Default.EventNote, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        when (selectedTab) {
            0 -> {
                // Par Athlète
                val attendanceCounts = remember(periodAttendances) {
                    periodAttendances.groupBy { it.memberId }.mapValues { it.value.size }
                }

                val sortedMembers = remember(allMembers, attendanceCounts) {
                    allMembers.sortedByDescending { attendanceCounts[it.id] ?: 0 }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(sortedMembers) { member ->
                        val count = attendanceCounts[member.id] ?: 0
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectMember(member.id) }
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
                                        name = member.fullName,
                                        belt = member.belt,
                                        photoUrl = member.photoUrl,
                                        sizeDp = 42
                                    )
                                    Column {
                                        Text(
                                            text = member.fullName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${member.group.label} • Ceinture ${member.belt.displayName}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (count > 0) JudoSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "$count présence${if (count > 1) "s" else ""}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (count > 0) JudoSuccess else MaterialTheme.colorScheme.error
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Par Catégorie
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(categories) { cat ->
                        val catMembers = allMembers.filter { it.groupCode.equals(cat.groupCode, ignoreCase = true) || it.group.name.equals(cat.name, ignoreCase = true) }
                        val catMemberIds = catMembers.map { it.id }.toSet()
                        val catAttendances = periodAttendances.filter { it.memberId in catMemberIds }
                        val uniqueAttendees = catAttendances.map { it.memberId }.distinct().size
                        val rate = if (catMembers.isNotEmpty()) ((uniqueAttendees.toDouble() / catMembers.size) * 100).toInt() else 0

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = JudoSuccess.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "$rate% assiduité",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = JudoSuccess)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$uniqueAttendees judokas venus sur ${catMembers.size} inscrits",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    Text(
                                        text = "${catAttendances.size} séances cumulées",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Par Séance
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(pastSessions) { session ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Séance ${session.categoryName}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = session.dateString,
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "🟢 ${session.presentCount} présents   🔴 ${session.absentCount} absents",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Coach : ${session.coach1Name}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = JudoGold)
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
