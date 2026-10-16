package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.CoachUser
import com.example.data.model.JudoCategory
import com.example.data.model.PastTrainingSession
import com.example.data.model.ScheduledTrainingSession
import com.example.data.model.SessionStatus

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val groupCode: String,
    val coach1Name: String,
    val coach2Name: String,
    val description: String,
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): JudoCategory = JudoCategory(
        id = id,
        name = name,
        groupCode = groupCode,
        coach1Name = coach1Name,
        coach2Name = coach2Name,
        description = description,
        isActive = isActive
    )

    companion object {
        fun fromDomain(cat: JudoCategory, updatedAt: Long = System.currentTimeMillis()): CategoryEntity = CategoryEntity(
            id = cat.id,
            name = cat.name,
            groupCode = cat.groupCode,
            coach1Name = cat.coach1Name,
            coach2Name = cat.coach2Name,
            description = cat.description,
            isActive = cat.isActive,
            updatedAt = updatedAt
        )
    }
}

@Entity(tableName = "coaches")
data class CoachEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val grade: String,
    val phone: String,
    val assignedCategoriesJoined: String, // comma separated
    val isActive: Boolean = true,
    val sessionsCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): CoachUser = CoachUser(
        id = id,
        name = name,
        grade = grade,
        phone = phone,
        assignedCategories = if (assignedCategoriesJoined.isBlank()) emptyList() else assignedCategoriesJoined.split(";"),
        isActive = isActive,
        sessionsCount = sessionsCount
    )

    companion object {
        fun fromDomain(coach: CoachUser, updatedAt: Long = System.currentTimeMillis()): CoachEntity = CoachEntity(
            id = coach.id,
            name = coach.name,
            grade = coach.grade,
            phone = coach.phone,
            assignedCategoriesJoined = coach.assignedCategories.joinToString(";"),
            isActive = coach.isActive,
            sessionsCount = coach.sessionsCount,
            updatedAt = updatedAt
        )
    }
}

@Entity(tableName = "scheduled_sessions")
data class ScheduledSessionEntity(
    @PrimaryKey
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val groupCode: String,
    val dayOfWeekName: String,
    val dateString: String,
    val formattedDisplayDate: String,
    val startTime: String,
    val endTime: String,
    val coachIdsJoined: String, // ";" separated
    val coachNamesJoined: String, // ";" separated
    val isRecurringWeekly: Boolean,
    val recurrenceEndDate: String?,
    val seriesId: String?,
    val notes: String,
    val status: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): ScheduledTrainingSession = ScheduledTrainingSession(
        id = id,
        categoryId = categoryId,
        categoryName = categoryName,
        groupCode = groupCode,
        dayOfWeekName = dayOfWeekName,
        dateString = dateString,
        formattedDisplayDate = formattedDisplayDate,
        startTime = startTime,
        endTime = endTime,
        coachIds = if (coachIdsJoined.isBlank()) emptyList() else coachIdsJoined.split(";"),
        coachNames = if (coachNamesJoined.isBlank()) emptyList() else coachNamesJoined.split(";"),
        isRecurringWeekly = isRecurringWeekly,
        recurrenceEndDate = recurrenceEndDate,
        seriesId = seriesId,
        notes = notes,
        status = try { SessionStatus.valueOf(status) } catch (_: Exception) { SessionStatus.SCHEDULED }
    )

    companion object {
        fun fromDomain(session: ScheduledTrainingSession, updatedAt: Long = System.currentTimeMillis()): ScheduledSessionEntity = ScheduledSessionEntity(
            id = session.id,
            categoryId = session.categoryId,
            categoryName = session.categoryName,
            groupCode = session.groupCode,
            dayOfWeekName = session.dayOfWeekName,
            dateString = session.dateString,
            formattedDisplayDate = session.formattedDisplayDate,
            startTime = session.startTime,
            endTime = session.endTime,
            coachIdsJoined = session.coachIds.joinToString(";"),
            coachNamesJoined = session.coachNames.joinToString(";"),
            isRecurringWeekly = session.isRecurringWeekly,
            recurrenceEndDate = session.recurrenceEndDate,
            seriesId = session.seriesId,
            notes = session.notes,
            status = session.status.name,
            updatedAt = updatedAt
        )
    }
}

@Entity(tableName = "past_sessions")
data class PastSessionEntity(
    @PrimaryKey
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val groupCode: String,
    val dateString: String,
    val startTime: String,
    val endTime: String,
    val coach1Name: String,
    val coach1Present: Boolean,
    val coach2Name: String,
    val coach2Present: Boolean,
    val presentCount: Int,
    val absentCount: Int,
    val presentMembersJoined: String,
    val absentMembersJoined: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): PastTrainingSession = PastTrainingSession(
        id = id,
        categoryId = categoryId,
        categoryName = categoryName,
        groupCode = groupCode,
        dateString = dateString,
        startTime = startTime,
        endTime = endTime,
        coach1Name = coach1Name,
        coach1Present = coach1Present,
        coach2Name = coach2Name,
        coach2Present = coach2Present,
        presentCount = presentCount,
        absentCount = absentCount,
        presentMemberNames = if (presentMembersJoined.isBlank()) emptyList() else presentMembersJoined.split(";"),
        absentMemberNames = if (absentMembersJoined.isBlank()) emptyList() else absentMembersJoined.split(";")
    )

    companion object {
        fun fromDomain(session: PastTrainingSession, updatedAt: Long = System.currentTimeMillis()): PastSessionEntity = PastSessionEntity(
            id = session.id,
            categoryId = session.categoryId,
            categoryName = session.categoryName,
            groupCode = session.groupCode,
            dateString = session.dateString,
            startTime = session.startTime,
            endTime = session.endTime,
            coach1Name = session.coach1Name,
            coach1Present = session.coach1Present,
            coach2Name = session.coach2Name,
            coach2Present = session.coach2Present,
            presentCount = session.presentCount,
            absentCount = session.absentCount,
            presentMembersJoined = session.presentMemberNames.joinToString(";"),
            absentMembersJoined = session.absentMemberNames.joinToString(";"),
            updatedAt = updatedAt
        )
    }
}
