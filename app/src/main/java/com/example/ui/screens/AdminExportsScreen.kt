package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceWithMember
import com.example.data.model.JudoCategory
import com.example.data.model.Member
import com.example.ui.theme.JudoGold
import com.example.ui.theme.JudoRed
import com.example.util.CsvExporter

@Composable
fun AdminExportsScreen(
    allMembers: List<Member>,
    allAttendance: List<AttendanceWithMember>,
    todayAttendance: List<AttendanceWithMember>,
    categories: List<JudoCategory>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var selectedPeriodFilter by remember { mutableStateOf(StatPeriod.MONTH) }

    val now = remember { System.currentTimeMillis() }
    val dayMs = 24L * 3600 * 1000L

    val filteredMembers = remember(allMembers, selectedCategoryFilter) {
        if (selectedCategoryFilter == "ALL") allMembers
        else allMembers.filter { it.groupCode.equals(selectedCategoryFilter, ignoreCase = true) }
    }

    val filteredAttendances = remember(selectedPeriodFilter, selectedCategoryFilter, allAttendance, todayAttendance) {
        val baseList = when (selectedPeriodFilter) {
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

        if (selectedCategoryFilter == "ALL") baseList
        else baseList.filter { it.groupCode.equals(selectedCategoryFilter, ignoreCase = true) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_exports_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
                Column {
                    Text(
                        text = "Exports & Rapports",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Génération de bilans et exports CSV conformes",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        // Filters Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Filtres d'exportation :",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    // Category filter
                    Text(text = "Catégorie :", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCategoryFilter == "ALL",
                                onClick = { selectedCategoryFilter = "ALL" },
                                label = { Text("Toutes catégories") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = JudoRed,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategoryFilter == cat.groupCode,
                                onClick = { selectedCategoryFilter = cat.groupCode },
                                label = { Text(cat.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = JudoRed,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Period filter
                    Text(text = "Période de présence :", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(StatPeriod.entries.toTypedArray()) { p ->
                            FilterChip(
                                selected = selectedPeriodFilter == p,
                                onClick = { selectedPeriodFilter = p },
                                label = { Text(p.frenchLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = JudoRed,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Export 1: Registre des Athlètes
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF2196F3), modifier = Modifier.size(28.dp))
                            Column {
                                Text(
                                    text = "Registre des Athlètes (CSV)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${filteredMembers.size} athlètes sélectionnés",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Contient noms, prénoms, ceintures, catégories, statuts de cotisation, dates de naissance et coordonnées.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    Button(
                        onClick = {
                            val label = if (selectedCategoryFilter == "ALL") "Tous les athlètes" else "Groupe $selectedCategoryFilter"
                            CsvExporter.shareMembersReport(context, filteredMembers, label)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exporter et Partager le Registre")
                    }
                }
            }
        }

        // Export 2: Registre des Présences
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = JudoRed, modifier = Modifier.size(28.dp))
                            Column {
                                Text(
                                    text = "Rapport de Présences (CSV)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${filteredAttendances.size} pointages • ${selectedPeriodFilter.frenchLabel}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Historique complet des séances avec horodatages, dates, athlètes présents et catégories.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    Button(
                        onClick = {
                            val label = "${selectedPeriodFilter.frenchLabel} - ${if (selectedCategoryFilter == "ALL") "Toutes catégories" else selectedCategoryFilter}"
                            CsvExporter.shareAttendanceReport(context, filteredAttendances, label)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JudoRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exporter et Partager les Présences")
                    }
                }
            }
        }
    }
}
