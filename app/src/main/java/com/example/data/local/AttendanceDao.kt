package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Attendance
import com.example.data.model.AttendanceWithMember
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attendances: List<Attendance>)

    @Delete
    suspend fun deleteAttendance(attendance: Attendance)

    @Query("DELETE FROM attendance WHERE id = :id")
    suspend fun deleteAttendanceById(id: Long)

    @Query("""
        SELECT a.id AS attendanceId, a.memberId, m.firstName, m.lastName, 
               m.groupCode, m.beltName, a.dateString, a.timeString, a.timestamp, 
               m.qrCode, m.phone, m.photoUrl
        FROM attendance a
        INNER JOIN members m ON a.memberId = m.id
        WHERE a.dateString = :dateString
        ORDER BY a.timestamp DESC
    """)
    fun getAttendanceForDate(dateString: String): Flow<List<AttendanceWithMember>>

    @Query("""
        SELECT a.id AS attendanceId, a.memberId, m.firstName, m.lastName, 
               m.groupCode, m.beltName, a.dateString, a.timeString, a.timestamp, 
               m.qrCode, m.phone, m.photoUrl
        FROM attendance a
        INNER JOIN members m ON a.memberId = m.id
        ORDER BY a.timestamp DESC
        LIMIT :limit
    """)
    fun getRecentAttendance(limit: Int = 15): Flow<List<AttendanceWithMember>>

    @Query("""
        SELECT a.id AS attendanceId, a.memberId, m.firstName, m.lastName, 
               m.groupCode, m.beltName, a.dateString, a.timeString, a.timestamp, 
               m.qrCode, m.phone, m.photoUrl
        FROM attendance a
        INNER JOIN members m ON a.memberId = m.id
        ORDER BY a.timestamp DESC
    """)
    fun getAllAttendanceWithMembers(): Flow<List<AttendanceWithMember>>

    @Query("SELECT * FROM attendance WHERE memberId = :memberId AND dateString = :dateString LIMIT 1")
    suspend fun findTodayAttendance(memberId: Long, dateString: String): Attendance?

    @Query("SELECT * FROM attendance WHERE memberId = :memberId ORDER BY timestamp DESC")
    fun getAttendanceForMember(memberId: Long): Flow<List<Attendance>>

    @Query("SELECT COUNT(*) FROM attendance WHERE dateString = :dateString")
    fun countAttendanceForDate(dateString: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendance WHERE memberId = :memberId")
    fun countAttendanceForMember(memberId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendance")
    suspend fun countTotalAttendance(): Int

    @Query("SELECT * FROM attendance ORDER BY timestamp DESC")
    suspend fun getAllAttendancesDirect(): List<Attendance>

    @Query("DELETE FROM attendance")
    suspend fun deleteAllAttendance()
}
