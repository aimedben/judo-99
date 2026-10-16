package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.Belt
import com.example.data.model.CoachProfile
import com.example.data.model.JudoGroup
import com.example.data.model.Member
import com.example.ui.theme.JudoGold
import com.example.ui.theme.JudoGoldLight
import com.example.ui.theme.JudoRed
import com.example.ui.theme.JudoSuccess
import com.example.util.CardDataParser
import com.example.util.ExtractedMemberInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMemberDialog(
    initialMember: Member?,
    initialExtractedInfo: ExtractedMemberInfo? = null,
    coachProfile: CoachProfile = CoachProfile(),
    onRequestScanCard: (() -> Unit)? = null,
    onEditCoachProfile: (() -> Unit)? = null,
    onSave: (Member) -> Unit,
    onDismiss: () -> Unit
) {
    var firstName by remember {
        mutableStateOf(initialMember?.firstName ?: initialExtractedInfo?.firstName ?: "")
    }
    var lastName by remember {
        mutableStateOf(initialMember?.lastName ?: initialExtractedInfo?.lastName ?: "")
    }
    var birthDate by remember {
        mutableStateOf(initialMember?.birthDate ?: initialExtractedInfo?.birthDate?.ifBlank { null } ?: "")
    }
    var phone by remember {
        mutableStateOf(initialMember?.phone ?: initialExtractedInfo?.phone ?: "")
    }
    var notes by remember {
        mutableStateOf(initialMember?.notes ?: initialExtractedInfo?.notes ?: "")
    }
    var qrCode by remember {
        mutableStateOf(
            initialMember?.qrCode ?: initialExtractedInfo?.qrCode?.ifBlank { null }
                ?: "JCS-${System.currentTimeMillis().toString().takeLast(4)}"
        )
    }
    var season by remember {
        mutableStateOf(initialExtractedInfo?.season?.ifBlank { null } ?: "2026/2027")
    }
    var photoUrl by remember {
        mutableStateOf(initialMember?.photoUrl ?: initialExtractedInfo?.photoUrl ?: "")
    }
    var status by remember {
        mutableStateOf(initialMember?.status ?: "ACTIF")
    }

    var selectedGroup by remember {
        mutableStateOf(
            initialMember?.group ?: initialExtractedInfo?.group
                ?: if (birthDate.isNotBlank()) JudoGroup.fromBirthDate(birthDate) else JudoGroup.MINIMES
        )
    }
    var isGroupExpanded by remember { mutableStateOf(false) }

    var selectedBelt by remember {
        mutableStateOf(initialMember?.belt ?: initialExtractedInfo?.belt ?: Belt.BLANCHE)
    }
    var isBeltExpanded by remember { mutableStateOf(false) }

    var gender by remember {
        mutableStateOf(
            initialExtractedInfo?.gender ?: run {
                val n = initialMember?.notes ?: ""
                if (n.contains("Sexe: M") || n.contains("Sexe: Masculin")) "M"
                else if (n.contains("Sexe: F") || n.contains("Sexe: Féminin")) "F"
                else ""
            }
        )
    }

    var isFromCardScan by remember {
        mutableStateOf(initialExtractedInfo != null)
    }
    var lastExtractedCardNumber by remember {
        mutableStateOf(initialExtractedInfo?.cardNumber ?: "")
    }
    var isRealCardScanDialogOpen by remember { mutableStateOf(false) }

    var errorMsg by remember { mutableStateOf("") }

    // Android zero-permission Photo Picker for Adherent
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUrl = uri.toString()
        }
    }

    fun applyExtracted(info: ExtractedMemberInfo) {
        if (info.firstName.isNotBlank()) firstName = info.firstName
        if (info.lastName.isNotBlank()) lastName = info.lastName
        if (info.birthDate.isNotBlank()) {
            birthDate = info.birthDate
            selectedGroup = JudoGroup.fromBirthDate(info.birthDate)
        } else if (info.group != null) {
            selectedGroup = info.group
        }
        if (info.gender.isNotBlank()) gender = info.gender
        if (info.phone.isNotBlank()) phone = info.phone
        if (info.qrCode.isNotBlank()) qrCode = info.qrCode
        if (info.season.isNotBlank()) season = info.season
        if (info.notes.isNotBlank()) notes = info.notes
        if (info.photoUrl.isNotBlank()) photoUrl = info.photoUrl
        if (info.cardNumber.isNotBlank()) lastExtractedCardNumber = info.cardNumber
        if (info.belt != Belt.BLANCHE) {
            selectedBelt = info.belt
        }
        isFromCardScan = true
    }

    // Dynamic age calculation
    val calculatedAge = remember(birthDate) {
        try {
            if (birthDate.length >= 4) {
                val year = birthDate.take(4).toInt()
                (2026 - year).coerceAtLeast(0)
            } else null
        } catch (_: Exception) {
            null
        }
    }

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
                .widthIn(max = 680.dp)
                .fillMaxWidth(0.95f)
                .imePadding()
                .padding(vertical = 16.dp)
                .testTag("add_edit_member_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Bar with Club branding & Title
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
                                .background(JudoRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "柔",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (initialMember == null) "Nouvelle Inscription" else "Modifier Judoka",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black
                                    )
                                )
                            }
                            Text(
                                text = "JUDO CLUB SEDDOUK • Saison $season",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_member_dialog_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                // Smart Detection Banner if from QR Scan
                if (isFromCardScan) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = JudoGold.copy(alpha = 0.15f),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = JudoGoldLight,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Données détectées automatiquement depuis le QR Code",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = JudoGoldLight
                                    )
                                )
                                Text(
                                    text = if (lastExtractedCardNumber.isNotBlank())
                                        "Carte N° $lastExtractedCardNumber • Tous les champs ont été remplis"
                                    else "Champs pré-remplis avec succès",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                // Sports Card Scan / Simulation Quick Action Bar
                if (initialMember == null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = null,
                                    tint = JudoRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Extraction automatique de carte sportive",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { isRealCardScanDialogOpen = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = JudoRed),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("scan_real_seddouk_card_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DocumentScanner,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Scanner la carte (Caméra / Galerie)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                if (onRequestScanCard != null) {
                                    OutlinedButton(
                                        onClick = onRequestScanCard,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(48.dp).testTag("dialog_scan_card_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("QR seul", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // SECTION 1: PHOTO DE PROFIL DE L'ADHÉRENT (OPTIONNEL)
                // ==========================================
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = JudoRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Photo de profil de l'adhérent",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Non obligatoire (Optionnel)",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        // Avatar Frame with Belt border
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(3.dp, selectedBelt.primaryColor, CircleShape)
                        ) {
                            if (photoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "Photo de l'adhérent",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "Aucune",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }

                        // Action buttons for Photo Picker
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("adherent_pick_photo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (photoUrl.isBlank()) "Ajouter une photo" else "Changer photo",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            if (photoUrl.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { photoUrl = "" },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Retirer", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // SECTION 2: IDENTITÉ DE L'ADHÉRENT
                // ==========================================
                Text(
                    text = "1. Identité Civile",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = JudoRed)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = {
                            firstName = it
                            if (errorMsg.isNotBlank()) errorMsg = ""
                        },
                        label = { Text("Prénom *") },
                        placeholder = { Text("ex: Babi") },
                        isError = errorMsg.isNotBlank() && firstName.isBlank(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_first_name")
                    )

                    OutlinedTextField(
                        value = lastName,
                        onValueChange = {
                            lastName = it
                            if (errorMsg.isNotBlank()) errorMsg = ""
                        },
                        label = { Text("Nom *") },
                        placeholder = { Text("ex: BOURENANE") },
                        isError = errorMsg.isNotBlank() && lastName.isBlank(),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_last_name")
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = birthDate,
                        onValueChange = {
                            birthDate = it
                            if (errorMsg.isNotBlank()) errorMsg = ""
                            // Dynamically update recommended group based on new birth year
                            selectedGroup = JudoGroup.fromBirthDate(it)
                        },
                        label = { Text("Date de Naissance (AAAA-MM-JJ) *") },
                        placeholder = { Text("2013-01-06") },
                        isError = errorMsg.isNotBlank() && birthDate.isBlank(),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Cake, contentDescription = null)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_birth_date")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (calculatedAge != null) "🎂 Âge calculé: $calculatedAge ans" else "🎂 Format: AAAA-MM-JJ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Catégorie suggérée: ${selectedGroup.label}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = JudoRed,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Sexe / Genre selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sexe :",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                    FilterChip(
                        selected = gender == "M",
                        onClick = { gender = if (gender == "M") "" else "M" },
                        label = { Text("Masculin (M)") },
                        leadingIcon = if (gender == "M") {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("member_gender_m")
                    )
                    FilterChip(
                        selected = gender == "F",
                        onClick = { gender = if (gender == "F") "" else "F" },
                        label = { Text("Féminin (F)") },
                        leadingIcon = if (gender == "F") {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("member_gender_f")
                    )
                }

                // ==========================================
                // SECTION 3: SECTION SPORTIVE & GRADES JUDO
                // ==========================================
                Text(
                    text = "2. Catégorie & Grade Martial",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = JudoRed)
                )

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = isGroupExpanded,
                    onExpandedChange = { isGroupExpanded = !isGroupExpanded }
                ) {
                    OutlinedTextField(
                        value = "${selectedGroup.label} (${selectedGroup.ageRange})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Catégorie / Tranche d'Âge") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.SportsMartialArts, contentDescription = null)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGroupExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isGroupExpanded,
                        onDismissRequest = { isGroupExpanded = false }
                    ) {
                        JudoGroup.entries.forEach { group ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = group.label, fontWeight = FontWeight.Bold)
                                        Text(text = "(${group.ageRange})", style = MaterialTheme.typography.bodySmall)
                                    }
                                },
                                onClick = {
                                    selectedGroup = group
                                    isGroupExpanded = false
                                }
                            )
                        }
                    }
                }

                // Belt Dropdown with Belt Colors
                ExposedDropdownMenuBox(
                    expanded = isBeltExpanded,
                    onExpandedChange = { isBeltExpanded = !isBeltExpanded }
                ) {
                    OutlinedTextField(
                        value = "🥋 ${selectedBelt.displayName}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ceinture / Grade") },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(selectedBelt.primaryColor)
                                    .border(1.dp, Color.Gray, CircleShape)
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isBeltExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isBeltExpanded,
                        onDismissRequest = { isBeltExpanded = false }
                    ) {
                        Belt.entries.forEach { belt ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(belt.primaryColor)
                                                .border(1.dp, Color.Gray, CircleShape)
                                        )
                                        Text(text = belt.displayName, fontWeight = FontWeight.SemiBold)
                                    }
                                },
                                onClick = {
                                    selectedBelt = belt
                                    isBeltExpanded = false
                                }
                            )
                        }
                    }
                }

                // ==========================================
                // SECTION 4: COORDONNÉES & TUTEUR LÉGAL
                // ==========================================
                Text(
                    text = "3. Contact & Tuteur Légal",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = JudoRed)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        if (errorMsg.isNotBlank()) errorMsg = ""
                    },
                    label = { Text("Téléphone Tuteur / Parent *") },
                    placeholder = { Text("0782487120") },
                    isError = errorMsg.isNotBlank() && phone.isBlank(),
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_phone")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes médicales & Observations") },
                    placeholder = { Text("Certificat médical, observations...") },
                    modifier = Modifier.fillMaxWidth()
                )

                // ==========================================
                // SECTION 5: LICENCE & IDENTIFIANT QR
                // ==========================================
                Text(
                    text = "4. Licence & Badge QR",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = JudoRed)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = qrCode,
                        onValueChange = { qrCode = it },
                        label = { Text("Identifiant Carte / QR *") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.QrCode, contentDescription = null)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("input_qr_code")
                    )

                    OutlinedTextField(
                        value = season,
                        onValueChange = { season = it },
                        label = { Text("Saison") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Status Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Statut :",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    FilterChip(
                        selected = status == "ACTIF",
                        onClick = { status = "ACTIF" },
                        label = { Text("Actif (Inscrit)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = JudoSuccess.copy(alpha = 0.2f),
                            selectedLabelColor = JudoSuccess
                        )
                    )
                    FilterChip(
                        selected = status == "SUSPENDU",
                        onClick = { status = "SUSPENDU" },
                        label = { Text("Suspendu") }
                    )
                }

                // ==========================================
                // SECTION 6: COACH RÉFÉRENT & SUPERVISEUR
                // ==========================================
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = CardDefaults.outlinedCardBorder()
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
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF212121))
                                    .border(2.dp, JudoGold, CircleShape)
                            ) {
                                if (coachProfile.photoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = coachProfile.photoUrl,
                                        contentDescription = "Coach",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.SportsMartialArts,
                                        contentDescription = null,
                                        tint = JudoGold,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Coach Référent: ${coachProfile.name}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${coachProfile.danGrade} • ${coachProfile.clubName}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        if (onEditCoachProfile != null) {
                            IconButton(
                                onClick = onEditCoachProfile,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Modifier profil coach",
                                    tint = JudoRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (errorMsg.isNotBlank()) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("Annuler")
                    }

                    Button(
                        onClick = {
                            if (firstName.isBlank() || lastName.isBlank()) {
                                errorMsg = "Veuillez renseigner le prénom et le nom de l'adhérent."
                                return@Button
                            }
                            if (birthDate.isBlank()) {
                                errorMsg = "Veuillez renseigner la date de naissance (format AAAA-MM-JJ)."
                                return@Button
                            }
                            if (phone.isBlank()) {
                                errorMsg = "Veuillez renseigner le numéro de téléphone du tuteur/parent."
                                return@Button
                            }
                            val cleanBirthDate = birthDate.trim()
                            val cleanPhone = phone.trim()
                            val cleanQr = qrCode.trim().ifBlank { "JCS-${System.currentTimeMillis().toString().takeLast(4)}" }

                            val genderTag = if (gender.isNotBlank()) "Sexe: ${if (gender == "M") "Masculin" else "Féminin"}" else ""
                            val finalNotes = if (genderTag.isNotBlank()) {
                                val clean = notes.replace(Regex("""(?i)Sexe:\s*(?:Masculin|Féminin|M|F)\s*[•\-]?"""), "").trim()
                                if (clean.isBlank()) genderTag else "$genderTag • $clean"
                            } else notes.trim()

                            val toSave = initialMember?.copy(
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                birthDate = cleanBirthDate,
                                phone = cleanPhone,
                                groupCode = selectedGroup.code,
                                beltName = selectedBelt.name,
                                qrCode = cleanQr,
                                status = status,
                                photoUrl = photoUrl.trim(),
                                notes = finalNotes
                            ) ?: Member(
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                birthDate = cleanBirthDate,
                                phone = cleanPhone,
                                groupCode = selectedGroup.code,
                                beltName = selectedBelt.name,
                                qrCode = cleanQr,
                                status = status,
                                registrationDate = "2026-09-01",
                                photoUrl = photoUrl.trim(),
                                notes = finalNotes
                            )
                            onSave(toSave)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JudoRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.8f)
                            .height(50.dp)
                            .testTag("submit_member_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (initialMember == null) "Valider & Enregistrer" else "Enregistrer Modifications",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }

    if (isRealCardScanDialogOpen) {
        SportCardScanDialog(
            onExtracted = { info ->
                applyExtracted(info)
                isRealCardScanDialogOpen = false
            },
            onDismiss = { isRealCardScanDialogOpen = false }
        )
    }
}
