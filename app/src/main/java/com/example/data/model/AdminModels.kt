package com.example.data.model

import java.util.UUID

data class CoachUser(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val grade: String,
    val phone: String,
    val assignedCategories: List<String> = emptyList(),
    val isActive: Boolean = true,
    val sessionsCount: Int = 0
)

data class ClubSettings(
    val clubName: String = "Judo Club Seddouk",
    val dojoAddress: String = "Dojo Municipal de Seddouk, 06000 Seddouk",
    val phone: String = "07 82 48 71 20",
    val season: String = "2024 - 2025",
    val openingHours: String = "Samedi - Jeudi : 09h00 - 21h00",
    val federationAffiliation: String = "Fédération de Judo"
)

data class AdminProfile(
    val name: String = "Sensei Ahmed BOURENANE",
    val phone: String = "07 82 48 71 20",
    val email: String = "contact@judoclubseddouk.dz",
    val role: String = "Directeur Technique & Administrateur",
    val pinCode: String = "2026",
    val photoUri: String? = null
)

data class AdminNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val timeAgo: String,
    val type: NotificationType,
    val isRead: Boolean = false
)

enum class NotificationType {
    COTISATION,
    SESSION,
    MEDICAL,
    INFO
}

enum class AdminSubScreen {
    NONE,
    STATS_DETAIL,
    ATTENDANCE_OVERVIEW,
    EXPORTS,
    SETTINGS,
    CLOUD_SYNC
}
