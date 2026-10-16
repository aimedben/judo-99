package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClubDao {
    // Categories
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun countCategories(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    // Coaches
    @Query("SELECT * FROM coaches ORDER BY name ASC")
    fun getAllCoachesFlow(): Flow<List<CoachEntity>>

    @Query("SELECT * FROM coaches ORDER BY name ASC")
    suspend fun getAllCoaches(): List<CoachEntity>

    @Query("SELECT COUNT(*) FROM coaches")
    suspend fun countCoaches(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoach(coach: CoachEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoaches(coaches: List<CoachEntity>)

    @Update
    suspend fun updateCoach(coach: CoachEntity)

    @Delete
    suspend fun deleteCoach(coach: CoachEntity)

    @Query("DELETE FROM coaches WHERE id = :id")
    suspend fun deleteCoachById(id: String)

    // Scheduled Sessions
    @Query("SELECT * FROM scheduled_sessions ORDER BY dateString ASC, startTime ASC")
    fun getAllScheduledSessionsFlow(): Flow<List<ScheduledSessionEntity>>

    @Query("SELECT * FROM scheduled_sessions ORDER BY dateString ASC, startTime ASC")
    suspend fun getAllScheduledSessions(): List<ScheduledSessionEntity>

    @Query("SELECT COUNT(*) FROM scheduled_sessions")
    suspend fun countScheduledSessions(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledSession(session: ScheduledSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledSessions(sessions: List<ScheduledSessionEntity>)

    @Update
    suspend fun updateScheduledSession(session: ScheduledSessionEntity)

    @Query("DELETE FROM scheduled_sessions WHERE id = :id")
    suspend fun deleteScheduledSessionById(id: String)

    @Query("DELETE FROM scheduled_sessions WHERE seriesId = :seriesId")
    suspend fun deleteScheduledSessionSeries(seriesId: String)

    // Past Sessions
    @Query("SELECT * FROM past_sessions ORDER BY dateString DESC, startTime DESC")
    fun getAllPastSessionsFlow(): Flow<List<PastSessionEntity>>

    @Query("SELECT * FROM past_sessions ORDER BY dateString DESC, startTime DESC")
    suspend fun getAllPastSessions(): List<PastSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPastSession(session: PastSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPastSessions(sessions: List<PastSessionEntity>)

    @Query("DELETE FROM past_sessions WHERE id = :id")
    suspend fun deletePastSessionById(id: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Query("DELETE FROM coaches")
    suspend fun deleteAllCoaches()

    @Query("DELETE FROM scheduled_sessions")
    suspend fun deleteAllScheduledSessions()

    @Query("DELETE FROM past_sessions")
    suspend fun deleteAllPastSessions()
}
