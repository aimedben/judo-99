package com.example.data.local.sync

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    fun getPendingOperationsFlow(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingOperationsDirect(): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    fun countPendingOperationsFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    suspend fun countPendingOperationsDirect(): Int

    @Query("SELECT * FROM sync_queue ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentQueue(limit: Int = 50): Flow<List<SyncQueueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(operation: SyncQueueEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueAll(operations: List<SyncQueueEntity>)

    @Update
    suspend fun update(operation: SyncQueueEntity)

    @Query("UPDATE sync_queue SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE sync_queue SET status = :status, lastError = :error, retryCount = retryCount + 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markError(id: Long, status: String, error: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun cleanSyncedOperations()

    @Delete
    suspend fun delete(operation: SyncQueueEntity)

    @Query("DELETE FROM sync_queue")
    suspend fun clearQueue()
}

@Dao
interface SyncAuditLogDao {
    @Query("SELECT * FROM sync_audit_log ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAuditLogs(limit: Int = 100): Flow<List<SyncAuditLogEntity>>

    @Query("SELECT * FROM sync_audit_log ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentAuditLogsDirect(limit: Int = 100): List<SyncAuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SyncAuditLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<SyncAuditLogEntity>)

    @Query("DELETE FROM sync_audit_log")
    suspend fun clearAuditLog()
}
