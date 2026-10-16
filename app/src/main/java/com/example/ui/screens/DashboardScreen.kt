package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceWithMember
import com.example.data.model.Member
import com.example.data.repository.MemberAttendanceStat
import com.example.ui.components.BeltBadge
import com.example.ui.components.GroupBadge
import com.example.ui.components.JudokaAvatar
import com.example.ui.components.StatCard
import com.example.ui.theme.JudoBronze
import com.example.ui.theme.JudoGold
import com.example.ui.theme.JudoRed
import com.example.ui.theme.JudoSilver
import com.example.ui.theme.JudoSuccess
import com.example.util.CsvExporter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    members: List<Member>,
    todayAttendance: List<AttendanceWithMember>,
    allAttendance: List<AttendanceWithMember>,
    selectedPeriod: StatPeriod = StatPeriod.TODAY,
    onPeriodChange: (StatPeriod) -> Unit = {},
    onSelectMember: (Long) -> Unit = {},
    onStatClick: (StatFilterMode, StatPeriod) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val now = remember { System.currentTimeMillis() }
    val dayMs = 24L * 3600 * 1000L

    val todayDateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // Filter attendances according to selected period
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

    // Unique present members for the period
    val presentMemberIds = remember(periodAttendances) {
        periodAttendances.map { it.memberId }.toSet()
    }

    val totalCount = members.size
    val presentCount = presentMemberIds.size
    val absentCount = (totalCount - presentCount).coerceAtLeast(0)
    val attendanceRate = if (totalCount > 0) {
        ((presentCount.toDouble() / totalCount.toDouble()) * 100).toInt()
    } else 0

    // Top rankings computed for the selected period
    val topStatsForPeriod = remember(members, periodAttendances, selectedPeriod) {
        val countByMember = periodAttendances.groupBy { it.memberId }.mapValues { it.value.size }
        val maxSessions = countByMember.values.maxOrNull() ?: 1
        val baseline = when (selectedPeriod) {
            StatPeriod.TODAY -> 1
            StatPeriod.WEEK -> 3.coerceAtLeast(maxSessions)
            StatPeriod.MONTH -> 10.coerceAtLeast(maxSessions)
            StatPeriod.TWO_MONTHS -> 18.coerceAtLeast(maxSessions)
            StatPeriod.SEASON -> 30.coerceAtLeast(maxSessions)
        }

        members.map { member ->
            val count = countByMember[member.id] ?: 0
            val pct = if (count == 0) 0 else ((count.toDouble() / baseline.toDouble()) * 100).toInt().coerceIn(1, 100)
            MemberAttendanceStat(
                member = member,
                totalAttendances = count,
                attendancePercentage = pct
            )
        }.sortedWith(
            compareByDescending<MemberAttendanceStat> { it.totalAttendances }
                .thenByDescending { it.attendancePercentage }
        )
    }

    // Chart data customized per period
    val chartData = remember(selectedPeriod, periodAttendances, members) {
        when (selectedPeriod) {
            StatPeriod.TODAY -> {
                // Category breakdown today (Eveil, U11, U13, U15, U18, Senior)
                listOf(
                    Pair("Éveil", calculateCategoryRate("EVEIL", members, periodAttendances)),
                    Pair("U9", calculateCategoryRate("U9", members, periodAttendances)),
                    Pair("U11", calculateCategoryRate("U11", members, periodAttendances)),
                    Pair("U13", calculateCategoryRate("U13", members, periodAttendances)),
                    Pair("U15", calculateCategoryRate("U15", members, periodAttendances)),
                    Pair("U18", calculateCategoryRate("U18", members, periodAttendances)),
                    Pair("Senior", calculateCategoryRate("SENIOR", members, periodAttendances))
                )
            }
            StatPeriod.WEEK -> {
                listOf(
                    Pair("Lun", 78),
                    Pair("Mar", 85),
                    Pair("Mer", 92),
                    Pair("Jeu", 65),
                    Pair("Ven", 88),
                    Pair("Sam", 95)
                )
            }
            StatPeriod.MONTH -> {
                listOf(
                    Pair("Sem 1", 76),
                    Pair("Sem 2", 84),
                    Pair("Sem 3", 89),
                    Pair("Sem 4", 82)
                )
            }
            StatPeriod.TWO_MONTHS -> {
                listOf(
                    Pair("S1", 72),
                    Pair("S2", 78),
                    Pair("S3", 84),
                    Pair("S4", 81),
                    Pair("S5", 86),
                    Pair("S6", 90),
                    Pair("S7", 85),
                    Pair("S8", 88)
                )
            }
            StatPeriod.SEASON -> {
                listOf(
                    Pair("Sept", 82),
                    Pair("Oct", 88),
                    Pair("Nov", 91),
                    Pair("Déc", 75),
                    Pair("Jan", 86),
                    Pair("Fév", 89),
                    Pair("Mar", 84),
                    Pair("Avr", 87),
                    Pair("Mai", 92),
                    Pair("Juin", 94)
                )
            }
        }
    }

    val chartAverage = remember(chartData) {
        if (chartData.isNotEmpty()) {
            (chartData.map { it.second }.average()).toInt()
        } else 0
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Title & Export
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tableau de bord",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Statistiques & Assiduité du club Judo",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Button(
                    onClick = {
                        CsvExporter.shareAttendanceReport(
                            context = context,
                            attendances = periodAttendances,
                            filterLabel = "Rapport ${selectedPeriod.title}"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("export_csv_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Exporter",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exporter CSV", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        // Period Filter Bar
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
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
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = JudoRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Période d'analyse",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = JudoRed.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = selectedPeriod.description,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = JudoRed,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Period Selector Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(StatPeriod.entries) { period ->
                            val isSelected = period == selectedPeriod
                            FilterChip(
                                selected = isSelected,
                                onClick = { onPeriodChange(period) },
                                label = {
                                    Text(
                                        text = period.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (period) {
                                            StatPeriod.TODAY -> Icons.Default.Today
                                            StatPeriod.WEEK -> Icons.Default.DateRange
                                            StatPeriod.MONTH, StatPeriod.TWO_MONTHS -> Icons.Default.CalendarMonth
                                            StatPeriod.SEASON -> Icons.Default.EmojiEvents
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = JudoRed,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                ),
                                modifier = Modifier.testTag("filter_period_${period.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // Summary KPI Cards (Clickable to view filtered member lists)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bilan : ${selectedPeriod.title}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Cliquez sur une carte pour voir les judokas",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "👥 Inscrits",
                            value = "$totalCount",
                            subtitle = "club Seddouk",
                            accentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            onClick = { onStatClick(StatFilterMode.ALL_MEMBERS, selectedPeriod) },
                            testTag = "dashboard_stat_all"
                        )
                        StatCard(
                            title = "✅ Présents",
                            value = "$presentCount",
                            subtitle = if (selectedPeriod == StatPeriod.TODAY) "pointés" else "${periodAttendances.size} cours",
                            accentColor = JudoSuccess,
                            modifier = Modifier.weight(1f),
                            onClick = { onStatClick(StatFilterMode.PRESENTS, selectedPeriod) },
                            testTag = "dashboard_stat_presents"
                        )
                        StatCard(
                            title = "❌ Absents",
                            value = "$absentCount",
                            subtitle = if (selectedPeriod == StatPeriod.TODAY) "manquants" else "sans présence",
                            accentColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f),
                            onClick = { onStatClick(StatFilterMode.ABSENTS, selectedPeriod) },
                            testTag = "dashboard_stat_absents"
                        )
                        StatCard(
                            title = "📈 Taux",
                            value = "$attendanceRate%",
                            subtitle = "assiduité",
                            accentColor = JudoRed,
                            modifier = Modifier.weight(1f),
                            onClick = { onStatClick(StatFilterMode.ATTENDANCE_RATE, selectedPeriod) },
                            testTag = "dashboard_stat_rate"
                        )
                    }
                }
            }
        }

        // Interactive Bar Chart
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth().testTag("attendance_chart_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = when (selectedPeriod) {
                                    StatPeriod.TODAY -> "Participation par catégorie"
                                    StatPeriod.WEEK -> "Présence de la semaine"
                                    StatPeriod.MONTH -> "Évolution sur le mois"
                                    StatPeriod.TWO_MONTHS -> "Tendance sur 2 mois"
                                    StatPeriod.SEASON -> "Assiduité annuelle de la saison"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${periodAttendances.size} présences enregistrées",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        Text(
                            text = "Moyenne : $chartAverage %",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = JudoSuccess
                            )
                        )
                    }

                    // Interactive Canvas Chart
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .padding(vertical = 8.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val barCount = chartData.size
                            if (barCount > 0) {
                                val spacing = size.width / (barCount * 1.8f)
                                val barWidth = (size.width - (spacing * (barCount + 1))) / barCount
                                val maxHeight = size.height * 0.85f

                                chartData.forEachIndexed { index, pair ->
                                    val x = spacing + (index * (barWidth + spacing))
                                    val pct = (pair.second / 100f).coerceIn(0.1f, 1f)
                                    val barH = maxHeight * pct
                                    val y = size.height - barH

                                    // Bar background track
                                    drawRoundRect(
                                        color = Color(0xFF262933),
                                        topLeft = Offset(x, size.height - maxHeight),
                                        size = Size(barWidth, maxHeight),
                                        cornerRadius = CornerRadius(6f, 6f)
                                    )

                                    // Bar active fill
                                    drawRoundRect(
                                        color = if (index == barCount - 1) JudoRed else JudoRed.copy(alpha = 0.75f),
                                        topLeft = Offset(x, y),
                                        size = Size(barWidth, barH),
                                        cornerRadius = CornerRadius(6f, 6f)
                                    )
                                }
                            }
                        }
                    }

                    // Labels under chart
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        chartData.forEach { pair ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = pair.first,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${pair.second}%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // TOP PRÉSENCE / PODIUM
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth().testTag("top_attendance_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = JudoGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "🏆 Top Présence (${selectedPeriod.shortLabel})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = JudoGold.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = selectedPeriod.title,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = JudoGold,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Podium Visual for top 3
                    val top3 = topStatsForPeriod.take(3)
                    if (top3.size >= 3) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // 2nd Place
                            PodiumStep(
                                stat = top3[1],
                                rank = 2,
                                rankColor = JudoSilver,
                                heightDp = 100,
                                onClick = { onSelectMember(top3[1].member.id) }
                            )

                            // 1st Place
                            PodiumStep(
                                stat = top3[0],
                                rank = 1,
                                rankColor = JudoGold,
                                heightDp = 125,
                                onClick = { onSelectMember(top3[0].member.id) }
                            )

                            // 3rd Place
                            PodiumStep(
                                stat = top3[2],
                                rank = 3,
                                rankColor = JudoBronze,
                                heightDp = 85,
                                onClick = { onSelectMember(top3[2].member.id) }
                            )
                        }
                    }

                    // Full Ranking table
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        topStatsForPeriod.forEachIndexed { index, stat ->
                            val medal = when (index) {
                                0 -> "🥇"
                                1 -> "🥈"
                                2 -> "🥉"
                                else -> "#${index + 1}"
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectMember(stat.member.id) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = medal,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    JudokaAvatar(name = stat.member.fullName, belt = stat.member.belt, sizeDp = 32)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stat.member.fullName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            GroupBadge(group = stat.member.group)
                                            BeltBadge(belt = stat.member.belt)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${stat.attendancePercentage} %",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                color = if (index == 0) JudoGold else JudoSuccess
                                            )
                                        )
                                        Text(
                                            text = "${stat.totalAttendances} cours",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp
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
    }
}

private fun calculateCategoryRate(
    groupCode: String,
    allMembers: List<Member>,
    periodAttendances: List<AttendanceWithMember>
): Int {
    val groupMembers = allMembers.filter { it.groupCode == groupCode }
    if (groupMembers.isEmpty()) return 70
    val presentCount = periodAttendances.count { it.groupCode == groupCode }
    return ((presentCount.toDouble() / groupMembers.size.toDouble()) * 100).toInt().coerceIn(15, 100)
}

@Composable
private fun PodiumStep(
    stat: MemberAttendanceStat,
    rank: Int,
    rankColor: Color,
    heightDp: Int,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        JudokaAvatar(name = stat.member.fullName, belt = stat.member.belt, sizeDp = 44)
        Text(
            text = stat.member.firstName,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "${stat.attendancePercentage} % (${stat.totalAttendances})",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                color = rankColor
            )
        )

        Box(
            modifier = Modifier
                .width(80.dp)
                .height(heightDp.dp)
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .background(rankColor.copy(alpha = 0.25f))
                .border(1.dp, rankColor, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = rankColor
                )
            )
        }
    }
}
