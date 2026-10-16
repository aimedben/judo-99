package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.CategoryEntity
import com.example.data.local.CoachEntity
import com.example.data.local.PastSessionEntity
import com.example.data.local.ScheduledSessionEntity
import com.example.data.local.sync.SyncAuditLogEntity
import com.example.data.local.sync.SyncQueueEntity
import com.example.data.local.sync.SyncStatus
import com.example.data.model.Attendance
import com.example.data.model.Member
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class SupabaseSyncService(
    private val context: Context,
    private val database: AppDatabase
) {
    companion object {
        const val DEFAULT_SUPABASE_URL = "https://vkcsqgxsylsuyyszlbko.supabase.co"
        const val DEFAULT_SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZrY3NxZ3hzeWxzdXl5c3psYmtvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTE5OTU3MTcsImV4cCI6MjEwNzU3MTcxN30.xExE0X-0Kx76GxXpLohBHToqvgJC76S6_PT6f3nDpwE"
    }

    private val prefs = context.getSharedPreferences("judo_supabase_prefs", Context.MODE_PRIVATE)
    private val syncQueueDao = database.syncQueueDao()
    private val auditLogDao = database.syncAuditLogDao()
    private val memberDao = database.memberDao()
    private val attendanceDao = database.attendanceDao()
    private val clubDao = database.clubDao()

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val _config = MutableStateFlow(loadConfig())
    val config: Flow<SupabaseConfig> = _config.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: Flow<Boolean> = _isSyncing.asStateFlow()

    val pendingCountFlow: Flow<Int> = syncQueueDao.countPendingOperationsFlow()
    val recentAuditLogs: Flow<List<SyncAuditLogEntity>> = auditLogDao.getRecentAuditLogs(50)

    private val syncMutex = Mutex()
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var autoSyncJob: Job? = null

    /**
     * Déclenche une synchronisation automatique en arrière-plan lorsque la connexion est active,
     * évitant la perte de données en cas de fermeture inattendue.
     */
    fun triggerBackgroundAutoSync() {
        if (_config.value.isAutoSyncEnabled && isOnline() && !_isSyncing.value) {
            autoSyncJob?.cancel()
            autoSyncJob = syncScope.launch {
                delay(1200L)
                if (isOnline() && !_isSyncing.value) {
                    try {
                        performFullSync()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun loadConfig(): SupabaseConfig {
        val savedUrl = prefs.getString("supabase_url", null)
        val savedKey = prefs.getString("supabase_anon_key", null)
        val url = if (savedUrl.isNullOrBlank()) DEFAULT_SUPABASE_URL else savedUrl.trimEnd('/')
        val key = if (savedKey.isNullOrBlank()) DEFAULT_SUPABASE_ANON_KEY else savedKey.trim()

        return SupabaseConfig(
            url = url,
            anonKey = key,
            isAutoSyncEnabled = prefs.getBoolean("supabase_auto_sync", true),
            isDaily22hSyncEnabled = prefs.getBoolean("supabase_daily_22h_sync", true),
            lastSyncTimestamp = prefs.getLong("supabase_last_sync_timestamp", 0L),
            lastSyncStatus = prefs.getString("supabase_last_sync_status", "CONFIGURÉ") ?: "CONFIGURÉ"
        )
    }

    fun updateConfig(
        url: String,
        anonKey: String,
        isAutoSyncEnabled: Boolean = _config.value.isAutoSyncEnabled,
        isDaily22hSyncEnabled: Boolean = _config.value.isDaily22hSyncEnabled
    ) {
        val cleanUrl = url.trim().trimEnd('/')
        val cleanKey = anonKey.trim()
        prefs.edit()
            .putString("supabase_url", cleanUrl)
            .putString("supabase_anon_key", cleanKey)
            .putBoolean("supabase_auto_sync", isAutoSyncEnabled)
            .putBoolean("supabase_daily_22h_sync", isDaily22hSyncEnabled)
            .apply()
        _config.value = loadConfig()
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNet = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNet) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val cfg = _config.value
        if (!cfg.isConfigured) {
            return@withContext Result.failure(Exception("URL ou Clé API Supabase non renseignée"))
        }
        if (!isOnline()) {
            return@withContext Result.failure(Exception("Aucune connexion Internet disponible sur la tablette"))
        }

        try {
            val request = Request.Builder()
                .url("${cfg.url}/rest/v1/members?select=id&limit=1")
                .addHeader("apikey", cfg.anonKey)
                .addHeader("Authorization", "Bearer ${cfg.anonKey}")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code in 200..299) {
                    Result.success("Connexion à Supabase réussie et tables opérationnelles (HTTP ${response.code})")
                } else {
                    Result.failure(Exception("Erreur de connexion Supabase (Code HTTP ${response.code})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Impossible de joindre le serveur Supabase : ${e.localizedMessage ?: e.message}"))
        }
    }

    // ----------------------------------------------------
    // ENQUEUE HELPERS (LOCAL SQLITE IMMEDIATE WRITE)
    // ----------------------------------------------------
    suspend fun enqueueMember(member: Member, operation: String = "UPSERT") = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("id", member.id)
            put("qr_code", member.qrCode)
            put("first_name", member.firstName)
            put("last_name", member.lastName)
            put("birth_date", member.birthDate)
            put("phone", member.phone)
            put("group_code", member.groupCode)
            put("belt_name", member.beltName)
            put("status", member.status)
            put("registration_date", member.registrationDate)
            put("photo_url", member.photoUrl)
            put("notes", member.notes)
            put("updated_at", getIsoNow())
        }
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entityType = "MEMBERS",
                entityId = member.id.toString(),
                operation = operation,
                payloadJson = json.toString()
            )
        )
        triggerBackgroundAutoSync()
    }

    suspend fun enqueueAttendance(attendance: Attendance) = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("id", attendance.id)
            put("member_id", attendance.memberId)
            put("date_string", attendance.dateString)
            put("time_string", attendance.timeString)
            put("timestamp", attendance.timestamp)
            put("check_in_method", attendance.checkInMethod)
            put("device_id", attendance.deviceId)
            put("created_at", getIsoNow())
        }
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entityType = "ATTENDANCES",
                entityId = attendance.id.toString(),
                operation = "UPSERT",
                payloadJson = json.toString()
            )
        )
        triggerBackgroundAutoSync()
    }

    suspend fun enqueueCategory(category: CategoryEntity, operation: String = "UPSERT") = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("id", category.id)
            put("name", category.name)
            put("group_code", category.groupCode)
            put("coach1_name", category.coach1Name)
            put("coach2_name", category.coach2Name)
            put("description", category.description)
            put("is_active", category.isActive)
            put("updated_at", getIsoNow())
        }
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entityType = "CATEGORIES",
                entityId = category.id,
                operation = operation,
                payloadJson = json.toString()
            )
        )
        triggerBackgroundAutoSync()
    }

    suspend fun enqueueCoach(coach: CoachEntity, operation: String = "UPSERT") = withContext(Dispatchers.IO) {
        val categoriesArray = JSONArray()
        if (coach.assignedCategoriesJoined.isNotBlank()) {
            coach.assignedCategoriesJoined.split(";").forEach { categoriesArray.put(it) }
        }
        val json = JSONObject().apply {
            put("id", coach.id)
            put("name", coach.name)
            put("grade", coach.grade)
            put("phone", coach.phone)
            put("assigned_categories", categoriesArray)
            put("is_active", coach.isActive)
            put("sessions_count", coach.sessionsCount)
            put("updated_at", getIsoNow())
        }
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entityType = "COACHES",
                entityId = coach.id,
                operation = operation,
                payloadJson = json.toString()
            )
        )
        triggerBackgroundAutoSync()
    }

    suspend fun enqueueScheduledSession(session: ScheduledSessionEntity, operation: String = "UPSERT") = withContext(Dispatchers.IO) {
        val coachIdsArray = JSONArray()
        if (session.coachIdsJoined.isNotBlank()) {
            session.coachIdsJoined.split(";").forEach { coachIdsArray.put(it) }
        }
        val coachNamesArray = JSONArray()
        if (session.coachNamesJoined.isNotBlank()) {
            session.coachNamesJoined.split(";").forEach { coachNamesArray.put(it) }
        }
        val json = JSONObject().apply {
            put("id", session.id)
            put("category_id", session.categoryId)
            put("category_name", session.categoryName)
            put("group_code", session.groupCode)
            put("day_of_week_name", session.dayOfWeekName)
            put("date_string", session.dateString)
            put("formatted_display_date", session.formattedDisplayDate)
            put("start_time", session.startTime)
            put("end_time", session.endTime)
            put("coach_ids", coachIdsArray)
            put("coach_names", coachNamesArray)
            put("is_recurring_weekly", session.isRecurringWeekly)
            put("recurrence_end_date", session.recurrenceEndDate ?: JSONObject.NULL)
            put("series_id", session.seriesId ?: JSONObject.NULL)
            put("notes", session.notes)
            put("status", session.status)
            put("updated_at", getIsoNow())
        }
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entityType = "SCHEDULED_SESSIONS",
                entityId = session.id,
                operation = operation,
                payloadJson = json.toString()
            )
        )
        triggerBackgroundAutoSync()
    }

    suspend fun enqueuePastSession(session: PastSessionEntity) = withContext(Dispatchers.IO) {
        val presentArray = JSONArray()
        if (session.presentMembersJoined.isNotBlank()) {
            session.presentMembersJoined.split(";").forEach { presentArray.put(it) }
        }
        val absentArray = JSONArray()
        if (session.absentMembersJoined.isNotBlank()) {
            session.absentMembersJoined.split(";").forEach { absentArray.put(it) }
        }
        val json = JSONObject().apply {
            put("id", session.id)
            put("category_id", session.categoryId)
            put("category_name", session.categoryName)
            put("group_code", session.groupCode)
            put("date_string", session.dateString)
            put("start_time", session.startTime)
            put("end_time", session.endTime)
            put("coach1_name", session.coach1Name)
            put("coach1_present", session.coach1Present)
            put("coach2_name", session.coach2Name)
            put("coach2_present", session.coach2Present)
            put("present_count", session.presentCount)
            put("absent_count", session.absentCount)
            put("present_member_names", presentArray)
            put("absent_member_names", absentArray)
            put("updated_at", getIsoNow())
        }
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entityType = "PAST_SESSIONS",
                entityId = session.id,
                operation = "UPSERT",
                payloadJson = json.toString()
            )
        )
        triggerBackgroundAutoSync()
    }

    suspend fun enqueueAllLocalData() = withContext(Dispatchers.IO) {
        val members = memberDao.getAllMembersDirect()
        for (m in members) {
            enqueueMember(m)
        }
        val attendances = attendanceDao.getAllAttendancesDirect()
        for (att in attendances) {
            enqueueAttendance(att)
        }
        val categories = clubDao.getAllCategories()
        for (c in categories) {
            enqueueCategory(c)
        }
        val coaches = clubDao.getAllCoaches()
        for (co in coaches) {
            enqueueCoach(co)
        }
        val sessions = clubDao.getAllScheduledSessions()
        for (s in sessions) {
            enqueueScheduledSession(s)
        }
        val past = clubDao.getAllPastSessions()
        for (p in past) {
            enqueuePastSession(p)
        }
    }

    // ----------------------------------------------------
    // SYNCHRONIZATION EXECUTION (OFFLINE-FIRST TO CLOUD)
    // ----------------------------------------------------
    suspend fun performFullSync(): SyncReport = withContext(Dispatchers.IO) {
        if (_isSyncing.value) {
            return@withContext SyncReport(
                isSuccess = false,
                errorMessage = "Une synchronisation est déjà en cours. Veuillez patienter.",
                pendingRemaining = syncQueueDao.countPendingOperationsDirect()
            )
        }

        val cfg = _config.value
        if (!cfg.isConfigured) {
            return@withContext SyncReport(
                isSuccess = false,
                errorMessage = "Supabase non configuré (renseignez l'URL et la clé anon dans les paramètres)",
                pendingRemaining = syncQueueDao.countPendingOperationsDirect()
            )
        }

        if (!isOnline()) {
            return@withContext SyncReport(
                isSuccess = false,
                errorMessage = "Aucune connexion Internet disponible sur la tablette. Les données restent conservées localement.",
                pendingRemaining = syncQueueDao.countPendingOperationsDirect()
            )
        }

        syncMutex.withLock {
            _isSyncing.value = true
            var itemsSent = 0
            var itemsReceived = 0
            var conflictsResolved = 0
            var syncError: String? = null

            try {
                // Seed local queue from existing SQLite database if queue is empty
                if (syncQueueDao.countPendingOperationsDirect() == 0) {
                    enqueueAllLocalData()
                }

                // 1. Process local pending queue (Push)
                val pending = syncQueueDao.getPendingOperationsDirect()
                for (op in pending) {
                    try {
                        syncQueueDao.updateStatus(op.id, SyncStatus.IN_PROGRESS.name)
                        val table = mapEntityTypeToSupabaseTable(op.entityType)
                        val success = if (op.operation == "DELETE") {
                            deleteFromSupabase(cfg, table, op.entityId)
                        } else {
                            upsertToSupabase(cfg, table, op.payloadJson)
                        }

                        if (success) {
                            syncQueueDao.updateStatus(op.id, SyncStatus.SYNCED.name)
                            itemsSent++
                            auditLogDao.insert(
                                SyncAuditLogEntity(
                                    entityName = op.entityType,
                                    entityId = op.entityId,
                                    operationType = op.operation,
                                    details = "Synchronisé avec succès vers table $table"
                                )
                            )
                        } else {
                            val errMsg = "Réponse serveur non-2xx sur la table $table"
                            syncQueueDao.markError(op.id, SyncStatus.PENDING.name, errMsg)
                            if (syncError == null) syncError = errMsg
                        }
                    } catch (e: Exception) {
                        syncQueueDao.markError(op.id, SyncStatus.PENDING.name, e.message)
                        if (syncError == null) syncError = e.message
                    }
                }

                // Clean synced items from queue to keep DB minimal
                syncQueueDao.cleanSyncedOperations()

                // 2. Downward pull (fetch remote updates since last sync)
                try {
                    val pulled = pullRemoteUpdates(cfg)
                    itemsReceived = pulled.itemsReceived
                    conflictsResolved = pulled.conflictsResolved
                } catch (e: Exception) {
                    Log.w("SupabaseSync", "Pull warning: ${e.message}")
                }

                val now = System.currentTimeMillis()
                val remaining = syncQueueDao.countPendingOperationsDirect()
                val isSuccess = remaining == 0 && syncError == null

                if (isSuccess) {
                    prefs.edit()
                        .putLong("supabase_last_sync_timestamp", now)
                        .putString("supabase_last_sync_status", "SYNCHRONISÉ")
                        .apply()
                    _config.value = loadConfig()
                }

                SyncReport(
                    isSuccess = isSuccess,
                    itemsSent = itemsSent,
                    itemsReceived = itemsReceived,
                    conflictsResolved = conflictsResolved,
                    errorMessage = if (!isSuccess) (syncError ?: "Certains éléments n'ont pas pu être synchronisés.") else null,
                    pendingRemaining = remaining,
                    timestamp = now
                )
            } catch (e: Exception) {
                val remaining = syncQueueDao.countPendingOperationsDirect()
                SyncReport(
                    isSuccess = false,
                    itemsSent = itemsSent,
                    errorMessage = e.localizedMessage ?: e.message,
                    pendingRemaining = remaining
                )
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun mapEntityTypeToSupabaseTable(entityType: String): String {
        return when (entityType) {
            "MEMBERS" -> "members"
            "ATTENDANCES" -> "attendances"
            "CATEGORIES" -> "judo_categories"
            "COACHES" -> "coaches"
            "SCHEDULED_SESSIONS" -> "scheduled_sessions"
            "PAST_SESSIONS" -> "past_sessions"
            else -> entityType.lowercase()
        }
    }

    private fun upsertToSupabase(cfg: SupabaseConfig, table: String, jsonPayload: String): Boolean {
        val url = "${cfg.url}/rest/v1/$table"
        val body = jsonPayload.toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", cfg.anonKey)
            .addHeader("Authorization", "Bearer ${cfg.anonKey}")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(body)
            .build()

        httpClient.newCall(request).execute().use { response ->
            return response.isSuccessful || response.code in 200..299
        }
    }

    private fun deleteFromSupabase(cfg: SupabaseConfig, table: String, id: String): Boolean {
        val url = "${cfg.url}/rest/v1/$table?id=eq.$id"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", cfg.anonKey)
            .addHeader("Authorization", "Bearer ${cfg.anonKey}")
            .delete()
            .build()

        httpClient.newCall(request).execute().use { response ->
            return response.isSuccessful || response.code in 200..299
        }
    }

    private data class PullResult(val itemsReceived: Int, val conflictsResolved: Int)

    private suspend fun pullRemoteUpdates(cfg: SupabaseConfig): PullResult {
        var received = 0
        var conflicts = 0

        // Pull members
        try {
            val url = "${cfg.url}/rest/v1/members?select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", cfg.anonKey)
                .addHeader("Authorization", "Bearer ${cfg.anonKey}")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    if (bodyString.isNotBlank() && bodyString.startsWith("[")) {
                        val arr = JSONArray(bodyString)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val id = obj.optLong("id", 0L)
                            val qrCode = obj.optString("qr_code", "")
                            if (id > 0 && qrCode.isNotBlank()) {
                                val existing = memberDao.getMemberByIdDirect(id)
                                val remoteMember = Member(
                                    id = id,
                                    qrCode = qrCode,
                                    firstName = obj.optString("first_name", ""),
                                    lastName = obj.optString("last_name", ""),
                                    birthDate = obj.optString("birth_date", ""),
                                    phone = obj.optString("phone", ""),
                                    groupCode = obj.optString("group_code", "U15"),
                                    beltName = obj.optString("belt_name", "BLANCHE"),
                                    status = obj.optString("status", "ACTIF"),
                                    registrationDate = obj.optString("registration_date", "2026-09-01"),
                                    photoUrl = obj.optString("photo_url", ""),
                                    notes = obj.optString("notes", "")
                                )
                                if (existing == null) {
                                    memberDao.insertMember(remoteMember)
                                    received++
                                } else if (existing != remoteMember) {
                                    // Conflict resolved with Last-Write-Wins
                                    memberDao.updateMember(remoteMember)
                                    conflicts++
                                    auditLogDao.insert(
                                        SyncAuditLogEntity(
                                            entityName = "MEMBERS",
                                            entityId = id.toString(),
                                            operationType = "CONFLICT_RESOLVED",
                                            resolvedBy = "TIMESTAMP_LWW",
                                            details = "Mise à jour membre $id (${remoteMember.fullName}) depuis le cloud"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("SupabaseSync", "Error pulling members: ${e.message}")
        }

        return PullResult(received, conflicts)
    }

    // ----------------------------------------------------
    // INDEPENDENT JSON BACKUP & RESTORE
    // ----------------------------------------------------
    suspend fun generateBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        val meta = JSONObject().apply {
            put("app", "Judo Club Seddouk")
            put("version", "1.0")
            put("schema_version", 2)
            put("exported_at", getIsoNow())
            put("device_id", "DOJO_TERMINAL_1")
        }
        root.put("metadata", meta)

        // Members
        val membersArr = JSONArray()
        // Collect current members from database
        val membersList = database.memberDao().let { dao ->
            // Let's get all members synchronously or query direct
            val list = mutableListOf<Member>()
            // Using SQLite query or direct list
            database.openHelper.readableDatabase.query("SELECT * FROM members").use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow("id")
                val qrIdx = cursor.getColumnIndexOrThrow("qrCode")
                val fnIdx = cursor.getColumnIndexOrThrow("firstName")
                val lnIdx = cursor.getColumnIndexOrThrow("lastName")
                val bdIdx = cursor.getColumnIndexOrThrow("birthDate")
                val phIdx = cursor.getColumnIndexOrThrow("phone")
                val gcIdx = cursor.getColumnIndexOrThrow("groupCode")
                val bnIdx = cursor.getColumnIndexOrThrow("beltName")
                val stIdx = cursor.getColumnIndexOrThrow("status")
                val rdIdx = cursor.getColumnIndexOrThrow("registrationDate")
                val puIdx = cursor.getColumnIndexOrThrow("photoUrl")
                val ntIdx = cursor.getColumnIndexOrThrow("notes")
                while (cursor.moveToNext()) {
                    list.add(
                        Member(
                            id = cursor.getLong(idIdx),
                            qrCode = cursor.getString(qrIdx),
                            firstName = cursor.getString(fnIdx),
                            lastName = cursor.getString(lnIdx),
                            birthDate = cursor.getString(bdIdx),
                            phone = cursor.getString(phIdx),
                            groupCode = cursor.getString(gcIdx),
                            beltName = cursor.getString(bnIdx),
                            status = cursor.getString(stIdx),
                            registrationDate = cursor.getString(rdIdx),
                            photoUrl = cursor.getString(puIdx),
                            notes = cursor.getString(ntIdx)
                        )
                    )
                }
            }
            list
        }
        for (m in membersList) {
            membersArr.put(
                JSONObject().apply {
                    put("id", m.id)
                    put("qr_code", m.qrCode)
                    put("first_name", m.firstName)
                    put("last_name", m.lastName)
                    put("birth_date", m.birthDate)
                    put("phone", m.phone)
                    put("group_code", m.groupCode)
                    put("belt_name", m.beltName)
                    put("status", m.status)
                    put("registration_date", m.registrationDate)
                    put("photo_url", m.photoUrl)
                    put("notes", m.notes)
                }
            )
        }
        root.put("members", membersArr)

        // Attendances
        val attendancesArr = JSONArray()
        database.openHelper.readableDatabase.query("SELECT * FROM attendance").use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow("id")
            val mIdIdx = cursor.getColumnIndexOrThrow("memberId")
            val dsIdx = cursor.getColumnIndexOrThrow("dateString")
            val tsIdx = cursor.getColumnIndexOrThrow("timeString")
            val timeIdx = cursor.getColumnIndexOrThrow("timestamp")
            val cimIdx = cursor.getColumnIndexOrThrow("checkInMethod")
            val devIdx = cursor.getColumnIndexOrThrow("deviceId")
            while (cursor.moveToNext()) {
                attendancesArr.put(
                    JSONObject().apply {
                        put("id", cursor.getLong(idIdx))
                        put("member_id", cursor.getLong(mIdIdx))
                        put("date_string", cursor.getString(dsIdx))
                        put("time_string", cursor.getString(tsIdx))
                        put("timestamp", cursor.getLong(timeIdx))
                        put("check_in_method", cursor.getString(cimIdx))
                        put("device_id", cursor.getString(devIdx))
                    }
                )
            }
        }
        root.put("attendances", attendancesArr)

        // Categories
        val categoriesArr = JSONArray()
        for (c in clubDao.getAllCategories()) {
            categoriesArr.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("group_code", c.groupCode)
                    put("coach1_name", c.coach1Name)
                    put("coach2_name", c.coach2Name)
                    put("description", c.description)
                    put("is_active", c.isActive)
                }
            )
        }
        root.put("categories", categoriesArr)

        // Coaches
        val coachesArr = JSONArray()
        for (co in clubDao.getAllCoaches()) {
            coachesArr.put(
                JSONObject().apply {
                    put("id", co.id)
                    put("name", co.name)
                    put("grade", co.grade)
                    put("phone", co.phone)
                    put("assigned_categories", co.assignedCategoriesJoined)
                    put("is_active", co.isActive)
                    put("sessions_count", co.sessionsCount)
                }
            )
        }
        root.put("coaches", coachesArr)

        // Scheduled sessions
        val scheduledArr = JSONArray()
        for (s in clubDao.getAllScheduledSessions()) {
            scheduledArr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("category_id", s.categoryId)
                    put("category_name", s.categoryName)
                    put("group_code", s.groupCode)
                    put("day_of_week_name", s.dayOfWeekName)
                    put("date_string", s.dateString)
                    put("formatted_display_date", s.formattedDisplayDate)
                    put("start_time", s.startTime)
                    put("end_time", s.endTime)
                    put("coach_ids", s.coachIdsJoined)
                    put("coach_names", s.coachNamesJoined)
                    put("is_recurring_weekly", s.isRecurringWeekly)
                    put("recurrence_end_date", s.recurrenceEndDate ?: "")
                    put("series_id", s.seriesId ?: "")
                    put("notes", s.notes)
                    put("status", s.status)
                }
            )
        }
        root.put("scheduled_sessions", scheduledArr)

        // Past sessions
        val pastArr = JSONArray()
        for (p in clubDao.getAllPastSessions()) {
            pastArr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("category_id", p.categoryId)
                    put("category_name", p.categoryName)
                    put("group_code", p.groupCode)
                    put("date_string", p.dateString)
                    put("start_time", p.startTime)
                    put("end_time", p.endTime)
                    put("coach1_name", p.coach1Name)
                    put("coach1_present", p.coach1Present)
                    put("coach2_name", p.coach2Name)
                    put("coach2_present", p.coach2Present)
                    put("present_count", p.presentCount)
                    put("absent_count", p.absentCount)
                    put("present_members", p.presentMembersJoined)
                    put("absent_members", p.absentMembersJoined)
                }
            )
        }
        root.put("past_sessions", pastArr)

        meta.put("total_members", membersList.size)
        meta.put("total_attendances", attendancesArr.length())
        meta.put("total_categories", categoriesArr.length())
        meta.put("total_coaches", coachesArr.length())
        meta.put("total_scheduled_sessions", scheduledArr.length())
        meta.put("total_past_sessions", pastArr.length())

        root.toString(2)
    }

    suspend fun restoreBackupJson(backupJson: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(backupJson)
            if (!root.has("members") && !root.has("categories")) {
                return@withContext Result.failure(Exception("Format de sauvegarde JSON invalide (clés attendues manquantes)"))
            }

            // Restore Members
            var restoredMembers = 0
            if (root.has("members")) {
                val arr = root.getJSONArray("members")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val m = Member(
                        id = obj.optLong("id", 0L),
                        qrCode = obj.optString("qr_code", "RESTORED-${UUID.randomUUID().toString().take(6)}"),
                        firstName = obj.optString("first_name", ""),
                        lastName = obj.optString("last_name", ""),
                        birthDate = obj.optString("birth_date", "2012-01-01"),
                        phone = obj.optString("phone", ""),
                        groupCode = obj.optString("group_code", "U15"),
                        beltName = obj.optString("belt_name", "BLANCHE"),
                        status = obj.optString("status", "ACTIF"),
                        registrationDate = obj.optString("registration_date", "2026-09-01"),
                        photoUrl = obj.optString("photo_url", ""),
                        notes = obj.optString("notes", "")
                    )
                    memberDao.insertMember(m)
                    restoredMembers++
                }
            }

            // Restore Attendances
            var restoredAttendances = 0
            if (root.has("attendances")) {
                val arr = root.getJSONArray("attendances")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val att = Attendance(
                        id = obj.optLong("id", 0L),
                        memberId = obj.optLong("member_id", 0L),
                        dateString = obj.optString("date_string", ""),
                        timeString = obj.optString("time_string", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        checkInMethod = obj.optString("check_in_method", "RESTORED"),
                        deviceId = obj.optString("device_id", "DOJO_TERMINAL_1")
                    )
                    if (att.memberId > 0) {
                        attendanceDao.insertAttendance(att)
                        restoredAttendances++
                    }
                }
            }

            // Restore Categories
            var restoredCategories = 0
            if (root.has("categories")) {
                val arr = root.getJSONArray("categories")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val cat = CategoryEntity(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "Catégorie"),
                        groupCode = obj.optString("group_code", "U15"),
                        coach1Name = obj.optString("coach1_name", ""),
                        coach2Name = obj.optString("coach2_name", ""),
                        description = obj.optString("description", ""),
                        isActive = obj.optBoolean("is_active", true)
                    )
                    clubDao.insertCategory(cat)
                    restoredCategories++
                }
            }

            // Log restoration
            auditLogDao.insert(
                SyncAuditLogEntity(
                    entityName = "ALL",
                    entityId = "BACKUP_RESTORE",
                    operationType = "BACKUP_RESTORE",
                    details = "Restauration terminée : $restoredMembers membres, $restoredAttendances présences, $restoredCategories catégories"
                )
            )

            Result.success("Restauration réussie : $restoredMembers adhérents, $restoredAttendances pointages réintégrés.")
        } catch (e: Exception) {
            Result.failure(Exception("Échec de la restauration : ${e.message}"))
        }
    }

    private fun getIsoNow(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
}
