package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminNotification
import com.example.data.model.AdminProfile
import com.example.data.model.ClubSettings
import com.example.ui.theme.JudoGold
import com.example.ui.theme.JudoRed

@Composable
fun AdminHeaderBar(
    clubSettings: ClubSettings,
    adminProfile: AdminProfile,
    notifications: List<AdminNotification> = emptyList(),
    isDarkTheme: Boolean = false,
    pendingSyncCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenCloudSync: () -> Unit = {},
    onOpenProfileDialog: () -> Unit,
    onSwitchToCoach: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var profileMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_header_bar"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Club Logo & Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_judo_launcher),
                    contentDescription = "Emblème Judo Seddouk",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(1.dp, JudoGold, CircleShape),
                    contentScale = ContentScale.Crop
                )

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = clubSettings.clubName.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    Text(
                        text = "Espace Administrateur",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = JudoGold,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }

            // Right: Actions (Tatami, Settings, Admin Profile Menu)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Minimized Tatami (Coach Space) Button
                IconButton(
                    onClick = onSwitchToCoach,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(JudoRed.copy(alpha = 0.12f))
                        .border(1.dp, JudoRed.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .testTag("admin_switch_coach_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsMartialArts,
                        contentDescription = "Accès Tatami (Entraîneur)",
                        tint = JudoRed,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Admin Profile Icon & Menu
                Box {
                    IconButton(
                        onClick = { profileMenuExpanded = true },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("admin_profile_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(JudoGold.copy(alpha = 0.2f))
                                .border(1.5.dp, JudoGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!adminProfile.photoUri.isNullOrBlank()) {
                                coil.compose.AsyncImage(
                                    model = adminProfile.photoUri,
                                    contentDescription = "Photo profil administrateur",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Profil Administrateur",
                                    tint = JudoGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = profileMenuExpanded,
                        onDismissRequest = { profileMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = adminProfile.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = adminProfile.role,
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            },
                            onClick = {
                                profileMenuExpanded = false
                                onOpenProfileDialog()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Modifier mes informations") },
                            leadingIcon = {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                profileMenuExpanded = false
                                onOpenProfileDialog()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Paramètres du club") },
                            leadingIcon = {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                profileMenuExpanded = false
                                onOpenSettings()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Supabase Cloud & Sauvegardes") },
                            leadingIcon = {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = JudoGold, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                profileMenuExpanded = false
                                onOpenCloudSync()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Espace Coach (Tatami)") },
                            leadingIcon = {
                                Icon(Icons.Default.SportsMartialArts, contentDescription = null, tint = JudoRed, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                profileMenuExpanded = false
                                onSwitchToCoach()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Quitter l'application",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                profileMenuExpanded = false
                                onLogout()
                            }
                        )
                    }
                }
            }
        }
    }
}
