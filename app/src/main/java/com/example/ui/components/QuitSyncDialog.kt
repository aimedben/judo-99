package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.sync.SupabaseConfig
import com.example.data.sync.SyncReport
import com.example.ui.theme.JudoGold
import com.example.ui.theme.JudoRed
import com.example.ui.theme.JudoSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Boîte de dialogue de confirmation et de synchronisation avant de quitter l'application Judo Club Seddouk.
 * Propose trois options :
 * 1. Synchroniser et quitter (attend la confirmation réelle avant fermeture).
 * 2. Continuer l'application (revient à l'écran sans rien modifier).
 * 3. Quitter sans synchroniser (avertit clairement l'utilisateur des risques de non-sauvegarde).
 */
@Composable
fun QuitSyncDialog(
    pendingCount: Int,
    supabaseConfig: SupabaseConfig,
    isOnline: Boolean,
    isSyncing: Boolean,
    lastSyncReport: SyncReport?,
    onPerformSync: ((SyncReport) -> Unit) -> Unit,
    onQuitWithoutSync: () -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var showWarningForQuitWithoutSync by remember { mutableStateOf(false) }
    var syncErrorMessage by remember { mutableStateOf<String?>(null) }
    var syncSuccessMessage by remember { mutableStateOf<String?>(null) }
    var localIsSyncing by remember { mutableStateOf(false) }

    val effectivelySyncing = isSyncing || localIsSyncing

    val lastSyncFormatted = if (supabaseConfig.lastSyncTimestamp > 0) {
        val sdf = SimpleDateFormat("dd/MM/yyyy 'à' HH:mm", Locale.FRANCE)
        sdf.format(Date(supabaseConfig.lastSyncTimestamp))
    } else {
        "Aucune synchronisation antérieure"
    }

    // Sous-fenêtre de confirmation obligatoire avant de quitter sans synchroniser
    if (showWarningForQuitWithoutSync) {
        AlertDialog(
            onDismissRequest = { showWarningForQuitWithoutSync = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Avertissement",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(30.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Quitter sans synchroniser ?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (pendingCount > 0) {
                            "Attention : $pendingCount modification(s) sont enregistrées uniquement sur cet appareil.\n\nSi vous quittez maintenant sans synchroniser, les dernières modifications risquent de ne pas être sauvegardées ailleurs (sur le Cloud) tant qu'une synchronisation n'aura pas été effectuée."
                        } else {
                            "Aucune modification n'est en attente. Vos données locales sont déjà à jour avec le Cloud."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Vos données locales restent en sécurité sur cette tablette.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWarningForQuitWithoutSync = false
                        onQuitWithoutSync()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_quit_without_sync_btn")
                ) {
                    Text("Confirmer et quitter", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showWarningForQuitWithoutSync = false },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("cancel_warning_btn")
                ) {
                    Text("Annuler")
                }
            }
        )
        return
    }

    // Fonction de déclenchement de la synchronisation avant fermeture
    fun startSyncAndQuit() {
        if (!isOnline) {
            syncErrorMessage = "Aucune connexion Internet active sur la tablette. Vos données sont conservées localement."
            return
        }
        if (!supabaseConfig.isConfigured) {
            syncErrorMessage = "Supabase n'est pas configuré. Veuillez vérifier les paramètres cloud."
            return
        }

        syncErrorMessage = null
        syncSuccessMessage = null
        localIsSyncing = true

        onPerformSync { report ->
            localIsSyncing = false
            if (report.isSuccess) {
                syncSuccessMessage = "Synchronisation réussie (${report.itemsSent} éléments envoyés). Fermeture en cours..."
                coroutineScope.launch {
                    delay(800L)
                    onQuitWithoutSync()
                }
            } else {
                syncErrorMessage = report.errorMessage ?: "Erreur lors de la synchronisation avec le serveur."
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!effectivelySyncing) onDismiss()
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            syncSuccessMessage != null -> JudoSuccess.copy(alpha = 0.15f)
                            syncErrorMessage != null -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            pendingCount > 0 -> JudoGold.copy(alpha = 0.15f)
                            else -> JudoSuccess.copy(alpha = 0.15f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        syncSuccessMessage != null -> Icons.Default.CheckCircle
                        syncErrorMessage != null -> Icons.Default.ErrorOutline
                        pendingCount > 0 -> Icons.Default.CloudSync
                        else -> Icons.Default.CloudDone
                    },
                    contentDescription = null,
                    tint = when {
                        syncSuccessMessage != null -> JudoSuccess
                        syncErrorMessage != null -> MaterialTheme.colorScheme.error
                        pendingCount > 0 -> JudoGold
                        else -> JudoSuccess
                    },
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "Quitter l'application",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Message principal requis
                Text(
                    text = "Voulez-vous synchroniser les données avant de quitter ?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // État de connectivité (En ligne / Hors-ligne)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isOnline) JudoSuccess.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (isOnline) JudoSuccess else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isOnline) "Connexion Internet active" else "Mode Hors-ligne (Tatami)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) JudoSuccess else MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }

                // Carte récapitulative des données locales
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Modifications en attente :",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (pendingCount == 0) JudoSuccess.copy(alpha = 0.15f) else JudoGold.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (pendingCount == 0) "À jour (0)" else "$pendingCount opération(s)",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (pendingCount == 0) JudoSuccess else JudoGold
                                    )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Dernière synchro cloud :",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = lastSyncFormatted,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }

                // Animation pendant la synchronisation
                AnimatedVisibility(visible = effectivelySyncing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(JudoGold.copy(alpha = 0.1f))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = JudoGold,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Synchronisation avec Supabase en cours...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Ne quittez pas l'application pendant le transfert.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                // Message de succès avec confirmation visuelle
                if (syncSuccessMessage != null && !effectivelySyncing) {
                    Surface(
                        color = JudoSuccess.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, JudoSuccess.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = JudoSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = syncSuccessMessage!!,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = JudoSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                // Message d'erreur clair si la synchronisation a échoué
                if (syncErrorMessage != null && !effectivelySyncing) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Échec de la synchronisation",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                )
                            }
                            Text(
                                text = syncErrorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Vos données locales restent intactes et enregistrées sur la tablette.",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // Les trois boutons d'action demandés
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Bouton "Synchroniser et quitter" (ou "Réessayer" si échec précédent)
                    if (syncErrorMessage != null) {
                        Button(
                            onClick = { startSyncAndQuit() },
                            enabled = !effectivelySyncing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JudoGold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("retry_sync_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Réessayer la synchronisation", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { startSyncAndQuit() },
                            enabled = !effectivelySyncing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JudoSuccess,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("sync_and_quit_btn")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (pendingCount > 0) "Synchroniser et quitter" else "Vérifier et quitter",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 2. Bouton "Continuer l'application"
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !effectivelySyncing,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("continue_app_btn")
                    ) {
                        Text("Continuer l'application", fontWeight = FontWeight.SemiBold)
                    }

                    // 3. Bouton "Quitter sans synchroniser" (avec avertissement si données en attente)
                    TextButton(
                        onClick = {
                            if (pendingCount > 0) {
                                showWarningForQuitWithoutSync = true
                            } else {
                                onQuitWithoutSync()
                            }
                        },
                        enabled = !effectivelySyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("quit_without_sync_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (pendingCount > 0) "Quitter sans synchroniser" else "Quitter directement",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
