package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Attendance
import com.example.data.model.AttendanceWithMember
import com.example.data.model.Member
import com.example.ui.theme.JudoRed
import com.example.ui.theme.JudoSuccess
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// French week day names for calendar headers (Sunday to Saturday: D, L, M, M, J, V, S)
private val WEEK_DAYS = listOf("D", "L", "M", "M", "J", "V", "S")

@Composable
fun MemberAttendanceCalendar(
    member: Member,
    attendanceList: List<Attendance>,
    allClubAttendances: List<AttendanceWithMember> = emptyList(),
    onRecordAttendanceForDate: (Long, String) -> Unit = { _, _ -> },
    onDeleteAttendanceForDate: (Long, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val todayCalendar = remember { Calendar.getInstance() }
    val todaySdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayDateString = remember { todaySdf.format(todayCalendar.time) }

    // Displayed month and year
    var displayedCalendar by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
            }
        )
    }

    // Default training days: Mercredi (4), Vendredi (6), Samedi (7)
    val selectedTrainingDays = remember {
        setOf(Calendar.WEDNESDAY, Calendar.FRIDAY, Calendar.SATURDAY)
    }

    // Selected date to view details
    var selectedDateString by remember { mutableStateOf<String?>(todayDateString) }

    // Map of this member's attendances by dateString (YYYY-MM-DD)
    val memberAttendanceMap = remember(attendanceList) {
        attendanceList.associateBy { it.dateString }
    }

    // Dates where ANY session was held by the club
    val clubSessionDates = remember(allClubAttendances) {
        allClubAttendances.map { it.dateString }.toSet()
    }

    // Month label formatter
    val monthTitleFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.FRENCH) }
    val monthTitle = remember(displayedCalendar) {
        monthTitleFormat.format(displayedCalendar.time).replaceFirstChar { it.uppercase() }
    }

    // Days computation for current month
    val year = displayedCalendar.get(Calendar.YEAR)
    val month = displayedCalendar.get(Calendar.MONTH)

    val maxDaysInMonth = remember(year, month) {
        val cal = (displayedCalendar.clone() as Calendar)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // Index of the first day of the week (Sunday = 0 ... Saturday = 6) to align with WEEK_DAYS: D, L, M, M, J, V, S
    val firstDayOffset = remember(year, month) {
        val cal = (displayedCalendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        // Convert Calendar.DAY_OF_WEEK to 0..6 (where Sunday is 0 to match WEEK_DAYS)
        when (dow) {
            Calendar.SUNDAY -> 0
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 0
        }
    }

    // Calculate monthly statistics for this displayed month
    var monthlyTotalTrainingSessions = 0
    var monthlyPresentSessions = 0
    var monthlyAbsentSessions = 0

    val currentMonthDays = (1..maxDaysInMonth).map { dayNumber ->
        val dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayNumber)

        val dayCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, dayNumber)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }

        val dayOfWeek = dayCal.get(Calendar.DAY_OF_WEEK)
        val isPastOrToday = dayCal.timeInMillis <= todayCalendar.timeInMillis || dateStr == todayDateString
        val isToday = dateStr == todayDateString

        // It is considered a training day if it matches selected training days,
        // or if a club session was held, or if the member attended
        val isTrainingDay = selectedTrainingDays.contains(dayOfWeek) ||
                clubSessionDates.contains(dateStr) ||
                memberAttendanceMap.containsKey(dateStr)

        val attendance = memberAttendanceMap[dateStr]
        val isPresent = attendance != null

        if (isTrainingDay && isPastOrToday) {
            monthlyTotalTrainingSessions++
            if (isPresent) {
                monthlyPresentSessions++
            } else {
                monthlyAbsentSessions++
            }
        }

        CalendarDayData(
            dayNumber = dayNumber,
            dateString = dateStr,
            dayOfWeek = dayOfWeek,
            isToday = isToday,
            isPastOrToday = isPastOrToday,
            isTrainingDay = isTrainingDay,
            isPresent = isPresent,
            attendance = attendance
        )
    }

    val attendanceRate = if (monthlyTotalTrainingSessions > 0) {
        ((monthlyPresentSessions.toDouble() / monthlyTotalTrainingSessions) * 100).toInt()
    } else {
        100
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier
            .fillMaxWidth()
            .testTag("attendance_calendar_section")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title and subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(JudoRed.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = JudoRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Agenda des Présences",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Vert = Présent • Rouge = Absent",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Month Jump: "Aujourd'hui" button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable {
                        displayedCalendar = Calendar.getInstance().apply {
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                        selectedDateString = todayDateString
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Revenir au mois actuel",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Aujourd'hui",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Month Navigator Bar
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newCal = (displayedCalendar.clone() as Calendar).apply {
                                add(Calendar.MONTH, -1)
                            }
                            displayedCalendar = newCal
                        },
                        modifier = Modifier.testTag("calendar_prev_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Mois précédent"
                        )
                    }

                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )

                    IconButton(
                        onClick = {
                            val newCal = (displayedCalendar.clone() as Calendar).apply {
                                add(Calendar.MONTH, 1)
                            }
                            displayedCalendar = newCal
                        },
                        modifier = Modifier.testTag("calendar_next_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Mois suivant"
                        )
                    }
                }
            }

            // Monthly Statistics Bar (Séances, Présences, Absences, Taux)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MonthStatChip(
                    label = "Séances",
                    count = "$monthlyTotalTrainingSessions",
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                MonthStatChip(
                    label = "Présent",
                    count = "$monthlyPresentSessions",
                    color = JudoSuccess,
                    isPositive = true,
                    modifier = Modifier.weight(1f)
                )
                MonthStatChip(
                    label = "Absent",
                    count = "$monthlyAbsentSessions",
                    color = JudoRed,
                    isNegative = true,
                    modifier = Modifier.weight(1f)
                )
                MonthStatChip(
                    label = "Assiduité",
                    count = "$attendanceRate%",
                    color = if (attendanceRate >= 75) JudoSuccess else if (attendanceRate >= 50) Color(0xFFF59E0B) else JudoRed,
                    modifier = Modifier.weight(1f)
                )
            }

            // Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendIndicator(color = JudoSuccess, label = "Présent")
                LegendIndicator(color = JudoRed, label = "Absent")
                LegendIndicator(color = MaterialTheme.colorScheme.surfaceVariant, label = "Repos")
                LegendIndicator(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), label = "Prévu")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // The Calendar Grid: Day Headers (Lun ... Dim)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WEEK_DAYS.forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Grid Days
            val totalCells = firstDayOffset + maxDaysInMonth
            val totalRows = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (rowIndex in 0 until totalRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (colIndex in 0 until 7) {
                            val cellIndex = rowIndex * 7 + colIndex
                            val dayNumber = cellIndex - firstDayOffset + 1

                            if (cellIndex < firstDayOffset || dayNumber > maxDaysInMonth) {
                                // Empty spacer for padding outside of this month
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            } else {
                                val dayData = currentMonthDays[dayNumber - 1]
                                val isSelected = dayData.dateString == selectedDateString

                                CalendarDayCell(
                                    dayData = dayData,
                                    isSelected = isSelected,
                                    onClick = {
                                        selectedDateString = dayData.dateString
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Day Detail & Quick Action Card
            val selectedDayData = currentMonthDays.find { it.dateString == selectedDateString }
            if (selectedDayData != null) {
                AnimatedVisibility(visible = true) {
                    SelectedDayDetailCard(
                        dayData = selectedDayData,
                        member = member,
                        onRecordAttendance = {
                            onRecordAttendanceForDate(member.id, selectedDayData.dateString)
                        },
                        onDeleteAttendance = {
                            onDeleteAttendanceForDate(member.id, selectedDayData.dateString)
                        }
                    )
                }
            }
        }
    }
}

private data class CalendarDayData(
    val dayNumber: Int,
    val dateString: String,
    val dayOfWeek: Int,
    val isToday: Boolean,
    val isPastOrToday: Boolean,
    val isTrainingDay: Boolean,
    val isPresent: Boolean,
    val attendance: Attendance?
)

@Composable
private fun CalendarDayCell(
    dayData: CalendarDayData,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine cell styling based on training day & presence
    val cellColor: Color
    val contentColor: Color
    val borderColor: Color?
    val badgeIcon: (@Composable () -> Unit)?

    when {
        // PRESENT: GREEN (Vert)
        dayData.isPresent -> {
            cellColor = JudoSuccess
            contentColor = Color.White
            borderColor = if (isSelected) Color.White else null
            badgeIcon = {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Présent",
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        // TRAINING DAY & MISSED (Past or Today): RED (Rouge)
        dayData.isTrainingDay && dayData.isPastOrToday -> {
            cellColor = JudoRed
            contentColor = Color.White
            borderColor = if (isSelected) Color.White else null
            badgeIcon = {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Absent",
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        // FUTURE TRAINING DAY: Scheduled accent
        dayData.isTrainingDay && !dayData.isPastOrToday -> {
            cellColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            badgeIcon = {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        // REGULAR NON-TRAINING DAY (REST)
        else -> {
            cellColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            borderColor = null
            badgeIcon = null
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(cellColor)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(10.dp))
                } else if (dayData.isToday) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                } else if (borderColor != null) {
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(10.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${dayData.dayNumber}",
                fontSize = 12.sp,
                fontWeight = if (dayData.isPresent || (dayData.isTrainingDay && dayData.isPastOrToday)) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )

            if (badgeIcon != null) {
                Spacer(modifier = Modifier.height(1.dp))
                badgeIcon()
            }
        }
    }
}

@Composable
private fun SelectedDayDetailCard(
    dayData: CalendarDayData,
    member: Member,
    onRecordAttendance: () -> Unit,
    onDeleteAttendance: () -> Unit
) {
    // Format full date in French
    val parsedDate = remember(dayData.dateString) {
        try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dayData.dateString)
        } catch (_: Exception) {
            Date()
        }
    }
    val fullDateString = remember(parsedDate) {
        val sdf = SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH)
        sdf.format(parsedDate ?: Date()).replaceFirstChar { it.uppercase() }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
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
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = fullDateString,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (dayData.isToday) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "Aujourd'hui",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Status Row
            when {
                // PRESENT: GREEN
                dayData.isPresent -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = JudoSuccess.copy(alpha = 0.12f),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = JudoSuccess,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Présent(e) à l'entraînement ✓",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = JudoSuccess
                                        )
                                    )
                                    val time = dayData.attendance?.timeString ?: "18:00"
                                    val method = dayData.attendance?.checkInMethod ?: "QR_SCAN"
                                    Text(
                                        text = "Pointé à $time • Mode: $method",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = onDeleteAttendance,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = JudoRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Retirer", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // MISSED TRAINING: RED
                dayData.isTrainingDay && dayData.isPastOrToday -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = JudoRed.copy(alpha = 0.12f),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = JudoRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Absent(e) à l'entraînement ✗",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = JudoRed
                                        )
                                    )
                                    Text(
                                        text = "Séance club non validée pour ${member.firstName}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                            Button(
                                onClick = onRecordAttendance,
                                colors = ButtonDefaults.buttonColors(containerColor = JudoSuccess),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Marquer présent", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // FUTURE TRAINING
                dayData.isTrainingDay && !dayData.isPastOrToday -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Séance d'entraînement à venir",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                            }

                            OutlinedButton(
                                onClick = onRecordAttendance,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Pointage manuel", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // REST DAY
                else -> {
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
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Pas de séance officielle ce jour (Repos)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        TextButton(onClick = onRecordAttendance) {
                            Text("+ Séance extra", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthStatChip(
    label: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier,
    isPositive: Boolean = false,
    isNegative: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = when {
            isPositive -> JudoSuccess.copy(alpha = 0.12f)
            isNegative -> JudoRed.copy(alpha = 0.12f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LegendIndicator(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}
