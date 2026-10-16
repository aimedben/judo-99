package com.example.ui.screens

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.JudoGold
import com.example.ui.theme.JudoRed
import com.example.ui.theme.JudoSuccess

enum class StatPeriod(
    val title: String,
    val shortLabel: String,
    val frenchLabel: String,
    val description: String
) {
    TODAY("Aujourd'hui", "Jour", "Aujourd'hui", "Séances du jour"),
    WEEK("Cette semaine", "Sem.", "Semaine", "Séances de la semaine"),
    MONTH("Ce mois", "Mois", "Ce mois", "Séances sur 30 jours"),
    TWO_MONTHS("2 mois", "2 mois", "2 mois", "Séances sur 60 jours"),
    SEASON("Saison", "Saison", "Saison", "Ensemble de la saison sportive")
}

enum class StatFilterMode(
    val title: String,
    val description: String,
    val accentColor: Color
) {
    ALL_MEMBERS("Tous les judokas", "Effectif global du club", JudoGold),
    PRESENTS("Présents", "Judokas ayant participé aux cours", JudoSuccess),
    ABSENTS("Absents", "Judokas sans pointage enregistré", JudoRed),
    ATTENDANCE_RATE("Taux d'assiduité", "Classement par assiduité", JudoGold)
}
