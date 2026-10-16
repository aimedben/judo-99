package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import com.example.ui.screens.AdminCloudSyncScreen
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ActiveTrainingSession
import com.example.data.model.AdminNotification
import com.example.data.model.AdminProfile
import com.example.data.model.AdminSubScreen
import com.example.data.model.ClubSettings
import com.example.data.model.CoachProfile
import com.example.data.model.CoachTab
import com.example.data.model.CoachUser
import com.example.data.model.JudoCategory
import com.example.data.model.Member
import com.example.data.model.PastTrainingSession
import com.example.data.model.UserRole
import com.example.data.repository.CheckInResult
import com.example.ui.MainViewModel
import com.example.ui.ThemePreference
import com.example.ui.components.AddEditMemberDialog
import com.example.ui.components.AdminPinDialog
import com.example.ui.components.AlreadyCheckedInDialog
import com.example.ui.components.AssociateCardDialog
import com.example.ui.components.CheckInConfirmationDialog
import com.example.ui.components.CoachProfileDialog
import com.example.ui.screens.AdminAttendanceOverviewScreen
import com.example.ui.screens.AdminCategoriesScreen
import com.example.ui.screens.AdminCoachesScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AdminExportsScreen
import com.example.ui.screens.AdminProfileEditDialog
import com.example.ui.screens.AdminSessionsScreen
import com.example.ui.screens.AdminSettingsScreen
import com.example.ui.screens.CategorySelectionScreen
import com.example.ui.screens.CoachAthletesScreen
import com.example.ui.screens.CoachHistoryScreen
import com.example.ui.screens.CoachMinimalHomeScreen
import com.example.ui.screens.CoachProfileScreen
import com.example.ui.screens.CoachDedicatedScannerScreen
import com.example.ui.screens.CoachSessionPointageScreen
import com.example.ui.screens.CoachSessionPreparationScreen
import com.example.ui.screens.CoachSessionSummaryScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MemberDetailScreen
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.NotificationsDialog
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.ScannerTargetMode
import com.example.ui.screens.StatFilterMode
import com.example.ui.screens.StatFilteredMembersScreen
import com.example.ui.screens.StatPeriod
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.SportsMartialArts
import com.example.ui.theme.JudoRed
import com.example.ui.theme.MyApplicationTheme

enum class ScreenTab(val label: String, val icon: ImageVector) {
    HOME("Accueil", Icons.Default.Dashboard),
    MEMBERS("Athlètes", Icons.Default.People),
    COACHES("Coachs", Icons.Default.SportsMartialArts),
    CATEGORIES("Catégories", Icons.Default.Category),
    SESSIONS("Séances", Icons.Default.EventNote)
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val douchetteBuffer = StringBuilder()
    private var lastDouchetteKeyTime = 0L

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        // Intercept hardware keys from Bluetooth HID or USB OTG douchette (barcode scanner)
        // This processes scans immediately without opening any soft keyboard or requiring text fields!
        if (event.action == android.view.KeyEvent.ACTION_DOWN) {
            val hasActiveSession = viewModel.activeSession.value != null
            if (hasActiveSession) {
                val now = System.currentTimeMillis()
                // Reset buffer if more than 1.5 seconds elapsed between scans
                if (now - lastDouchetteKeyTime > 1500L) {
                    douchetteBuffer.setLength(0)
                }
                lastDouchetteKeyTime = now

                if (event.keyCode == android.view.KeyEvent.KEYCODE_ENTER ||
                    event.keyCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER
                ) {
                    val code = douchetteBuffer.toString().trim()
                    douchetteBuffer.setLength(0)
                    if (code.isNotBlank()) {
                        viewModel.onQrScanned(code)
                        return true
                    }
                } else {
                    val unicode = event.getUnicodeChar(event.metaState)
                    if (unicode != 0 && !Character.isISOControl(unicode)) {
                        douchetteBuffer.append(unicode.toChar())
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onStop() {
        super.onStop()
        // Synchronisation directe en arrière-plan vers la base de données
        if (viewModel.supabaseConfig.value.isConfigured && viewModel.isOnline()) {
            viewModel.performFullSync()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePref by viewModel.themePreference.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themePref) {
                ThemePreference.DARK -> true
                ThemePreference.LIGHT -> false
                ThemePreference.SYSTEM -> systemDark
            }

            MyApplicationTheme(darkTheme = isDark) {
                JudoApp(viewModel = viewModel, isDarkTheme = isDark)
            }
        }
    }
}

@Composable
fun JudoApp(viewModel: MainViewModel, isDarkTheme: Boolean) {
    var currentTab by remember { mutableStateOf(ScreenTab.HOME) }
    var selectedMemberId by remember { mutableStateOf<Long?>(null) }
    var activeStatFilter by remember { mutableStateOf<StatFilterMode?>(null) }
    var activeStatPeriod by remember { mutableStateOf(StatPeriod.TODAY) }
    var dashboardPeriod by remember { mutableStateOf(StatPeriod.TODAY) }
    var memberToEdit by remember { mutableStateOf<Member?>(null) }
    var isAddAthleteDialogOpen by remember { mutableStateOf(false) }
    var scannerMode by remember { mutableStateOf(ScannerTargetMode.POINTAGE) }
    var isCoachProfileDialogOpen by remember { mutableStateOf(false) }

    // Admin State & SubScreens
    var activeAdminSubScreen by remember { mutableStateOf(AdminSubScreen.NONE) }
    var isNotificationsDialogOpen by remember { mutableStateOf(false) }
    var isAdminProfileDialogOpen by remember { mutableStateOf(false) }
    var isAdminPinDialogOpen by remember { mutableStateOf(false) }

    val categories by viewModel.categories.collectAsState()
    val coaches by viewModel.coaches.collectAsState()
    val clubSettings by viewModel.clubSettings.collectAsState()
    val adminProfile by viewModel.adminProfile.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val themePreference by viewModel.themePreference.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val coachScanFeedback by viewModel.coachScanFeedback.collectAsState()
    val pastSessions by viewModel.pastSessions.collectAsState()

    var isPreparingSession by remember { mutableStateOf(false) }
    var isSessionSummaryShown by remember { mutableStateOf(false) }
    var currentCoachTab by remember { mutableStateOf(CoachTab.SESSION) }
    var isCoachCameraScannerOpen by remember { mutableStateOf(false) }

    val coachProfile by viewModel.coachProfile.collectAsState()
    val allMembers by viewModel.allMembers.collectAsState()
    val todayAttendance by viewModel.todayAttendance.collectAsState()
    val recentAttendance by viewModel.recentAttendance.collectAsState()
    val allAttendance by viewModel.allAttendance.collectAsState()
    val lastScanned by viewModel.lastScanned.collectAsState()
    val activeCheckInResult by viewModel.activeCheckInResult.collectAsState()
    val unassignedQrCode by viewModel.unassignedQrCode.collectAsState()
    val scannedMemberForCreation by viewModel.scannedMemberForCreation.collectAsState()
    val scheduledSessions by viewModel.scheduledSessions.collectAsStateWithLifecycle()

    val supabaseConfig by viewModel.supabaseConfig.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val lastSyncReport by viewModel.lastSyncReport.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isDirectSyncingBeforeExit by remember { mutableStateOf(false) }

    fun quitAppWithDirectSync() {
        if (isDirectSyncingBeforeExit) return
        val activity = context as? Activity
        if (supabaseConfig.isConfigured && viewModel.isOnline()) {
            isDirectSyncingBeforeExit = true
            coroutineScope.launch {
                try {
                    withTimeoutOrNull(3000L) {
                        suspendCancellableCoroutine { continuation ->
                            viewModel.performFullSync {
                                if (continuation.isActive) continuation.resume(Unit)
                            }
                        }
                    }
                } catch (_: Exception) {}
                isDirectSyncingBeforeExit = false
                activity?.finish()
            }
        } else {
            activity?.finish()
        }
    }

    val handleLogoutWithSync = {
        if (supabaseConfig.isConfigured && (pendingSyncCount > 0 || viewModel.isOnline())) {
            viewModel.performFullSync()
        }
        viewModel.selectCategory(null)
        viewModel.setUserRole(UserRole.COACH)
    }

    val topStats = remember(allMembers, allAttendance, dashboardPeriod) {
        viewModel.getTopAttendanceList(allMembers, allAttendance, dashboardPeriod)
    }

    // Filter members for active coach session category
    val categoryMembers = remember(allMembers, selectedCategory) {
        if (selectedCategory == null) allMembers
        else allMembers.filter { it.groupCode.equals(selectedCategory!!.groupCode, ignoreCase = true) }
    }

    val sessionAttendances = remember(todayAttendance, selectedCategory) {
        if (selectedCategory == null) todayAttendance
        else todayAttendance.filter { it.groupCode.equals(selectedCategory!!.groupCode, ignoreCase = true) }
    }

    // Back handling priority
    if (isDirectSyncingBeforeExit) {
        BackHandler {
            // Empêche toute interruption pendant la synchronisation directe avant fermeture
        }
    } else if (userRole == UserRole.COACH && selectedCategory != null) {
        if (isSessionSummaryShown) {
            BackHandler {
                isSessionSummaryShown = false
                viewModel.endSession()
            }
        } else if (isCoachCameraScannerOpen) {
            BackHandler {
                isCoachCameraScannerOpen = false
            }
        } else if (isPreparingSession) {
            BackHandler {
                isPreparingSession = false
            }
        } else if (currentCoachTab != CoachTab.SESSION) {
            BackHandler {
                currentCoachTab = CoachTab.SESSION
            }
        } else if (activeSession == null) {
            BackHandler {
                viewModel.selectCategory(null)
            }
        } else {
            BackHandler {
                quitAppWithDirectSync()
            }
        }
    } else if (activeAdminSubScreen != AdminSubScreen.NONE) {
        BackHandler {
            activeAdminSubScreen = AdminSubScreen.NONE
        }
    } else if (selectedMemberId != null) {
        BackHandler {
            selectedMemberId = null
        }
    } else if (activeStatFilter != null) {
        BackHandler {
            activeStatFilter = null
        }
    } else if (currentTab != ScreenTab.HOME) {
        BackHandler {
            currentTab = ScreenTab.HOME
        }
    } else {
        // Intercept back key at root screen (Welcome / Category selection or Admin Home)
        BackHandler {
            quitAppWithDirectSync()
        }
    }

    // Synchronisation directe vers la BDD lors de la fermeture sans fenêtre de confirmation
    if (isDirectSyncingBeforeExit) {
        Dialog(
            onDismissRequest = { /* Non annulable */ },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = JudoRed)
                    Text(
                        text = "Synchronisation vers la base de données...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Fermeture automatique de l'application...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // 1. Welcome Screen: Category Selection (when in COACH mode and no category selected yet)
    if (userRole == UserRole.COACH && selectedCategory == null) {
        CategorySelectionScreen(
            categories = categories,
            onSelectCategory = { category ->
                viewModel.selectCategory(category)
                currentCoachTab = CoachTab.SESSION
            },
            onAdminAccess = {
                isAdminPinDialogOpen = true
            },
            onQuitApp = {
                quitAppWithDirectSync()
            },
            isDarkTheme = isDarkTheme,
            onToggleTheme = { viewModel.toggleTheme() }
        )

        if (isAdminPinDialogOpen) {
            AdminPinDialog(
                currentPin = adminProfile.pinCode,
                onPinValidated = {
                    isAdminPinDialogOpen = false
                    viewModel.setUserRole(UserRole.ADMIN)
                },
                onDismiss = {
                    isAdminPinDialogOpen = false
                }
            )
        }
        return
    }

    // 2. Coach Flow with selected category & dedicated Coach Navbar
    if (userRole == UserRole.COACH && selectedCategory != null) {
        // Dedicated Full-Screen Scanner Page: occupies entire app surface without navbar, navigation rail, stats, or menus
        if (activeSession != null && isCoachCameraScannerOpen) {
            CoachDedicatedScannerScreen(
                session = activeSession!!,
                sessionAttendances = sessionAttendances,
                totalCategoryCount = categoryMembers.size,
                scanFeedback = coachScanFeedback,
                onQrScanned = { code -> viewModel.onQrScanned(code) },
                onBackToSession = { isCoachCameraScannerOpen = false },
                onDismissFeedback = { viewModel.clearCoachScanFeedback() }
            )
            return
        }

        @Composable
        fun CoachScreenBody() {
            when (currentCoachTab) {
                CoachTab.SESSION -> {
                    if (isSessionSummaryShown && activeSession != null) {
                        // Screen 4: Fin de séance - Récapitulatif
                        CoachSessionSummaryScreen(
                            session = activeSession!!,
                            categoryMembers = categoryMembers,
                            sessionAttendances = sessionAttendances,
                            onValidateAndSave = { endTime: String ->
                                viewModel.saveFinishedSession(
                                    session = activeSession!!,
                                    endTime = endTime,
                                    categoryMembers = categoryMembers,
                                    presentAttendances = sessionAttendances
                                )
                                isSessionSummaryShown = false
                            }
                        )
                    } else if (activeSession != null) {
                        // Screen 3: Pointage pendant entraînement (Statistiques de séance)
                        CoachSessionPointageScreen(
                            session = activeSession!!,
                            categoryMembers = categoryMembers,
                            sessionAttendances = sessionAttendances,
                            scanFeedback = coachScanFeedback,
                            onOpenScanner = { isCoachCameraScannerOpen = true },
                            onQrScanned = { code -> viewModel.onQrScanned(code) },
                            onManualCheckInMember = { member -> viewModel.manualCheckIn(member.id) },
                            onFinishSession = { isSessionSummaryShown = true },
                            onDismissFeedback = { viewModel.clearCoachScanFeedback() }
                        )
                    } else if (isPreparingSession) {
                        // Screen 2: Préparation de séance
                        CoachSessionPreparationScreen(
                            category = selectedCategory!!,
                            onBack = { isPreparingSession = false },
                            onStartTraining = { session ->
                                viewModel.startSession(session)
                                isPreparingSession = false
                                isCoachCameraScannerOpen = true // Display dedicated full-screen scan page by default!
                            }
                        )
                    } else {
                        // Screen 1: Minimalist Coach Home with prominent "Nouvelle séance" button
                        CoachMinimalHomeScreen(
                            category = selectedCategory!!,
                            onChangeCategory = { viewModel.selectCategory(null) },
                            onStartNewSession = { isPreparingSession = true }
                        )
                    }
                }
                CoachTab.ATHLETES -> {
                    CoachAthletesScreen(
                        category = selectedCategory!!,
                        members = categoryMembers,
                        todayAttendances = sessionAttendances,
                        onManualCheckIn = { member -> viewModel.manualCheckIn(member.id) }
                    )
                }
                CoachTab.HISTORY -> {
                    CoachHistoryScreen(
                        category = selectedCategory!!,
                        pastSessions = pastSessions
                    )
                }
                CoachTab.PROFILE -> {
                    CoachProfileScreen(
                        category = selectedCategory!!,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { viewModel.toggleTheme() },
                        onChangeCategory = { viewModel.selectCategory(null) }
                    )
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isTablet = maxWidth >= 600.dp
            if (isTablet) {
                // Tablet layout: Coach NavigationRail on left
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("coach_tablet_navigation_rail"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        header = {
                            Text(
                                text = "🥋 COACH",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = JudoRed
                                ),
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    ) {
                        CoachTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = currentCoachTab == tab,
                                onClick = {
                                    isCoachCameraScannerOpen = false
                                    currentCoachTab = tab
                                },
                                icon = {
                                    Icon(
                                        imageVector = when (tab) {
                                            CoachTab.SESSION -> Icons.Default.SportsMartialArts
                                            CoachTab.ATHLETES -> Icons.Default.People
                                            CoachTab.HISTORY -> Icons.Default.History
                                            CoachTab.PROFILE -> Icons.Default.AccountCircle
                                        },
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = JudoRed,
                                    indicatorColor = JudoRed
                                ),
                                modifier = Modifier.testTag("coach_rail_${tab.name.lowercase()}")
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        CoachScreenBody()
                    }
                }
            } else {
                // Phone layout: Coach Bottom Navigation Bar (4 tabs only: Séance, Athlètes, Historique, Profil)
                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            modifier = Modifier.testTag("coach_bottom_navigation_bar")
                        ) {
                            CoachTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentCoachTab == tab,
                                    onClick = {
                                        isCoachCameraScannerOpen = false
                                        currentCoachTab = tab
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = when (tab) {
                                                CoachTab.SESSION -> Icons.Default.SportsMartialArts
                                                CoachTab.ATHLETES -> Icons.Default.People
                                                CoachTab.HISTORY -> Icons.Default.History
                                                CoachTab.PROFILE -> Icons.Default.AccountCircle
                                            },
                                            contentDescription = tab.label,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (currentCoachTab == tab) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        selectedTextColor = JudoRed,
                                        indicatorColor = JudoRed,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("coach_nav_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        CoachScreenBody()
                    }
                }
            }
        }

        // Global CheckIn Confirmation Dialogs
        when (val result = activeCheckInResult) {
            is CheckInResult.Success -> {
                CheckInConfirmationDialog(
                    member = result.member,
                    timeString = result.attendance.timeString,
                    onDismiss = { viewModel.dismissCheckInDialog() },
                    onViewProfile = { _ ->
                        viewModel.dismissCheckInDialog()
                    }
                )
            }
            is CheckInResult.AlreadyCheckedIn -> {
                AlreadyCheckedInDialog(
                    member = result.member,
                    existingTime = result.existingAttendance.timeString,
                    onDismiss = { viewModel.dismissCheckInDialog() }
                )
            }
            else -> Unit
        }
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp

        if (isTablet) {
            // Tablet layout: NavigationRail on left
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .testTag("tablet_navigation_rail"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Text(
                            text = "🥋 JUDO",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = JudoRed
                            ),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                ) {
                    ScreenTab.entries.forEach { tab ->
                        NavigationRailItem(
                            selected = currentTab == tab && selectedMemberId == null && activeStatFilter == null && activeAdminSubScreen == AdminSubScreen.NONE,
                            onClick = {
                                selectedMemberId = null
                                activeStatFilter = null
                                activeAdminSubScreen = AdminSubScreen.NONE
                                currentTab = tab
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = JudoRed,
                                indicatorColor = JudoRed
                            ),
                            modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}")
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Theme toggle on tablet rail
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Changer thème",
                            tint = if (isDarkTheme) Color(0xFFFFD54F) else JudoRed
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    AppScreenContent(
                        currentTab = currentTab,
                        selectedMemberId = selectedMemberId,
                        activeStatFilter = activeStatFilter,
                        activeStatPeriod = activeStatPeriod,
                        dashboardPeriod = dashboardPeriod,
                        activeAdminSubScreen = activeAdminSubScreen,
                        coaches = coaches,
                        categories = categories,
                        clubSettings = clubSettings,
                        adminProfile = adminProfile,
                        pastSessions = pastSessions,
                        notifications = notifications,
                        themePreference = themePreference,
                        allMembers = allMembers,
                        todayAttendance = todayAttendance,
                        recentAttendance = recentAttendance,
                        allAttendance = allAttendance,
                        lastScanned = lastScanned,
                        topStats = topStats,
                        isDarkTheme = isDarkTheme,
                        coachProfile = coachProfile,
                        activeSession = activeSession,
                        onAdminSubScreenChange = { activeAdminSubScreen = it },
                        onDashboardPeriodChange = { dashboardPeriod = it },
                        onActiveStatPeriodChange = { activeStatPeriod = it },
                        onFilterModeChange = { activeStatFilter = it },
                        onEditCoachProfile = { isCoachProfileDialogOpen = true },
                        onToggleTheme = { viewModel.toggleTheme() },
                        onNavigateToTab = { tab ->
                            activeAdminSubScreen = AdminSubScreen.NONE
                            activeStatFilter = null
                            selectedMemberId = null
                            currentTab = tab
                        },
                        onSelectMember = { selectedMemberId = it },
                        onBackFromMemberDetail = { selectedMemberId = null },
                        onStatClick = { filterMode, period ->
                            selectedMemberId = null
                            activeStatFilter = filterMode
                            activeStatPeriod = period
                        },
                        onBackFromStatFilter = { activeStatFilter = null },
                        onEditMember = { memberToEdit = it },
                        onDeleteMember = { viewModel.deleteMember(it) },
                        onSaveMember = { viewModel.saveMember(it) },
                        onDeleteAttendance = { viewModel.deleteAttendance(it) },
                        onManualCheckIn = { viewModel.manualCheckIn(it) },
                        onRecordAttendanceForDate = { id, date -> viewModel.recordAttendanceForDate(id, date) },
                        onDeleteAttendanceForDate = { id, date -> viewModel.deleteAttendanceForDate(id, date) },
                        onSwitchRole = {
                            viewModel.selectCategory(null)
                            viewModel.setUserRole(UserRole.COACH)
                        },
                        onOpenNotifications = { isNotificationsDialogOpen = true },
                        onOpenProfileDialog = { isAdminProfileDialogOpen = true },
                        onAddAthlete = { isAddAthleteDialogOpen = true },
                        onAddCoach = { viewModel.addCoach(it) },
                        onUpdateCoach = { viewModel.updateCoach(it) },
                        onToggleCoachActive = { viewModel.toggleCoachActive(it) },
                        onAddCategory = { viewModel.addCategory(it) },
                        onUpdateCategory = { viewModel.updateCategory(it) },
                        onToggleCategoryActive = { viewModel.toggleCategoryActive(it) },
                        onCorrectAttendance = { sId, mName, pres -> viewModel.correctSessionAttendance(sId, mName, pres) },
                        onSetThemePreference = { viewModel.setThemePreference(it) },
                        onUpdateClubSettings = { viewModel.updateClubSettings(it) },
                        onUpdateAdminProfile = { viewModel.updateAdminProfile(it) },
                        onLogout = {
                            if (supabaseConfig.isConfigured && viewModel.isOnline()) {
                                viewModel.performFullSync()
                            }
                            viewModel.selectCategory(null)
                            viewModel.setUserRole(UserRole.COACH)
                        },
                        scheduledSessions = scheduledSessions,
                        onAddScheduledSession = { s, repeat -> viewModel.addScheduledSession(s, repeat) },
                        onUpdateScheduledSession = { s -> viewModel.updateScheduledSession(s) },
                        onDuplicateScheduledSession = { s -> viewModel.duplicateScheduledSession(s) },
                        onDeleteScheduledSession = { id, scope -> viewModel.deleteScheduledSession(id, scope) },
                        onCheckSessionConflicts = { s, excludeId -> viewModel.checkSessionConflicts(s, excludeId) }
                    )
                }
            }
        } else {
            // Phone layout: Bottom Navigation Bar
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        ScreenTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = currentTab == tab && selectedMemberId == null && activeStatFilter == null && activeAdminSubScreen == AdminSubScreen.NONE,
                                onClick = {
                                    selectedMemberId = null
                                    activeStatFilter = null
                                    activeAdminSubScreen = AdminSubScreen.NONE
                                    currentTab = tab
                                },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (currentTab == tab && activeStatFilter == null && activeAdminSubScreen == AdminSubScreen.NONE) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = JudoRed,
                                    indicatorColor = JudoRed,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_bar_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    AppScreenContent(
                        currentTab = currentTab,
                        selectedMemberId = selectedMemberId,
                        activeStatFilter = activeStatFilter,
                        activeStatPeriod = activeStatPeriod,
                        dashboardPeriod = dashboardPeriod,
                        activeAdminSubScreen = activeAdminSubScreen,
                        coaches = coaches,
                        categories = categories,
                        clubSettings = clubSettings,
                        adminProfile = adminProfile,
                        pastSessions = pastSessions,
                        notifications = notifications,
                        themePreference = themePreference,
                        allMembers = allMembers,
                        todayAttendance = todayAttendance,
                        recentAttendance = recentAttendance,
                        allAttendance = allAttendance,
                        lastScanned = lastScanned,
                        topStats = topStats,
                        isDarkTheme = isDarkTheme,
                        coachProfile = coachProfile,
                        activeSession = activeSession,
                        onAdminSubScreenChange = { activeAdminSubScreen = it },
                        onDashboardPeriodChange = { dashboardPeriod = it },
                        onActiveStatPeriodChange = { activeStatPeriod = it },
                        onFilterModeChange = { activeStatFilter = it },
                        onEditCoachProfile = { isCoachProfileDialogOpen = true },
                        onToggleTheme = { viewModel.toggleTheme() },
                        onNavigateToTab = { tab ->
                            activeAdminSubScreen = AdminSubScreen.NONE
                            activeStatFilter = null
                            selectedMemberId = null
                            currentTab = tab
                        },
                        onSelectMember = { selectedMemberId = it },
                        onBackFromMemberDetail = { selectedMemberId = null },
                        onStatClick = { filterMode, period ->
                            selectedMemberId = null
                            activeStatFilter = filterMode
                            activeStatPeriod = period
                        },
                        onBackFromStatFilter = { activeStatFilter = null },
                        onEditMember = { memberToEdit = it },
                        onDeleteMember = { viewModel.deleteMember(it) },
                        onSaveMember = { viewModel.saveMember(it) },
                        onDeleteAttendance = { viewModel.deleteAttendance(it) },
                        onManualCheckIn = { viewModel.manualCheckIn(it) },
                        onRecordAttendanceForDate = { id, date -> viewModel.recordAttendanceForDate(id, date) },
                        onDeleteAttendanceForDate = { id, date -> viewModel.deleteAttendanceForDate(id, date) },
                        onSwitchRole = {
                            viewModel.selectCategory(null)
                            viewModel.setUserRole(UserRole.COACH)
                        },
                        onOpenNotifications = { isNotificationsDialogOpen = true },
                        onOpenProfileDialog = { isAdminProfileDialogOpen = true },
                        onAddAthlete = { isAddAthleteDialogOpen = true },
                        onAddCoach = { viewModel.addCoach(it) },
                        onUpdateCoach = { viewModel.updateCoach(it) },
                        onToggleCoachActive = { viewModel.toggleCoachActive(it) },
                        onAddCategory = { viewModel.addCategory(it) },
                        onUpdateCategory = { viewModel.updateCategory(it) },
                        onToggleCategoryActive = { viewModel.toggleCategoryActive(it) },
                        onCorrectAttendance = { sId, mName, pres -> viewModel.correctSessionAttendance(sId, mName, pres) },
                        onSetThemePreference = { viewModel.setThemePreference(it) },
                        onUpdateClubSettings = { viewModel.updateClubSettings(it) },
                        onUpdateAdminProfile = { viewModel.updateAdminProfile(it) },
                        onLogout = {
                            if (supabaseConfig.isConfigured && viewModel.isOnline()) {
                                viewModel.performFullSync()
                            }
                            viewModel.selectCategory(null)
                            viewModel.setUserRole(UserRole.COACH)
                        },
                        onClearDatabase = { viewModel.clearDatabase() },
                        scheduledSessions = scheduledSessions,
                        onAddScheduledSession = { s, repeat -> viewModel.addScheduledSession(s, repeat) },
                        onUpdateScheduledSession = { s -> viewModel.updateScheduledSession(s) },
                        onDuplicateScheduledSession = { s -> viewModel.duplicateScheduledSession(s) },
                        onDeleteScheduledSession = { id, scope -> viewModel.deleteScheduledSession(id, scope) },
                        onCheckSessionConflicts = { s, excludeId -> viewModel.checkSessionConflicts(s, excludeId) }
                    )
                }
            }
        }
    }

    // Admin Dialogs
    if (isNotificationsDialogOpen) {
        NotificationsDialog(
            notifications = notifications,
            onMarkAsRead = { viewModel.markNotificationRead(it) },
            onMarkAllAsRead = { viewModel.markAllNotificationsRead() },
            onDismiss = { isNotificationsDialogOpen = false }
        )
    }

    if (isAdminProfileDialogOpen) {
        AdminProfileEditDialog(
            profile = adminProfile,
            onSave = { viewModel.updateAdminProfile(it) },
            onDismiss = { isAdminProfileDialogOpen = false }
        )
    }

    // Modal dialogs triggered by scan or actions
    when (val result = activeCheckInResult) {
        is CheckInResult.Success -> {
            CheckInConfirmationDialog(
                member = result.member,
                timeString = result.attendance.timeString,
                onDismiss = { viewModel.dismissCheckInDialog() },
                onViewProfile = { memberId ->
                    viewModel.dismissCheckInDialog()
                    selectedMemberId = memberId
                }
            )
        }
        is CheckInResult.AlreadyCheckedIn -> {
            AlreadyCheckedInDialog(
                member = result.member,
                existingTime = result.existingAttendance.timeString,
                onDismiss = { viewModel.dismissCheckInDialog() }
            )
        }
        else -> Unit
    }

    // Associate unassigned QR Card Dialog
    if (unassignedQrCode != null && scannedMemberForCreation == null) {
        val pendingCode = unassignedQrCode!!
        AssociateCardDialog(
            qrCode = pendingCode,
            members = allMembers,
            onAssociate = { memberId, code ->
                viewModel.associateQrToMember(memberId, code)
            },
            onCreateNewMemberWithCard = { code ->
                viewModel.cancelUnassignedQr()
                viewModel.extractCardDataForMemberCreation(code)
            },
            onDismiss = { viewModel.cancelUnassignedQr() }
        )
    }

    // Scanned Card Auto-Extraction Dialog for Member Registration!
    if (scannedMemberForCreation != null) {
        AddEditMemberDialog(
            initialMember = null,
            initialExtractedInfo = scannedMemberForCreation,
            coachProfile = coachProfile,
            onEditCoachProfile = { isCoachProfileDialogOpen = true },
            onRequestScanCard = {
                activeStatFilter = null
                selectedMemberId = null
                scannerMode = ScannerTargetMode.EXTRACTION_MEMBRE
                currentTab = ScreenTab.HOME
            },
            onSave = { newMember ->
                viewModel.saveMember(newMember)
                viewModel.clearScannedMemberForCreation()
            },
            onDismiss = { viewModel.clearScannedMemberForCreation() }
        )
    }

    // Edit Member Dialog
    if (memberToEdit != null) {
        AddEditMemberDialog(
            initialMember = memberToEdit,
            coachProfile = coachProfile,
            onEditCoachProfile = { isCoachProfileDialogOpen = true },
            onSave = { updated ->
                viewModel.saveMember(updated)
                memberToEdit = null
            },
            onDismiss = { memberToEdit = null }
        )
    }

    // Add Athlete Dialog triggered from Admin Dashboard or shortcuts
    if (isAddAthleteDialogOpen) {
        AddEditMemberDialog(
            initialMember = null,
            coachProfile = coachProfile,
            onEditCoachProfile = { isCoachProfileDialogOpen = true },
            onSave = { newMember ->
                viewModel.saveMember(newMember)
                isAddAthleteDialogOpen = false
            },
            onDismiss = { isAddAthleteDialogOpen = false }
        )
    }

    // Coach / Sensei Profile Dialog
    if (isCoachProfileDialogOpen) {
        CoachProfileDialog(
            coachProfile = coachProfile,
            onSave = { updated ->
                viewModel.updateCoachProfile(updated)
                isCoachProfileDialogOpen = false
            },
            onDismiss = { isCoachProfileDialogOpen = false }
        )
    }
}

@Composable
private fun AppScreenContent(
    currentTab: ScreenTab,
    selectedMemberId: Long?,
    activeStatFilter: StatFilterMode?,
    activeStatPeriod: StatPeriod,
    dashboardPeriod: StatPeriod,
    activeAdminSubScreen: AdminSubScreen,
    coaches: List<CoachUser>,
    categories: List<JudoCategory>,
    clubSettings: ClubSettings,
    adminProfile: AdminProfile,
    pastSessions: List<PastTrainingSession>,
    notifications: List<AdminNotification>,
    themePreference: ThemePreference,
    allMembers: List<Member>,
    todayAttendance: List<com.example.data.model.AttendanceWithMember>,
    recentAttendance: List<com.example.data.model.AttendanceWithMember>,
    allAttendance: List<com.example.data.model.AttendanceWithMember>,
    lastScanned: com.example.data.model.AttendanceWithMember?,
    topStats: List<com.example.data.repository.MemberAttendanceStat>,
    isDarkTheme: Boolean,
    coachProfile: CoachProfile,
    activeSession: ActiveTrainingSession?,
    onAdminSubScreenChange: (AdminSubScreen) -> Unit,
    onDashboardPeriodChange: (StatPeriod) -> Unit,
    onActiveStatPeriodChange: (StatPeriod) -> Unit,
    onFilterModeChange: (StatFilterMode) -> Unit,
    onEditCoachProfile: () -> Unit,
    onToggleTheme: () -> Unit,
    onNavigateToTab: (ScreenTab) -> Unit,
    onSelectMember: (Long) -> Unit,
    onBackFromMemberDetail: () -> Unit,
    onStatClick: (StatFilterMode, StatPeriod) -> Unit,
    onBackFromStatFilter: () -> Unit,
    onEditMember: (Member) -> Unit,
    onDeleteMember: (Member) -> Unit,
    onSaveMember: (Member) -> Unit,
    onDeleteAttendance: (Long) -> Unit,
    onManualCheckIn: (Long) -> Unit,
    onRecordAttendanceForDate: (Long, String) -> Unit = { _, _ -> },
    onDeleteAttendanceForDate: (Long, String) -> Unit = { _, _ -> },
    onSwitchRole: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenProfileDialog: () -> Unit = {},
    onAddAthlete: () -> Unit = {},
    onAddCoach: (CoachUser) -> Unit = {},
    onUpdateCoach: (CoachUser) -> Unit = {},
    onToggleCoachActive: (String) -> Unit = {},
    onAddCategory: (JudoCategory) -> Unit = {},
    onUpdateCategory: (JudoCategory) -> Unit = {},
    onToggleCategoryActive: (String) -> Unit = {},
    onCorrectAttendance: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onSetThemePreference: (ThemePreference) -> Unit = {},
    onUpdateClubSettings: (ClubSettings) -> Unit = {},
    onUpdateAdminProfile: (AdminProfile) -> Unit = {},
    onLogout: () -> Unit = {},
    onClearDatabase: () -> Unit = {},
    scheduledSessions: List<com.example.data.model.ScheduledTrainingSession> = emptyList(),
    onAddScheduledSession: (com.example.data.model.ScheduledTrainingSession, Int) -> Unit = { _, _ -> },
    onUpdateScheduledSession: (com.example.data.model.ScheduledTrainingSession) -> Unit = {},
    onDuplicateScheduledSession: (com.example.data.model.ScheduledTrainingSession) -> Unit = {},
    onDeleteScheduledSession: (String, com.example.data.model.RecurrenceScope) -> Unit = { _, _ -> },
    onCheckSessionConflicts: (com.example.data.model.ScheduledTrainingSession, String?) -> List<com.example.data.model.SessionConflict> = { _, _ -> emptyList() },
    supabaseConfig: com.example.data.sync.SupabaseConfig = com.example.data.sync.SupabaseConfig(),
    isSyncing: Boolean = false,
    pendingSyncCount: Int = 0,
    lastSyncReport: com.example.data.sync.SyncReport? = null,
    recentAuditLogs: List<com.example.data.local.sync.SyncAuditLogEntity> = emptyList(),
    isOnline: Boolean = true,
    onUpdateSupabaseConfig: (String, String, Boolean, Boolean) -> Unit = { _, _, _, _ -> },
    onTestConnection: suspend () -> Result<String> = { Result.success("") },
    onPerformSync: () -> Unit = {},
    onGenerateBackup: suspend () -> String = { "" },
    onRestoreBackup: suspend (String) -> Result<String> = { Result.success("") }
) {
    // 1. Member Profile Detail Screen
    if (selectedMemberId != null) {
        val member = allMembers.find { it.id == selectedMemberId }
        if (member != null) {
            val memberAttendances = allAttendance
                .filter { it.memberId == selectedMemberId }
                .map {
                    com.example.data.model.Attendance(
                        id = it.attendanceId,
                        memberId = it.memberId,
                        dateString = it.dateString,
                        timeString = it.timeString,
                        timestamp = it.timestamp
                    )
                }

            MemberDetailScreen(
                member = member,
                attendanceList = memberAttendances,
                allClubAttendances = allAttendance,
                onBack = onBackFromMemberDetail,
                onManualCheckIn = onManualCheckIn,
                onRecordAttendanceForDate = onRecordAttendanceForDate,
                onDeleteAttendanceForDate = onDeleteAttendanceForDate,
                onEditMember = onEditMember,
                onDeleteMember = onDeleteMember
            )
            return
        }
    }

    // 2. Stat Filtered Members Screen (Absents, Présents, Inscrits, Taux)
    if (activeStatFilter != null) {
        StatFilteredMembersScreen(
            filterMode = activeStatFilter,
            statPeriod = activeStatPeriod,
            onPeriodChange = onActiveStatPeriodChange,
            onFilterModeChange = onFilterModeChange,
            allMembers = allMembers,
            todayAttendance = todayAttendance,
            allAttendance = allAttendance,
            onSelectMember = onSelectMember,
            onManualCheckIn = onManualCheckIn,
            onBack = onBackFromStatFilter
        )
        return
    }

    // 3. Sub-Screens accessible from Quick Access or Header
    when (activeAdminSubScreen) {
        AdminSubScreen.STATS_DETAIL -> {
            DashboardScreen(
                members = allMembers,
                todayAttendance = todayAttendance,
                allAttendance = allAttendance,
                selectedPeriod = dashboardPeriod,
                onPeriodChange = onDashboardPeriodChange,
                onSelectMember = onSelectMember,
                onStatClick = onStatClick
            )
            return
        }
        AdminSubScreen.ATTENDANCE_OVERVIEW -> {
            AdminAttendanceOverviewScreen(
                allMembers = allMembers,
                todayAttendance = todayAttendance,
                allAttendance = allAttendance,
                categories = categories,
                pastSessions = pastSessions,
                onSelectMember = onSelectMember,
                onBack = { onAdminSubScreenChange(AdminSubScreen.NONE) }
            )
            return
        }
        AdminSubScreen.EXPORTS -> {
            AdminExportsScreen(
                allMembers = allMembers,
                allAttendance = allAttendance,
                todayAttendance = todayAttendance,
                categories = categories,
                onBack = { onAdminSubScreenChange(AdminSubScreen.NONE) }
            )
            return
        }
        AdminSubScreen.SETTINGS -> {
            AdminSettingsScreen(
                clubSettings = clubSettings,
                adminProfile = adminProfile,
                currentThemePreference = themePreference,
                onSetThemePreference = onSetThemePreference,
                onUpdateClubSettings = onUpdateClubSettings,
                onUpdateAdminProfile = onUpdateAdminProfile,
                onNavigateToCloudSync = { onAdminSubScreenChange(AdminSubScreen.CLOUD_SYNC) },
                onLogout = onLogout,
                onClearDatabase = onClearDatabase,
                onBack = { onAdminSubScreenChange(AdminSubScreen.NONE) }
            )
            return
        }
        AdminSubScreen.CLOUD_SYNC -> {
            AdminCloudSyncScreen(
                supabaseConfig = supabaseConfig,
                isOnline = isOnline,
                isSyncing = isSyncing,
                pendingCount = pendingSyncCount,
                lastSyncReport = lastSyncReport,
                recentAuditLogs = recentAuditLogs,
                onUpdateConfig = onUpdateSupabaseConfig,
                onTestConnection = onTestConnection,
                onPerformSync = onPerformSync,
                onGenerateBackup = onGenerateBackup,
                onRestoreBackup = onRestoreBackup,
                onBack = { onAdminSubScreenChange(AdminSubScreen.NONE) }
            )
            return
        }
        AdminSubScreen.NONE -> Unit
    }

    // 4. Primary Admin Tab Screens (Without QR scan or scanner tools)
    when (currentTab) {
        ScreenTab.HOME -> {
            AdminDashboardScreen(
                clubSettings = clubSettings,
                adminProfile = adminProfile,
                allMembers = allMembers,
                todayAttendance = todayAttendance,
                allAttendance = allAttendance,
                categories = categories,
                coaches = coaches,
                pastSessions = pastSessions,
                notifications = notifications,
                isDarkTheme = isDarkTheme,
                onNavigateToAthletes = { onNavigateToTab(ScreenTab.MEMBERS) },
                onNavigateToCoaches = { onNavigateToTab(ScreenTab.COACHES) },
                onNavigateToCategories = { onNavigateToTab(ScreenTab.CATEGORIES) },
                onNavigateToAssignments = { onNavigateToTab(ScreenTab.COACHES) },
                onNavigateToSessions = { onNavigateToTab(ScreenTab.SESSIONS) },
                onNavigateToAttendanceOverview = { onAdminSubScreenChange(AdminSubScreen.ATTENDANCE_OVERVIEW) },
                onNavigateToHistory = { onNavigateToTab(ScreenTab.SESSIONS) },
                onNavigateToStats = { onAdminSubScreenChange(AdminSubScreen.STATS_DETAIL) },
                onNavigateToExports = { onAdminSubScreenChange(AdminSubScreen.EXPORTS) },
                onNavigateToSettings = { onAdminSubScreenChange(AdminSubScreen.SETTINGS) },
                onAddAthlete = onAddAthlete,
                onOpenNotifications = onOpenNotifications,
                onOpenProfileDialog = onOpenProfileDialog,
                onSwitchToCoach = onSwitchRole,
                onLogout = onLogout,
                onSelectMember = onSelectMember
            )
        }
        ScreenTab.MEMBERS -> {
            MembersScreen(
                members = allMembers,
                todayAttendance = todayAttendance,
                coachProfile = coachProfile,
                onEditCoachProfile = onEditCoachProfile,
                onSelectMember = onSelectMember,
                onSaveMember = onSaveMember
            )
        }
        ScreenTab.COACHES -> {
            AdminCoachesScreen(
                coaches = coaches,
                categories = categories,
                onAddCoach = onAddCoach,
                onUpdateCoach = onUpdateCoach,
                onToggleCoachActive = onToggleCoachActive
            )
        }
        ScreenTab.CATEGORIES -> {
            AdminCategoriesScreen(
                categories = categories,
                allMembers = allMembers,
                coaches = coaches,
                onAddCategory = onAddCategory,
                onUpdateCategory = onUpdateCategory,
                onToggleCategoryActive = onToggleCategoryActive,
                onViewCategoryMembers = { onNavigateToTab(ScreenTab.MEMBERS) }
            )
        }
        ScreenTab.SESSIONS -> {
            AdminSessionsScreen(
                scheduledSessions = scheduledSessions,
                pastSessions = pastSessions,
                activeSession = activeSession,
                categories = categories,
                coaches = coaches,
                onAddScheduledSession = onAddScheduledSession,
                onUpdateScheduledSession = onUpdateScheduledSession,
                onDuplicateScheduledSession = onDuplicateScheduledSession,
                onDeleteScheduledSession = onDeleteScheduledSession,
                onCheckConflicts = onCheckSessionConflicts,
                onCorrectAttendance = onCorrectAttendance
            )
        }
    }
}
