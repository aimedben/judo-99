package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.model.AttendanceWithMember
import com.example.data.model.Member

object CsvExporter {

    fun shareMembersReport(
        context: Context,
        members: List<Member>,
        filterLabel: String = "Tous",
        groupLabel: String = filterLabel
    ) {
        val label = if (groupLabel != "Tous") groupLabel else filterLabel
        val sb = StringBuilder()
        sb.append("ID,Nom,Prénom,DateNaissance,Groupe,Ceinture,Téléphone,Statut,NuméroCarte,DateInscription\n")
        for (m in members) {
            sb.append("\"${m.id}\",")
            sb.append("\"${m.lastName}\",")
            sb.append("\"${m.firstName}\",")
            sb.append("\"${m.birthDate}\",")
            sb.append("\"${m.groupCode}\",")
            sb.append("\"${m.beltName}\",")
            sb.append("\"${m.phone}\",")
            sb.append("\"${m.status}\",")
            sb.append("\"${m.qrCode}\",")
            sb.append("\"${m.registrationDate}\"\n")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Registre Judokas - $label")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        context.startActivity(Intent.createChooser(intent, "Exporter le registre des judokas"))
    }

    fun shareAttendanceReport(
        context: Context,
        attendances: List<AttendanceWithMember>,
        filterLabel: String = "Rapport",
        periodLabel: String = filterLabel
    ) {
        val label = if (filterLabel != "Rapport") filterLabel else periodLabel
        val sb = StringBuilder()
        sb.append("Date,Heure,Nom,Prénom,Catégorie,Ceinture,Téléphone\n")
        for (att in attendances) {
            sb.append("\"${att.dateString}\",")
            sb.append("\"${att.timeString}\",")
            sb.append("\"${att.lastName}\",")
            sb.append("\"${att.firstName}\",")
            sb.append("\"${att.groupCode}\",")
            sb.append("\"${att.beltName}\",")
            sb.append("\"${att.phone}\"\n")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Présences Judo - $label")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        context.startActivity(Intent.createChooser(intent, "Exporter le rapport des présences"))
    }
}
