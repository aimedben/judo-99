package com.example.data.model

data class JudoCategory(
    val id: String,
    val name: String,
    val groupCode: String,
    val coach1Name: String,
    val coach2Name: String,
    val description: String = "",
    val isActive: Boolean = true
)

enum class UserRole {
    COACH,
    ADMIN
}

data class ActiveTrainingSession(
    val categoryId: String,
    val categoryName: String,
    val startTime: String,
    val startDate: String,
    val coach1Name: String,
    val coach1Present: Boolean,
    val coach2Name: String,
    val coach2Present: Boolean,
    val isExternalScannerMode: Boolean = true
)

data class PastTrainingSession(
    val id: String = java.util.UUID.randomUUID().toString(),
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
    val presentMemberNames: List<String> = emptyList(),
    val absentMemberNames: List<String> = emptyList()
)

enum class CoachTab(val label: String) {
    SESSION("Séance"),
    ATHLETES("Athlètes"),
    HISTORY("Historique"),
    PROFILE("Profil")
}

sealed class CoachScanFeedback {
    data class Success(
        val member: Member,
        val isCotisationUpToDate: Boolean,
        val timeString: String
    ) : CoachScanFeedback()

    data class AlreadyChecked(
        val member: Member,
        val timeString: String
    ) : CoachScanFeedback()

    data class Invalid(
        val message: String
    ) : CoachScanFeedback()
}

