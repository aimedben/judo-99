package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class Belt(
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color? = null,
    val order: Int
) {
    BLANCHE("Blanche", Color(0xFFEEEEEE), null, 1),
    BLANCHE_JAUNE("Blanche-Jaune", Color(0xFFEEEEEE), Color(0xFFFFD54F), 2),
    JAUNE("Jaune", Color(0xFFFFD54F), null, 3),
    JAUNE_ORANGE("Jaune-Orange", Color(0xFFFFD54F), Color(0xFFFF9800), 4),
    ORANGE("Orange", Color(0xFFFF9800), null, 5),
    ORANGE_VERTE("Orange-Verte", Color(0xFFFF9800), Color(0xFF4CAF50), 6),
    VERTE("Verte", Color(0xFF2E7D32), null, 7),
    BLEUE("Bleue", Color(0xFF1976D2), null, 8),
    MARRON("Marron", Color(0xFF6D4C41), null, 9),
    NOIRE("Noire (1er Dan)", Color(0xFF212121), null, 10);

    companion object {
        fun fromString(value: String): Belt {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: BLANCHE
        }
    }
}

enum class JudoGroup(
    val code: String,
    val label: String,
    val ageRange: String
) {
    EVEIL("EVEIL", "Éveil Judo", "4-5 ans"),
    MINI_POUSSINS("U9", "Mini-Poussins (U9)", "6-7 ans"),
    POUSSINS("U11", "Poussins (U11)", "8-9 ans"),
    BENJAMINS("U13", "Benjamins (U13)", "10-11 ans"),
    MINIMES("U15", "Minimes (U15)", "12-13 ans"),
    CADETS("U18", "Cadets (U18)", "14-16 ans"),
    SENIORS("SENIOR", "Juniors / Séniors", "17+ ans");

    companion object {
        fun fromString(value: String): JudoGroup {
            val v = value.trim().lowercase()
            return when {
                v.contains("minime") || v.contains("u15") -> MINIMES
                v.contains("benjamin") || v.contains("u13") -> BENJAMINS
                v.contains("cadet") || v.contains("u18") -> CADETS
                v.contains("mini-poussin") || v.contains("u9") || v.contains("u8") -> MINI_POUSSINS
                v.contains("poussin") || v.contains("u11") || v.contains("u10") -> POUSSINS
                v.contains("eveil") || v.contains("éveil") || v.contains("baby") -> EVEIL
                v.contains("senior") || v.contains("sénior") || v.contains("junior") || v.contains("adulte") -> SENIORS
                else -> entries.find { it.code.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true) }
                    ?: MINIMES
            }
        }

        fun fromBirthDate(birthDate: String, referenceYear: Int = 2026): JudoGroup {
            val year = try {
                val clean = birthDate.trim()
                when {
                    clean.length >= 4 && clean.take(4).all { it.isDigit() } -> clean.take(4).toInt()
                    clean.length >= 10 && clean.takeLast(4).all { it.isDigit() } -> clean.takeLast(4).toInt()
                    else -> 2013
                }
            } catch (_: Exception) {
                2013
            }
            val age = (referenceYear - year).coerceIn(4, 80)
            return when {
                age in 4..5 -> EVEIL
                age in 6..7 -> MINI_POUSSINS
                age in 8..9 -> POUSSINS
                age in 10..11 -> BENJAMINS
                age in 12..13 -> MINIMES
                age in 14..16 -> CADETS
                else -> SENIORS
            }
        }
    }
}

val Belt.frenchName: String get() = displayName
val JudoGroup.frenchName: String get() = label
val JudoGroup.frenchLabel: String get() = label
