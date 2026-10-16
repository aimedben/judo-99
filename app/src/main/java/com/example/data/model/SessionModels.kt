package com.example.data.model

import java.util.UUID

enum class SessionStatus(val label: String) {
    SCHEDULED("Programmée"),
    IN_PROGRESS("En cours"),
    COMPLETED("Terminée"),
    CANCELLED("Annulée")
}

enum class RecurrenceScope {
    THIS_ONLY,
    ALL_SERIES
}

data class ScheduledTrainingSession(
    val id: String = UUID.randomUUID().toString(),
    val categoryId: String,
    val categoryName: String,
    val groupCode: String,
    val dayOfWeekName: String, // e.g. "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"
    val dateString: String,    // e.g. "2026-10-14"
    val formattedDisplayDate: String, // e.g. "Mercredi 14 Octobre 2026"
    val startTime: String,     // e.g. "17:30"
    val endTime: String,       // e.g. "19:00"
    val coachIds: List<String> = emptyList(),
    val coachNames: List<String> = emptyList(),
    val isRecurringWeekly: Boolean = false,
    val recurrenceEndDate: String? = null,
    val seriesId: String? = null,
    val notes: String = "",
    val status: SessionStatus = SessionStatus.SCHEDULED
) {
    val durationMinutes: Int
        get() {
            return try {
                val (sh, sm) = startTime.split(":").map { it.toInt() }
                val (eh, em) = endTime.split(":").map { it.toInt() }
                val startTotal = sh * 60 + sm
                val endTotal = eh * 60 + em
                (endTotal - startTotal).coerceAtLeast(0)
            } catch (_: Exception) {
                90
            }
        }
}

enum class ConflictType {
    INVALID_TIME,
    INVALID_DURATION,
    COACH_OVERLAP,
    CATEGORY_OVERLAP
}

data class SessionConflict(
    val type: ConflictType,
    val message: String,
    val conflictingSession: ScheduledTrainingSession? = null,
    val isBlocking: Boolean = false
)

enum class CategorySortOption(val label: String) {
    NAME("Par nom"),
    ATHLETES_COUNT("Effectif athlètes"),
    NEXT_SESSION("Prochaine séance")
}
