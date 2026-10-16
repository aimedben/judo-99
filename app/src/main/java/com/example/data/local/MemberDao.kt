package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Member
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY lastName ASC, firstName ASC")
    fun getAllMembers(): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    fun getMemberById(id: Long): Flow<Member?>

    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    suspend fun getMemberByIdDirect(id: Long): Member?

    @Query("SELECT * FROM members WHERE qrCode = :qrCode LIMIT 1")
    suspend fun getMemberByQrCode(qrCode: String): Member?

    @Query("SELECT * FROM members WHERE qrCode = :qrCode LIMIT 1")
    fun getMemberByQrCodeFlow(qrCode: String): Flow<Member?>

    @Query("SELECT COUNT(*) FROM members WHERE status = 'ACTIF'")
    fun countActiveMembersFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM members")
    suspend fun countMembers(): Int

    @Query("SELECT * FROM members ORDER BY id ASC")
    suspend fun getAllMembersDirect(): List<Member>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<Member>): List<Long>

    @Update
    suspend fun updateMember(member: Member)

    @Query("UPDATE members SET qrCode = :qrCode WHERE id = :memberId")
    suspend fun updateMemberQrCode(memberId: Long, qrCode: String)

    @Delete
    suspend fun deleteMember(member: Member)

    @Query("DELETE FROM members")
    suspend fun deleteAllMembers()
}
