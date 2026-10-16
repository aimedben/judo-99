package com.example.data.sync

data class SupabaseConfig(
    val url: String = "https://vkcsqgxsylsuyyszlbko.supabase.co",
    val anonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZrY3NxZ3hzeWxzdXl5c3psYmtvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTE5OTU3MTcsImV4cCI6MjEwNzU3MTcxN30.xExE0X-0Kx76GxXpLohBHToqvgJC76S6_PT6f3nDpwE",
    val isAutoSyncEnabled: Boolean = true,
    val isDaily22hSyncEnabled: Boolean = true,
    val lastSyncTimestamp: Long = 0L,
    val lastSyncStatus: String = "CONFIGURÉ"
) {
    val isConfigured: Boolean
        get() = url.isNotBlank() && anonKey.isNotBlank() && url.startsWith("https://")
}

data class SyncReport(
    val isSuccess: Boolean,
    val itemsSent: Int = 0,
    val itemsReceived: Int = 0,
    val conflictsResolved: Int = 0,
    val pendingRemaining: Int = 0,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class BackupMetadata(
    val appVersion: String = "1.0",
    val schemaVersion: Int = 2,
    val clubName: String = "Judo Club Seddouk",
    val exportedAt: String,
    val totalMembers: Int,
    val totalAttendances: Int,
    val totalCategories: Int,
    val totalCoaches: Int,
    val totalScheduledSessions: Int,
    val totalPastSessions: Int
)
