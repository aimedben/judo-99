package com.example.data.local.sync

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class SyncStatus {
    PENDING,
    IN_PROGRESS,
    SYNCED,
    ERROR
}

@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["status"]),
        Index(value = ["entityType", "entityId"])
    ]
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val queueId: String = UUID.randomUUID().toString(),
    val entityType: String, // "MEMBERS", "ATTENDANCES", "CATEGORIES", "COACHES", "SCHEDULED_SESSIONS", "PAST_SESSIONS"
    val entityId: String,
    val operation: String, // "UPSERT", "DELETE"
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: String = SyncStatus.PENDING.name,
    val retryCount: Int = 0,
    val lastError: String? = null
)

@Entity(
    tableName = "sync_audit_log",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["entityName", "entityId"])
    ]
)
data class SyncAuditLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val deviceId: String = "DOJO_TERMINAL_1",
    val entityName: String,
    val entityId: String,
    val operationType: String, // "UPSERT", "DELETE", "CONFLICT_RESOLVED", "BACKUP_RESTORE"
    val resolvedBy: String = "TIMESTAMP_LWW",
    val timestamp: Long = System.currentTimeMillis(),
    val details: String = ""
)
