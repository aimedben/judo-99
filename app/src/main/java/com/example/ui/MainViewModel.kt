package com.example.ui

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ActiveTrainingSession
import com.example.data.model.AdminNotification
import com.example.data.model.AdminProfile
import com.example.data.model.AttendanceWithMember
import com.example.data.model.ClubSettings
import com.example.data.model.CoachProfile
import com.example.data.model.CoachScanFeedback
import com.example.data.model.CoachUser
import com.example.data.model.JudoCategory
import com.example.data.model.Member
import com.example.data.model.NotificationType
import com.example.data.model.PastTrainingSession
import com.example.data.model.ScheduledTrainingSession
import com.example.data.model.SessionStatus
import com.example.data.model.RecurrenceScope
import com.example.data.model.SessionConflict
import com.example.data.model.ConflictType
import com.example.data.model.UserRole
import com.example.data.repository.CheckInResult
import com.example.data.repository.JudoRepository
import com.example.data.repository.MemberAttendanceStat
import com.example.ui.screens.StatPeriod
import com.example.util.CardDataParser
import com.example.util.ExtractedMemberInfo
import com.example.data.local.CategoryEntity
import com.example.data.local.CoachEntity
import com.example.data.local.PastSessionEntity
import com.example.data.local.ScheduledSessionEntity
import com.example.data.local.sync.SyncAuditLogEntity
import com.example.data.sync.SupabaseConfig
import com.example.data.sync.SupabaseSyncService
import com.example.data.sync.SyncReport
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ThemePreference {
    DARK, LIGHT, SYSTEM
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("judo_seddouk_prefs", Context.MODE_PRIVATE)
    private val database = AppDatabase.getInstance(application)
    val supabaseSyncService = SupabaseSyncService(application, database)
    private val repository = JudoRepository(
        memberDao = database.memberDao(),
        attendanceDao = database.attendanceDao(),
        clubDao = database.clubDao(),
        syncService = supabaseSyncService
    )

    val supabaseConfig: StateFlow<SupabaseConfig> = supabaseSyncService.config
        .stateIn(viewModelScope, SharingStarted.Eagerly, SupabaseConfig())

    val isSyncing: StateFlow<Boolean> = supabaseSyncService.isSyncing
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val pendingSyncCount: StateFlow<Int> = supabaseSyncService.pendingCountFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val recentAuditLogs: StateFlow<List<SyncAuditLogEntity>> = supabaseSyncService.recentAuditLogs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _lastSyncReport = MutableStateFlow<SyncReport?>(null)
    val lastSyncReport: StateFlow<SyncReport?> = _lastSyncReport.asStateFlow()

    fun updateSupabaseConfig(url: String, anonKey: String, autoSync: Boolean, dailySync: Boolean) {
        supabaseSyncService.updateConfig(url, anonKey, autoSync, dailySync)
    }

    suspend fun testSupabaseConnection(): Result<String> {
        return supabaseSyncService.testConnection()
    }

    fun performFullSync(onFinished: ((SyncReport) -> Unit)? = null) {
        viewModelScope.launch {
            val report = supabaseSyncService.performFullSync()
            _lastSyncReport.value = report
            onFinished?.invoke(report)
        }
    }

    suspend fun generateBackupJson(): String {
        return supabaseSyncService.generateBackupJson()
    }

    suspend fun restoreBackupJson(jsonString: String): Result<String> {
        return supabaseSyncService.restoreBackupJson(jsonString)
    }

    fun isOnline(): Boolean {
        return supabaseSyncService.isOnline()
    }

    private fun startDaily22hSyncChecker() {
        viewModelScope.launch {
            var minuteCounter = 0
            while (true) {
                delay(60_000L)
                minuteCounter++
                try {
                    val cal = java.util.Calendar.getInstance()
                    val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                    val minute = cal.get(java.util.Calendar.MINUTE)
                    if (hour == 22 && minute == 0 && supabaseConfig.value.isDaily22hSyncEnabled && isOnline()) {
                        supabaseSyncService.performFullSync()
                    } else if (supabaseConfig.value.isAutoSyncEnabled && isOnline()) {
                        // Synchronisation automatique périodique toutes les 5 minutes ou dès qu'il y a des opérations en attente
                        if (pendingSyncCount.value > 0 || (minuteCounter % 5 == 0)) {
                            supabaseSyncService.performFullSync()
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private val _themePreference = MutableStateFlow(
        try {
            ThemePreference.valueOf(prefs.getString("theme_pref", ThemePreference.LIGHT.name) ?: ThemePreference.LIGHT.name)
        } catch (_: Exception) {
            ThemePreference.LIGHT
        }
    )
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    private val _coachProfile = MutableStateFlow(loadCoachProfile())
    val coachProfile: StateFlow<CoachProfile> = _coachProfile.asStateFlow()

    private fun loadCoachProfile(): CoachProfile {
        return CoachProfile(
            name = prefs.getString("coach_name", "Sensei Ahmed BOURENANE") ?: "Sensei Ahmed BOURENANE",
            title = prefs.getString("coach_title", "Entraîneur Principal") ?: "Entraîneur Principal",
            beltName = prefs.getString("coach_belt", "NOIRE") ?: "NOIRE",
            danGrade = prefs.getString("coach_dan", "Ceinture Noire 4ème Dan") ?: "Ceinture Noire 4ème Dan",
            phone = prefs.getString("coach_phone", "0782487120") ?: "0782487120",
            photoUrl = prefs.getString("coach_photo_url", "") ?: "",
            clubName = prefs.getString("coach_club", "JUDO CLUB SEDDOUK") ?: "JUDO CLUB SEDDOUK",
            bio = prefs.getString("coach_bio", "Professeur diplômé d'État • Dojo Municipal de Seddouk") ?: "Professeur diplômé d'État • Dojo Municipal de Seddouk"
        )
    }

    fun updateCoachProfile(profile: CoachProfile) {
        _coachProfile.value = profile
        prefs.edit()
            .putString("coach_name", profile.name)
            .putString("coach_title", profile.title)
            .putString("coach_belt", profile.beltName)
            .putString("coach_dan", profile.danGrade)
            .putString("coach_phone", profile.phone)
            .putString("coach_photo_url", profile.photoUrl)
            .putString("coach_club", profile.clubName)
            .putString("coach_bio", profile.bio)
            .apply()
    }

    fun updateCoachPhoto(photoUrl: String) {
        val updated = _coachProfile.value.copy(photoUrl = photoUrl)
        updateCoachProfile(updated)
    }

    // Categories Management
    private val _categories = MutableStateFlow<List<JudoCategory>>(
        listOf(
            JudoCategory("baby", "Baby Judo", "EVEIL", "Sensei Ahmed", "Coach Yacine", "Initiation 4-5 ans"),
            JudoCategory("poussins", "Poussins", "U11", "Sensei Ahmed", "Coach Karim", "Enfants 8-9 ans"),
            JudoCategory("benjamins", "Benjamins", "U13", "Sensei Ahmed", "Coach Yacine", "Jeunes 10-11 ans"),
            JudoCategory("minimes", "Minimes", "U15", "Sensei Ahmed", "Coach Karim", "Cadres 12-13 ans"),
            JudoCategory("cadets", "Cadets", "U18", "Coach Yacine", "Sensei Ahmed", "Compétition 14-16 ans"),
            JudoCategory("adultes", "Adultes & Séniors", "SENIOR", "Sensei Ahmed", "Coach Yacine", "Juniors & Séniors 17+ ans")
        )
    )
    val categories: StateFlow<List<JudoCategory>> = _categories.asStateFlow()

    private val _userRole = MutableStateFlow(UserRole.COACH)
    val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

    private val _selectedCategory = MutableStateFlow<JudoCategory?>(null)
    val selectedCategory: StateFlow<JudoCategory?> = _selectedCategory.asStateFlow()

    private val _activeSession = MutableStateFlow<ActiveTrainingSession?>(null)
    val activeSession: StateFlow<ActiveTrainingSession?> = _activeSession.asStateFlow()

    private val _coachScanFeedback = MutableStateFlow<CoachScanFeedback?>(null)
    val coachScanFeedback: StateFlow<CoachScanFeedback?> = _coachScanFeedback.asStateFlow()

    fun clearCoachScanFeedback() {
        _coachScanFeedback.value = null
    }

    private val _pastSessions = MutableStateFlow<List<PastTrainingSession>>(
        listOf(
            PastTrainingSession(
                categoryId = "benjamins",
                categoryName = "Benjamins",
                groupCode = "U13",
                dateString = "Mercredi 8 Octobre 2026",
                startTime = "17:30",
                endTime = "19:00",
                coach1Name = "Sensei Ahmed",
                coach1Present = true,
                coach2Name = "Coach Yacine",
                coach2Present = true,
                presentCount = 18,
                absentCount = 3,
                presentMemberNames = listOf("Karim Belkacem", "Amine Mansouri", "Yanis Ait-Ali", "Mehdi Saidi", "Sami Benali", "Ines Meziane"),
                absentMemberNames = listOf("Rayane Hamdi", "Lina Khelif", "Adel Boudiaf")
            ),
            PastTrainingSession(
                categoryId = "benjamins",
                categoryName = "Benjamins",
                groupCode = "U13",
                dateString = "Lundi 6 Octobre 2026",
                startTime = "17:30",
                endTime = "19:00",
                coach1Name = "Sensei Ahmed",
                coach1Present = true,
                coach2Name = "Coach Yacine",
                coach2Present = false,
                presentCount = 19,
                absentCount = 2,
                presentMemberNames = listOf("Karim Belkacem", "Amine Mansouri", "Rayane Hamdi", "Yanis Ait-Ali", "Mehdi Saidi"),
                absentMemberNames = listOf("Sami Benali", "Lina Khelif")
            ),
            PastTrainingSession(
                categoryId = "minimes",
                categoryName = "Minimes",
                groupCode = "U15",
                dateString = "Mardi 7 Octobre 2026",
                startTime = "18:00",
                endTime = "19:30",
                coach1Name = "Sensei Ahmed",
                coach1Present = true,
                coach2Name = "Coach Karim",
                coach2Present = true,
                presentCount = 15,
                absentCount = 2,
                presentMemberNames = listOf("Tarek Bouzid", "Walid Cherif", "Sofiane Brahimi", "Nassim Hadj"),
                absentMemberNames = listOf("Bilal Kaci", "Malik Oualid")
            ),
            PastTrainingSession(
                categoryId = "baby",
                categoryName = "Baby Judo",
                groupCode = "EVEIL",
                dateString = "Samedi 4 Octobre 2026",
                startTime = "10:00",
                endTime = "11:00",
                coach1Name = "Sensei Ahmed",
                coach1Present = true,
                coach2Name = "Coach Yacine",
                coach2Present = true,
                presentCount = 14,
                absentCount = 1,
                presentMemberNames = listOf("Adam Khelil", "Nour Benali", "Ilyas Messaoudi", "Yasmine Larbi"),
                absentMemberNames = listOf("Maya Boualem")
            )
        )
    )
    val pastSessions: StateFlow<List<PastTrainingSession>> = _pastSessions.asStateFlow()

    // Coach Users Management
    private val _coaches = MutableStateFlow<List<CoachUser>>(loadCoachesFromPrefs())
    val coaches: StateFlow<List<CoachUser>> = _coaches.asStateFlow()

    private fun loadCoachesFromPrefs(): List<CoachUser> {
        val defaultCoaches = listOf(
            CoachUser("c1", "Sensei Ahmed BOURENANE", "Ceinture Noire 4ème Dan", "0782487120", listOf("Baby Judo", "Poussins", "Benjamins", "Minimes", "Adultes & Séniors"), true, 42),
            CoachUser("c2", "Coach Yacine HAMIDI", "Ceinture Noire 2ème Dan", "0550123456", listOf("Baby Judo", "Benjamins", "Cadets", "Adultes & Séniors"), true, 28),
            CoachUser("c3", "Coach Karim BELKACEM", "Ceinture Noire 1er Dan", "0661987654", listOf("Poussins", "Minimes"), true, 19),
            CoachUser("c4", "Coach Amina SAIDI", "Ceinture Marron", "0770345678", listOf("Baby Judo"), true, 12)
        )
        val json = prefs.getString("coaches_json", null) ?: return defaultCoaches
        return try {
            val arr = org.json.JSONArray(json)
            val list = mutableListOf<CoachUser>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val catArr = obj.optJSONArray("assignedCategories") ?: org.json.JSONArray()
                val cats = mutableListOf<String>()
                for (j in 0 until catArr.length()) {
                    cats.add(catArr.getString(j))
                }
                list.add(
                    CoachUser(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        grade = obj.optString("grade", "Ceinture Noire"),
                        phone = obj.optString("phone", ""),
                        assignedCategories = cats,
                        isActive = obj.optBoolean("isActive", true),
                        sessionsCount = obj.optInt("sessionsCount", 0)
                    )
                )
            }
            if (list.isNotEmpty()) list else defaultCoaches
        } catch (_: Exception) {
            defaultCoaches
        }
    }

    private fun saveCoachesToPrefs() {
        try {
            val arr = org.json.JSONArray()
            for (c in _coaches.value) {
                val obj = org.json.JSONObject()
                obj.put("id", c.id)
                obj.put("name", c.name)
                obj.put("grade", c.grade)
                obj.put("phone", c.phone)
                obj.put("assignedCategories", org.json.JSONArray(c.assignedCategories))
                obj.put("isActive", c.isActive)
                obj.put("sessionsCount", c.sessionsCount)
                arr.put(obj)
            }
            prefs.edit().putString("coaches_json", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun addCoach(coach: CoachUser) {
        _coaches.value = _coaches.value + coach
        saveCoachesToPrefs()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.clubDao().insertCoach(CoachEntity.fromDomain(coach))
                supabaseSyncService.enqueueCoach(CoachEntity.fromDomain(coach), "UPSERT")
            } catch (_: Exception) {}
        }
    }

    fun updateCoach(coach: CoachUser) {
        _coaches.value = _coaches.value.map { if (it.id == coach.id) coach else it }
        saveCoachesToPrefs()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.clubDao().updateCoach(CoachEntity.fromDomain(coach))
                supabaseSyncService.enqueueCoach(CoachEntity.fromDomain(coach), "UPSERT")
            } catch (_: Exception) {}
        }
    }

    fun toggleCoachActive(coachId: String) {
        _coaches.value = _coaches.value.map {
            if (it.id == coachId) it.copy(isActive = !it.isActive) else it
        }
        saveCoachesToPrefs()
        val updated = _coaches.value.find { it.id == coachId }
        if (updated != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    database.clubDao().updateCoach(CoachEntity.fromDomain(updated))
                    supabaseSyncService.enqueueCoach(CoachEntity.fromDomain(updated), "UPSERT")
                } catch (_: Exception) {}
            }
        }
    }

    fun deleteCoach(coachId: String) {
        val coachToDelete = _coaches.value.find { it.id == coachId }
        _coaches.value = _coaches.value.filterNot { it.id == coachId }
        saveCoachesToPrefs()

        if (coachToDelete != null) {
            val coachName = coachToDelete.name
            // Dissociate coach from categories if currently assigned
            _categories.value = _categories.value.map { cat ->
                var updated = cat
                if (cat.coach1Name.equals(coachName, ignoreCase = true)) {
                    updated = updated.copy(coach1Name = "")
                }
                if (cat.coach2Name.equals(coachName, ignoreCase = true)) {
                    updated = updated.copy(coach2Name = "")
                }
                updated
            }
            // Dissociate coach from scheduled sessions
            _scheduledSessions.value = _scheduledSessions.value.map { s ->
                if (s.coachNames.any { it.equals(coachName, ignoreCase = true) }) {
                    s.copy(coachNames = s.coachNames.filterNot { it.equals(coachName, ignoreCase = true) })
                } else s
            }
            saveScheduledSessionsToPrefs()

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    database.clubDao().deleteCoachById(coachId)
                } catch (_: Exception) {}
                try {
                    supabaseSyncService.enqueueCoach(CoachEntity.fromDomain(coachToDelete), "DELETE")
                } catch (_: Exception) {}
            }
        }
    }

    fun deletePastSession(sessionId: String) {
        _pastSessions.value = _pastSessions.value.filterNot { it.id == sessionId }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.clubDao().deletePastSessionById(sessionId)
            } catch (_: Exception) {}
        }
    }

    // Categories Management
    fun addCategory(category: JudoCategory) {
        _categories.value = _categories.value + category
    }

    fun updateCategory(category: JudoCategory) {
        _categories.value = _categories.value.map { if (it.id == category.id) category else it }
    }

    fun toggleCategoryActive(categoryId: String) {
        _categories.value = _categories.value.map {
            if (it.id == categoryId) it.copy(isActive = !it.isActive) else it
        }
    }

    // Coach assignment to categories
    fun assignCoachToCategory(categoryId: String, coachName: String) {
        _categories.value = _categories.value.map { cat ->
            if (cat.id == categoryId) {
                if (cat.coach1Name.isBlank() || cat.coach1Name == "Non assigné") {
                    cat.copy(coach1Name = coachName)
                } else {
                    cat.copy(coach2Name = coachName)
                }
            } else cat
        }
        _coaches.value = _coaches.value.map { coach ->
            if (coach.name.equals(coachName, ignoreCase = true)) {
                val catName = _categories.value.find { it.id == categoryId }?.name ?: categoryId
                if (!coach.assignedCategories.contains(catName)) {
                    coach.copy(assignedCategories = coach.assignedCategories + catName)
                } else coach
            } else coach
        }
    }

    fun removeCoachFromCategory(categoryId: String, coachName: String) {
        _categories.value = _categories.value.map { cat ->
            if (cat.id == categoryId) {
                when {
                    cat.coach1Name.equals(coachName, ignoreCase = true) -> cat.copy(coach1Name = cat.coach2Name, coach2Name = "")
                    cat.coach2Name.equals(coachName, ignoreCase = true) -> cat.copy(coach2Name = "")
                    else -> cat
                }
            } else cat
        }
        _coaches.value = _coaches.value.map { coach ->
            if (coach.name.equals(coachName, ignoreCase = true)) {
                val catName = _categories.value.find { it.id == categoryId }?.name ?: categoryId
                coach.copy(assignedCategories = coach.assignedCategories.filterNot { it.equals(catName, ignoreCase = true) })
            } else coach
        }
    }

    // Scheduled Training Sessions (Weekly Planning & Calendar)
    private val _scheduledSessions = MutableStateFlow<List<ScheduledTrainingSession>>(
        listOf(
            ScheduledTrainingSession(
                id = "s_baby_sam",
                categoryId = "baby",
                categoryName = "Baby Judo",
                groupCode = "EVEIL",
                dayOfWeekName = "Samedi",
                dateString = "2026-10-17",
                formattedDisplayDate = "Samedi 17 Octobre 2026",
                startTime = "10:00",
                endTime = "11:00",
                coachIds = listOf("c1", "c2"),
                coachNames = listOf("Sensei Ahmed BOURENANE", "Coach Yacine HAMIDI"),
                isRecurringWeekly = true,
                status = SessionStatus.SCHEDULED,
                notes = "Éveil moteur & chutes de base"
            ),
            ScheduledTrainingSession(
                id = "s_adultes_sam",
                categoryId = "adultes",
                categoryName = "Adultes & Séniors",
                groupCode = "SENIOR",
                dayOfWeekName = "Samedi",
                dateString = "2026-10-17",
                formattedDisplayDate = "Samedi 17 Octobre 2026",
                startTime = "18:00",
                endTime = "20:00",
                coachIds = listOf("c1", "c2"),
                coachNames = listOf("Sensei Ahmed BOURENANE", "Coach Yacine HAMIDI"),
                isRecurringWeekly = true,
                status = SessionStatus.SCHEDULED,
                notes = "Ne-waza & Randori intensif"
            ),
            ScheduledTrainingSession(
                id = "s_poussins_lun",
                categoryId = "poussins",
                categoryName = "Poussins",
                groupCode = "U11",
                dayOfWeekName = "Lundi",
                dateString = "2026-10-12",
                formattedDisplayDate = "Lundi 12 Octobre 2026",
                startTime = "17:00",
                endTime = "18:30",
                coachIds = listOf("c1", "c3"),
                coachNames = listOf("Sensei Ahmed BOURENANE", "Coach Karim BELKACEM"),
                isRecurringWeekly = true,
                status = SessionStatus.SCHEDULED,
                notes = "Technique debout : O-Soto-Gari"
            ),
            ScheduledTrainingSession(
                id = "s_benjamins_lun",
                categoryId = "benjamins",
                categoryName = "Benjamins",
                groupCode = "U13",
                dayOfWeekName = "Lundi",
                dateString = "2026-10-12",
                formattedDisplayDate = "Lundi 12 Octobre 2026",
                startTime = "17:30",
                endTime = "19:00",
                coachIds = listOf("c1", "c2"),
                coachNames = listOf("Sensei Ahmed BOURENANE", "Coach Yacine HAMIDI"),
                isRecurringWeekly = true,
                status = SessionStatus.SCHEDULED,
                notes = "Kumikata et déplacements"
            ),
            ScheduledTrainingSession(
                id = "s_minimes_mar",
                categoryId = "minimes",
                categoryName = "Minimes",
                groupCode = "U15",
                dayOfWeekName = "Mardi",
                dateString = "2026-10-13",
                formattedDisplayDate = "Mardi 13 Octobre 2026",
                startTime = "18:00",
                endTime = "19:30",
                coachIds = listOf("c1", "c3"),
                coachNames = listOf("Sensei Ahmed BOURENANE", "Coach Karim BELKACEM"),
                isRecurringWeekly = true,
                status = SessionStatus.SCHEDULED,
                notes = "Perfectionnement Seoi-Nage"
            ),
            ScheduledTrainingSession(
                id = "s_cadets_mar",
                categoryId = "cadets",
                categoryName = "Cadets",
                groupCode = "U18",
                dayOfWeekName = "Mardi",
                dateString = "2026-10-13",
                formattedDisplayDate = "Mardi 13 Octobre 2026",
                startTime = "19:30",
                endTime = "21:00",
                coachIds = listOf("c2", "c1"),
                coachNames = listOf("Coach Yacine HAMIDI", "Sensei Ahmed BOURENANE"),
                isRecurringWeekly = true,
                status = SessionStatus.SCHEDULED,
                notes = "Préparation régionale & liaison debout-sol"
            ),
            ScheduledTrainingSession(
                id = "s_baby_mer",
                categoryId = "baby",
                categoryName = "Baby Judo",
                groupCode = "EVEIL",
                dayOfWeekName = "Mercredi",
                dateString = "2026-10-14",
                formattedDisplayDate = "Mercredi 14 Octobre 2026",
                startTime = "14:00",
                endTime = "15:00",
                coachIds = listOf("c1", "c4"),
                coachNames = listOf("Sensei Ahmed BOURENANE", "Coach Amina SAIDI"),
                isRecurringWeekly = true,
                status = SessionStatus.SCHEDULED,
                notes = "Parcours d'agilité & jeux d'opposition"
            ),
            ScheduledTrainingSession(
                id = "s_benjamins_mer",
                categoryId = "benjamins",
                categoryName = "Benjamins",
                groupCode = "U13",
                dayOfWeekName = "Mercredi",
                dateString = "2026-10-14",
                formattedDisplayDate = "Mercredi 14 Octobre 2026",
                startTime = "17:30",
                endTime = "19:00",
                coachIds = listOf("c1", "c2"),
                coachNames = listOf("Sensei Ahmed BOURENANE", "Coach Yacine HAMIDI"),
                isRecurringWeekly = true,
                status = SessionStatus.SCHEDULED,
                notes = "Enchaînements & combinaisons"
            ),
            ScheduledTrainingSession(
                id = "s_adultes_mer",
                categoryId = "adultes",
                categoryName = "Adultes & Séniors",
                groupCode = "SENIOR",
                dayOfWeekName = "Mercredi",
                dateString = "2026-10-14",
                formattedDisplayDate = "Mercredi 14 Octobre 2026",
                startTime = "19:30",
                endTime = "21:30",
                coachIds = listOf("c1", "c2"),
                coachNames = listOf("Sensei Ahmed BOURENANE", "Coach Yacine HAMIDI"),
                isRecurringWeekly = true,
                status = SessionStatus.SCHEDULED,
                notes = "Kata & perfectionnement technique"
            ),
            ScheduledTrainingSession(
                id = "s_poussins_jeu",
                categoryId = "poussins",
                categoryName = "Poussins",
                groupCode = "U11",
                dayOfWeekName = "Jeudi",
                dateString = "2026-10-15",
                formattedDisplayDate = "Jeudi 15 Octobre 2026",
                startTime = "17:00",
                endTime = "18:30",
                coachIds = listOf("c1", "c3"),
                coachNames = listOf("Sensei Ahmed BOURENANE", "Coach Karim BELKACEM"),
                isRecurringWeekly = true,
                status = SessionStatus.SCHEDULED,
                notes = "Randori éducatif"
            ),
            ScheduledTrainingSession(
                id = "s_cadets_jeu",
                categoryId = "cadets",
                categoryName = "Cadets",
                groupCode = "U18",
                dayOfWeekName = "Jeudi",
                dateString = "2026-10-15",
                formattedDisplayDate = "Jeudi 15 Octobre 2026",
                startTime = "19:30",
                endTime = "21:00",
                coachIds = listOf("c2", "c1"),
                coachNames = listOf("Coach Yacine HAMIDI", "Sensei Ahmed BOURENANE"),
                isRecurringWeekly = true,
                status = SessionStatus.SCHEDULED,
                notes = "Situation de compétition & arbitrage"
            ),
            ScheduledTrainingSession(
                id = "s_minimes_ven",
                categoryId = "minimes",
                categoryName = "Minimes",
                groupCode = "U15",
                dayOfWeekName = "Vendredi",
                dateString = "2026-10-16",
                formattedDisplayDate = "Vendredi 16 Octobre 2026",
                startTime = "18:00",
                endTime = "19:30",
                coachIds = listOf("c1", "c3"),
                coachNames = listOf("Sensei Ahmed BOURENANE", "Coach Karim BELKACEM"),
                isRecurringWeekly = true,
                status = SessionStatus.SCHEDULED,
                notes = "Randori à thèmes"
            )
        )
    )
    private fun saveScheduledSessionsToPrefs() {
        try {
            val json = serializeScheduledSessions(_scheduledSessions.value)
            prefs.edit().putString("scheduled_sessions_json", json).apply()
        } catch (_: Exception) {}
    }

    private fun serializeScheduledSessions(sessions: List<ScheduledTrainingSession>): String {
        val arr = org.json.JSONArray()
        for (s in sessions) {
            val obj = org.json.JSONObject()
            obj.put("id", s.id)
            obj.put("categoryId", s.categoryId)
            obj.put("categoryName", s.categoryName)
            obj.put("groupCode", s.groupCode)
            obj.put("dayOfWeekName", s.dayOfWeekName)
            obj.put("dateString", s.dateString)
            obj.put("formattedDisplayDate", s.formattedDisplayDate)
            obj.put("startTime", s.startTime)
            obj.put("endTime", s.endTime)
            obj.put("coachIds", org.json.JSONArray(s.coachIds))
            obj.put("coachNames", org.json.JSONArray(s.coachNames))
            obj.put("isRecurringWeekly", s.isRecurringWeekly)
            if (s.recurrenceEndDate != null) obj.put("recurrenceEndDate", s.recurrenceEndDate)
            if (s.seriesId != null) obj.put("seriesId", s.seriesId)
            obj.put("notes", s.notes)
            obj.put("status", s.status.name)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun deserializeScheduledSessions(jsonStr: String): List<ScheduledTrainingSession> {
        val result = mutableListOf<ScheduledTrainingSession>()
        try {
            val arr = org.json.JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val coachIdsArr = obj.optJSONArray("coachIds")
                val coachIds = mutableListOf<String>()
                if (coachIdsArr != null) {
                    for (j in 0 until coachIdsArr.length()) coachIds.add(coachIdsArr.getString(j))
                }
                val coachNamesArr = obj.optJSONArray("coachNames")
                val coachNames = mutableListOf<String>()
                if (coachNamesArr != null) {
                    for (j in 0 until coachNamesArr.length()) coachNames.add(coachNamesArr.getString(j))
                }
                result.add(
                    ScheduledTrainingSession(
                        id = obj.getString("id"),
                        categoryId = obj.getString("categoryId"),
                        categoryName = obj.getString("categoryName"),
                        groupCode = obj.optString("groupCode", "U15"),
                        dayOfWeekName = obj.getString("dayOfWeekName"),
                        dateString = obj.optString("dateString", "2026-10-14"),
                        formattedDisplayDate = obj.optString("formattedDisplayDate", ""),
                        startTime = obj.getString("startTime"),
                        endTime = obj.getString("endTime"),
                        coachIds = coachIds,
                        coachNames = coachNames,
                        isRecurringWeekly = obj.optBoolean("isRecurringWeekly", true),
                        recurrenceEndDate = if (obj.has("recurrenceEndDate")) obj.getString("recurrenceEndDate") else null,
                        seriesId = if (obj.has("seriesId")) obj.getString("seriesId") else null,
                        notes = obj.optString("notes", ""),
                        status = try {
                            SessionStatus.valueOf(obj.optString("status", SessionStatus.SCHEDULED.name))
                        } catch (_: Exception) {
                            SessionStatus.SCHEDULED
                        }
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    val scheduledSessions: StateFlow<List<ScheduledTrainingSession>> = _scheduledSessions.asStateFlow()

    init {
        val savedSessionsJson = prefs.getString("scheduled_sessions_json", null)
        if (!savedSessionsJson.isNullOrBlank()) {
            val parsed = deserializeScheduledSessions(savedSessionsJson)
            if (parsed.isNotEmpty()) {
                _scheduledSessions.value = parsed
            }
        }
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            if (supabaseConfig.value.isConfigured && isOnline()) {
                val report = supabaseSyncService.performFullSync()
                _lastSyncReport.value = report
            }
        }
        startDaily22hSyncChecker()
    }

    fun addScheduledSession(session: ScheduledTrainingSession, repeatWeeks: Int = 1) {
        val seriesId = if (session.isRecurringWeekly) java.util.UUID.randomUUID().toString() else null
        val baseSession = session.copy(seriesId = seriesId)
        val newSessions = mutableListOf(baseSession)

        if (session.isRecurringWeekly && repeatWeeks > 1) {
            for (w in 1 until repeatWeeks) {
                newSessions.add(
                    baseSession.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        seriesId = seriesId
                    )
                )
            }
        }
        _scheduledSessions.value = _scheduledSessions.value + newSessions
        saveScheduledSessionsToPrefs()
    }

    fun duplicateScheduledSession(session: ScheduledTrainingSession, targetDay: String? = null, targetStartTime: String? = null): ScheduledTrainingSession {
        val duplicated = session.copy(
            id = java.util.UUID.randomUUID().toString(),
            dayOfWeekName = targetDay ?: session.dayOfWeekName,
            startTime = targetStartTime ?: session.startTime,
            seriesId = if (session.isRecurringWeekly) java.util.UUID.randomUUID().toString() else null
        )
        _scheduledSessions.value = _scheduledSessions.value + duplicated
        saveScheduledSessionsToPrefs()
        return duplicated
    }

    fun updateScheduledSession(session: ScheduledTrainingSession, scope: RecurrenceScope = RecurrenceScope.THIS_ONLY) {
        _scheduledSessions.value = _scheduledSessions.value.map { s ->
            when {
                s.id == session.id -> session
                scope == RecurrenceScope.ALL_SERIES && session.seriesId != null && s.seriesId == session.seriesId -> {
                    s.copy(
                        startTime = session.startTime,
                        endTime = session.endTime,
                        coachIds = session.coachIds,
                        coachNames = session.coachNames,
                        categoryId = session.categoryId,
                        categoryName = session.categoryName,
                        groupCode = session.groupCode,
                        notes = session.notes
                    )
                }
                else -> s
            }
        }
        saveScheduledSessionsToPrefs()
    }

    fun deleteScheduledSession(sessionId: String, scope: RecurrenceScope = RecurrenceScope.THIS_ONLY) {
        val target = _scheduledSessions.value.find { it.id == sessionId }
        _scheduledSessions.value = if (scope == RecurrenceScope.ALL_SERIES && target?.seriesId != null) {
            _scheduledSessions.value.filterNot { it.seriesId == target.seriesId }
        } else {
            _scheduledSessions.value.filterNot { it.id == sessionId }
        }
        saveScheduledSessionsToPrefs()
    }

    fun getNextSessionForCategory(categoryId: String): ScheduledTrainingSession? {
        return _scheduledSessions.value
            .filter { it.categoryId == categoryId && it.status == SessionStatus.SCHEDULED }
            .minByOrNull { it.startTime }
    }

    fun getTodayScheduledSessionForCategory(categoryId: String): ScheduledTrainingSession? {
        val calendar = java.util.Calendar.getInstance()
        val frenchDays = mapOf(
            java.util.Calendar.SATURDAY to "Samedi",
            java.util.Calendar.SUNDAY to "Dimanche",
            java.util.Calendar.MONDAY to "Lundi",
            java.util.Calendar.TUESDAY to "Mardi",
            java.util.Calendar.WEDNESDAY to "Mercredi",
            java.util.Calendar.THURSDAY to "Jeudi",
            java.util.Calendar.FRIDAY to "Vendredi"
        )
        val currentDay = frenchDays[calendar.get(java.util.Calendar.DAY_OF_WEEK)] ?: "Mercredi"
        return _scheduledSessions.value
            .filter { it.categoryId.equals(categoryId, ignoreCase = true) && it.dayOfWeekName.equals(currentDay, ignoreCase = true) && it.status != SessionStatus.CANCELLED }
            .minByOrNull { it.startTime }
    }

    fun checkSessionConflicts(
        newSession: ScheduledTrainingSession,
        excludeSessionId: String? = null
    ): List<SessionConflict> {
        val conflicts = mutableListOf<SessionConflict>()

        val startMin = timeToMinutes(newSession.startTime)
        val endMin = timeToMinutes(newSession.endTime)

        if (startMin < 0 || endMin < 0) {
            conflicts.add(
                SessionConflict(
                    type = ConflictType.INVALID_TIME,
                    message = "Format d'heure invalide. Utilisez le format 24h HH:mm (ex: 17:30).",
                    isBlocking = true
                )
            )
        } else if (endMin <= startMin) {
            conflicts.add(
                SessionConflict(
                    type = ConflictType.INVALID_TIME,
                    message = "L'heure de fin (${newSession.endTime}) doit être postérieure à l'heure de début (${newSession.startTime}).",
                    isBlocking = true
                )
            )
        } else {
            val duration = endMin - startMin
            if (duration < 30) {
                conflicts.add(
                    SessionConflict(
                        type = ConflictType.INVALID_DURATION,
                        message = "La durée minimale d'une séance est de 30 minutes ($duration min actuellement).",
                        isBlocking = true
                    )
                )
            } else if (duration > 240) {
                conflicts.add(
                    SessionConflict(
                        type = ConflictType.INVALID_DURATION,
                        message = "La durée maximale d'une séance est de 4 heures ($duration min actuellement).",
                        isBlocking = true
                    )
                )
            }
        }

        val sameDaySessions = _scheduledSessions.value.filter { s ->
            s.id != excludeSessionId &&
            s.status != SessionStatus.CANCELLED &&
            s.dayOfWeekName.equals(newSession.dayOfWeekName, ignoreCase = true)
        }

        for (existing in sameDaySessions) {
            val eStart = timeToMinutes(existing.startTime)
            val eEnd = timeToMinutes(existing.endTime)
            if (eStart < 0 || eEnd < 0) continue

            val overlaps = (startMin < eEnd && endMin > eStart)
            if (overlaps) {
                if (existing.categoryId == newSession.categoryId) {
                    val isExact = (existing.startTime == newSession.startTime && existing.endTime == newSession.endTime)
                    conflicts.add(
                        SessionConflict(
                            type = ConflictType.CATEGORY_OVERLAP,
                            message = if (isExact) {
                                "Doublon interdit : la catégorie ${newSession.categoryName} a déjà ce créneau le ${newSession.dayOfWeekName} (${existing.startTime} - ${existing.endTime})."
                            } else {
                                "Chevauchement horaire : la catégorie ${newSession.categoryName} a déjà une séance prévue sur ce créneau (${existing.startTime} - ${existing.endTime})."
                            },
                            conflictingSession = existing,
                            isBlocking = true
                        )
                    )
                }

                val commonCoaches = newSession.coachNames.filter { cn ->
                    existing.coachNames.any { it.equals(cn, ignoreCase = true) }
                }
                if (commonCoaches.isNotEmpty()) {
                    conflicts.add(
                        SessionConflict(
                            type = ConflictType.COACH_OVERLAP,
                            message = "${commonCoaches.joinToString(", ")} est déjà affecté(e) à ${existing.categoryName} (${existing.startTime} - ${existing.endTime}).",
                            conflictingSession = existing,
                            isBlocking = false
                        )
                    )
                }
            }
        }

        return conflicts
    }

    private fun timeToMinutes(timeStr: String): Int {
        return try {
            val parts = timeStr.trim().split(":")
            if (parts.size != 2) return -1
            val h = parts[0].trim().toInt()
            val m = parts[1].trim().toInt()
            if (h !in 0..23 || m !in 0..59) return -1
            h * 60 + m
        } catch (_: Exception) {
            -1
        }
    }

    // Club Settings Management
    private val _clubSettings = MutableStateFlow(
        ClubSettings(
            clubName = prefs.getString("club_name", "Judo Club Seddouk") ?: "Judo Club Seddouk",
            dojoAddress = prefs.getString("club_address", "Dojo Municipal de Seddouk, 06000 Seddouk") ?: "Dojo Municipal de Seddouk, 06000 Seddouk",
            phone = prefs.getString("club_phone", "07 82 48 71 20") ?: "07 82 48 71 20",
            season = prefs.getString("club_season", "2024 - 2025") ?: "2024 - 2025",
            openingHours = prefs.getString("club_hours", "Samedi - Jeudi : 09h00 - 21h00") ?: "Samedi - Jeudi : 09h00 - 21h00"
        )
    )
    val clubSettings: StateFlow<ClubSettings> = _clubSettings.asStateFlow()

    fun updateClubSettings(settings: ClubSettings) {
        _clubSettings.value = settings
        prefs.edit()
            .putString("club_name", settings.clubName)
            .putString("club_address", settings.dojoAddress)
            .putString("club_phone", settings.phone)
            .putString("club_season", settings.season)
            .putString("club_hours", settings.openingHours)
            .apply()
    }

    // Admin Profile Management
    private val _adminProfile = MutableStateFlow(
        run {
            val savedPin = prefs.getString("admin_pin", "2026") ?: "2026"
            val effectivePin = if (savedPin == "1234") "2026" else savedPin
            AdminProfile(
                name = prefs.getString("admin_name", "Sensei Ahmed BOURENANE") ?: "Sensei Ahmed BOURENANE",
                phone = prefs.getString("admin_phone", "07 82 48 71 20") ?: "07 82 48 71 20",
                email = prefs.getString("admin_email", "contact@judoclubseddouk.dz") ?: "contact@judoclubseddouk.dz",
                role = prefs.getString("admin_role", "Directeur Technique & Administrateur") ?: "Directeur Technique & Administrateur",
                pinCode = effectivePin,
                photoUri = prefs.getString("admin_photo_uri", null)
            )
        }
    )
    val adminProfile: StateFlow<AdminProfile> = _adminProfile.asStateFlow()

    fun updateAdminProfile(profile: AdminProfile) {
        _adminProfile.value = profile
        prefs.edit()
            .putString("admin_name", profile.name)
            .putString("admin_phone", profile.phone)
            .putString("admin_email", profile.email)
            .putString("admin_role", profile.role)
            .putString("admin_pin", profile.pinCode)
            .putString("admin_photo_uri", profile.photoUri)
            .apply()
    }

    // Admin Notifications
    private val _notifications = MutableStateFlow<List<AdminNotification>>(
        listOf(
            AdminNotification(
                id = "n1",
                title = "Cotisation en attente",
                message = "3 judokas ont une cotisation mensuelle en retard.",
                timeAgo = "Il y a 10 min",
                type = NotificationType.COTISATION
            ),
            AdminNotification(
                id = "n2",
                title = "Séance Benjamins terminée",
                message = "Entraînement terminé avec 18 présents sur 21 (86%).",
                timeAgo = "Hier à 19h00",
                type = NotificationType.SESSION
            ),
            AdminNotification(
                id = "n3",
                title = "Certificats médicaux",
                message = "2 athlètes doivent renouveler leur certificat médical ce mois-ci.",
                timeAgo = "Il y a 2 jours",
                type = NotificationType.MEDICAL
            )
        )
    )
    val notifications: StateFlow<List<AdminNotification>> = _notifications.asStateFlow()

    fun markNotificationRead(id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    fun markAllNotificationsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }

    fun correctSessionAttendance(sessionId: String, memberName: String, markAsPresent: Boolean) {
        _pastSessions.value = _pastSessions.value.map { session ->
            if (session.id == sessionId) {
                if (markAsPresent) {
                    val newAbsents = session.absentMemberNames.filter { it != memberName }
                    val newPresents = if (memberName !in session.presentMemberNames) session.presentMemberNames + memberName else session.presentMemberNames
                    session.copy(
                        presentCount = newPresents.size,
                        absentCount = newAbsents.size,
                        presentMemberNames = newPresents,
                        absentMemberNames = newAbsents
                    )
                } else {
                    val newPresents = session.presentMemberNames.filter { it != memberName }
                    val newAbsents = if (memberName !in session.absentMemberNames) session.absentMemberNames + memberName else session.absentMemberNames
                    session.copy(
                        presentCount = newPresents.size,
                        absentCount = newAbsents.size,
                        presentMemberNames = newPresents,
                        absentMemberNames = newAbsents
                    )
                }
            } else session
        }
    }

    fun selectCategory(category: JudoCategory?) {
        _selectedCategory.value = category
    }

    fun setUserRole(role: UserRole) {
        _userRole.value = role
    }

    fun startSession(session: ActiveTrainingSession) {
        _activeSession.value = session
    }

    fun endSession() {
        _activeSession.value = null
    }

    fun saveFinishedSession(
        session: ActiveTrainingSession,
        endTime: String,
        categoryMembers: List<Member>,
        presentAttendances: List<AttendanceWithMember>
    ) {
        val presentMemberIds = presentAttendances.map { it.memberId }.toSet()
        val presentNames = presentAttendances.map { it.fullName }
        val absentNames = categoryMembers
            .filter { it.id !in presentMemberIds }
            .map { it.fullName }

        val newPastSession = PastTrainingSession(
            categoryId = session.categoryId,
            categoryName = session.categoryName,
            groupCode = _selectedCategory.value?.groupCode ?: "U13",
            dateString = session.startDate,
            startTime = session.startTime,
            endTime = endTime,
            coach1Name = session.coach1Name,
            coach1Present = session.coach1Present,
            coach2Name = session.coach2Name,
            coach2Present = session.coach2Present,
            presentCount = presentAttendances.size,
            absentCount = absentNames.size,
            presentMemberNames = presentNames,
            absentMemberNames = absentNames
        )

        _pastSessions.value = listOf(newPastSession) + _pastSessions.value
        _activeSession.value = null
    }

    val allMembers: StateFlow<List<Member>> = repository.allMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayAttendance: StateFlow<List<AttendanceWithMember>> = repository.getTodayAttendance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentAttendance: StateFlow<List<AttendanceWithMember>> = repository.recentAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAttendance: StateFlow<List<AttendanceWithMember>> = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lastScanned = MutableStateFlow<AttendanceWithMember?>(null)
    val lastScanned: StateFlow<AttendanceWithMember?> = _lastScanned.asStateFlow()

    private val _activeCheckInResult = MutableStateFlow<CheckInResult?>(null)
    val activeCheckInResult: StateFlow<CheckInResult?> = _activeCheckInResult.asStateFlow()

    private val _unassignedQrCode = MutableStateFlow<String?>(null)
    val unassignedQrCode: StateFlow<String?> = _unassignedQrCode.asStateFlow()

    private val _scannedMemberForCreation = MutableStateFlow<ExtractedMemberInfo?>(null)
    val scannedMemberForCreation: StateFlow<ExtractedMemberInfo?> = _scannedMemberForCreation.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGroupFilter = MutableStateFlow("ALL")
    val selectedGroupFilter: StateFlow<String> = _selectedGroupFilter.asStateFlow()

    private var lastScannedCode: String = ""
    private var lastScannedTimestamp: Long = 0L

    init {
        viewModelScope.launch {
            if (!prefs.getBoolean("has_initialized_db", false)) {
                repository.seedInitialDataIfEmpty()
                prefs.edit().putBoolean("has_initialized_db", true).apply()
            }
        }
    }

    fun clearDatabase(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearEntireDatabase()
            prefs.edit().putBoolean("has_initialized_db", true).apply()
            _activeCheckInResult.value = null
            _lastScanned.value = null
            _scannedMemberForCreation.value = null
            _unassignedQrCode.value = null
            onComplete()
        }
    }

    fun setThemePreference(pref: ThemePreference) {
        _themePreference.value = pref
        prefs.edit().putString("theme_pref", pref.name).apply()
    }

    fun toggleTheme() {
        val next = if (_themePreference.value == ThemePreference.DARK) ThemePreference.LIGHT else ThemePreference.DARK
        setThemePreference(next)
    }

    fun onQrScanned(code: String) {
        val now = System.currentTimeMillis()
        val clean = code.trim()
        if (clean.isBlank()) return

        // Debounce exact duplicate scan within 400ms to eliminate hardware key-repeat bounce
        if (clean == lastScannedCode && (now - lastScannedTimestamp) < 400) {
            return
        }
        lastScannedCode = clean
        lastScannedTimestamp = now

        viewModelScope.launch {
            val result = repository.processCheckIn(clean)
            triggerHapticFeedback(result)
            when (result) {
                is CheckInResult.Success -> {
                    _activeCheckInResult.value = result
                    _coachScanFeedback.value = CoachScanFeedback.Success(
                        member = result.member,
                        isCotisationUpToDate = result.member.isCotisationUpToDate,
                        timeString = result.attendance.timeString
                    )
                    playScanSound(true)
                    _lastScanned.value = AttendanceWithMember(
                        attendanceId = result.attendance.id,
                        memberId = result.member.id,
                        firstName = result.member.firstName,
                        lastName = result.member.lastName,
                        groupCode = result.member.groupCode,
                        beltName = result.member.beltName,
                        dateString = result.attendance.dateString,
                        timeString = result.attendance.timeString,
                        timestamp = result.attendance.timestamp,
                        qrCode = result.member.qrCode,
                        phone = result.member.phone
                    )
                }
                is CheckInResult.AlreadyCheckedIn -> {
                    _activeCheckInResult.value = result
                    _coachScanFeedback.value = CoachScanFeedback.AlreadyChecked(
                        member = result.member,
                        timeString = result.existingAttendance.timeString
                    )
                    playScanSound(true)
                }
                is CheckInResult.UnknownQr -> {
                    // Try parsing as sports card with member info!
                    val extracted = CardDataParser.parseCardData(clean)
                    if (extracted.firstName.isNotBlank() || extracted.lastName.isNotBlank()) {
                        _scannedMemberForCreation.value = extracted
                    } else {
                        _unassignedQrCode.value = clean
                    }
                    _activeCheckInResult.value = result
                    _coachScanFeedback.value = CoachScanFeedback.Invalid("QR code non reconnu dans le club")
                    playScanSound(false)
                }
                is CheckInResult.Error -> {
                    _activeCheckInResult.value = result
                    _coachScanFeedback.value = CoachScanFeedback.Invalid(result.message)
                    playScanSound(false)
                }
            }
        }
    }

    fun extractCardDataForMemberCreation(rawCardData: String) {
        val extracted = CardDataParser.parseCardData(rawCardData)
        _scannedMemberForCreation.value = extracted
    }

    fun clearScannedMemberForCreation() {
        _scannedMemberForCreation.value = null
    }

    fun manualCheckIn(memberId: Long) {
        viewModelScope.launch {
            val result = repository.manualCheckIn(memberId)
            triggerHapticFeedback(result)
            _activeCheckInResult.value = result
            when (result) {
                is CheckInResult.Success -> {
                    _coachScanFeedback.value = CoachScanFeedback.Success(
                        member = result.member,
                        isCotisationUpToDate = result.member.isCotisationUpToDate,
                        timeString = result.attendance.timeString
                    )
                    playScanSound(true)
                    _lastScanned.value = AttendanceWithMember(
                        attendanceId = result.attendance.id,
                        memberId = result.member.id,
                        firstName = result.member.firstName,
                        lastName = result.member.lastName,
                        groupCode = result.member.groupCode,
                        beltName = result.member.beltName,
                        dateString = result.attendance.dateString,
                        timeString = result.attendance.timeString,
                        timestamp = result.attendance.timestamp,
                        qrCode = result.member.qrCode,
                        phone = result.member.phone
                    )
                }
                is CheckInResult.AlreadyCheckedIn -> {
                    _coachScanFeedback.value = CoachScanFeedback.AlreadyChecked(
                        member = result.member,
                        timeString = result.existingAttendance.timeString
                    )
                    playScanSound(true)
                }
                else -> {
                    _coachScanFeedback.value = CoachScanFeedback.Invalid("Erreur lors du pointage")
                    playScanSound(false)
                }
            }
        }
    }

    private fun playScanSound(isSuccess: Boolean) {
        try {
            val toneType = if (isSuccess) ToneGenerator.TONE_PROP_BEEP else ToneGenerator.TONE_PROP_NACK
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            toneGenerator.startTone(toneType, 180)
        } catch (_: Exception) {}
    }

    fun recordAttendanceForDate(memberId: Long, dateString: String) {
        viewModelScope.launch {
            repository.recordAttendanceForDate(memberId, dateString)
        }
    }

    fun deleteAttendanceForDate(memberId: Long, dateString: String) {
        viewModelScope.launch {
            repository.deleteAttendanceForMemberAndDate(memberId, dateString)
        }
    }

    fun associateQrToMember(memberId: Long, qrCode: String) {
        viewModelScope.launch {
            repository.associateQrCodeToMember(memberId, qrCode)
            _unassignedQrCode.value = null
            // Automatically check-in after associating!
            val result = repository.processCheckIn(qrCode)
            _activeCheckInResult.value = result
        }
    }

    fun cancelUnassignedQr() {
        _unassignedQrCode.value = null
        if (_activeCheckInResult.value is CheckInResult.UnknownQr) {
            _activeCheckInResult.value = null
        }
    }

    fun dismissCheckInDialog() {
        _activeCheckInResult.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setGroupFilter(groupCode: String) {
        _selectedGroupFilter.value = groupCode
    }

    fun saveMember(member: Member) {
        viewModelScope.launch {
            if (member.id == 0L) {
                val newId = repository.insertMember(member)
                // If member was just created from card, auto check-in them for their session!
                repository.processCheckIn(member.qrCode)
            } else {
                repository.updateMember(member)
            }
        }
    }

    fun deleteMember(member: Member) {
        viewModelScope.launch {
            repository.deleteMember(member)
        }
    }

    fun deleteAttendance(attendanceId: Long) {
        viewModelScope.launch {
            repository.deleteAttendance(attendanceId)
        }
    }

    fun filterAttendancesByPeriod(
        attendances: List<AttendanceWithMember>,
        period: StatPeriod
    ): List<AttendanceWithMember> {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 3600 * 1000L
        val todayStr = repository.getTodayDateString()

        return when (period) {
            StatPeriod.TODAY -> attendances.filter { it.dateString == todayStr }
            StatPeriod.WEEK -> {
                val cutoff = now - (7L * dayMs)
                attendances.filter { it.timestamp >= cutoff }
            }
            StatPeriod.MONTH -> {
                val cutoff = now - (30L * dayMs)
                attendances.filter { it.timestamp >= cutoff }
            }
            StatPeriod.TWO_MONTHS -> {
                val cutoff = now - (60L * dayMs)
                attendances.filter { it.timestamp >= cutoff }
            }
            StatPeriod.SEASON -> attendances
        }
    }

    fun getTopAttendanceList(
        allMembersList: List<Member>,
        allAttendances: List<AttendanceWithMember>,
        period: StatPeriod = StatPeriod.SEASON
    ): List<MemberAttendanceStat> {
        if (allMembersList.isEmpty()) return emptyList()
        val periodAttendances = filterAttendancesByPeriod(allAttendances, period)
        val attendanceCountByMember = periodAttendances.groupBy { it.memberId }
            .mapValues { it.value.size }

        val maxAttendance = attendanceCountByMember.values.maxOrNull() ?: 1
        val baselineTotal = when (period) {
            StatPeriod.TODAY -> 1
            StatPeriod.WEEK -> 3.coerceAtLeast(maxAttendance)
            StatPeriod.MONTH -> 10.coerceAtLeast(maxAttendance)
            StatPeriod.TWO_MONTHS -> 18.coerceAtLeast(maxAttendance)
            StatPeriod.SEASON -> 30.coerceAtLeast(maxAttendance)
        }

        return allMembersList.map { member ->
            val count = attendanceCountByMember[member.id] ?: 0
            val pct = if (count == 0) 0 else ((count.toDouble() / baselineTotal.toDouble()) * 100).toInt().coerceIn(1, 100)
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

    private fun triggerHapticFeedback(result: CheckInResult) {
        try {
            val context = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                val pattern = when (result) {
                    is CheckInResult.Success -> longArrayOf(0, 70, 50, 90)
                    is CheckInResult.AlreadyCheckedIn -> longArrayOf(0, 150)
                    else -> longArrayOf(0, 100, 100, 100)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (_: Exception) {}
    }
}
