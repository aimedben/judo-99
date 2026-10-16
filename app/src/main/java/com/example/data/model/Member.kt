package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "members",
    indices = [Index(value = ["qrCode"], unique = true)]
)
data class Member(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val qrCode: String,
    val firstName: String,
    val lastName: String,
    val birthDate: String, // e.g. "2012-05-14"
    val phone: String,
    val groupCode: String, // e.g. "U15"
    val beltName: String, // e.g. "VERTE"
    val status: String = "ACTIF", // "ACTIF" or "SUSPENDU"
    val registrationDate: String = "2026-09-01",
    val photoUrl: String = "",
    val notes: String = ""
) {
    val fullName: String
        get() = "$firstName $lastName"

    val belt: Belt
        get() = Belt.fromString(beltName)

    val group: JudoGroup
        get() = JudoGroup.fromString(groupCode)

    val isCotisationUpToDate: Boolean
        get() = status != "SUSPENDU" && !notes.contains("retard", ignoreCase = true)
}
