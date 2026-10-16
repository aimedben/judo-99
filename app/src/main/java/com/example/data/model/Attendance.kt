package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance",
    foreignKeys = [
        ForeignKey(
            entity = Member::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["memberId"]),
        Index(value = ["dateString"]),
        Index(value = ["memberId", "dateString"], unique = false)
    ]
)
data class Attendance(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memberId: Long,
    val dateString: String, // e.g. "2026-10-06"
    val timeString: String, // e.g. "18:07"
    val timestamp: Long = System.currentTimeMillis(),
    val checkInMethod: String = "QR_SCAN", // "QR_SCAN", "MANUAL", "CARD_SIMULATOR"
    val deviceId: String = "DOJO_TERMINAL_1"
)

data class AttendanceWithMember(
    val attendanceId: Long,
    val memberId: Long,
    val firstName: String,
    val lastName: String,
    val groupCode: String,
    val beltName: String,
    val dateString: String,
    val timeString: String,
    val timestamp: Long,
    val qrCode: String,
    val phone: String,
    val photoUrl: String = ""
) {
    val fullName: String
        get() = "$firstName $lastName"

    val belt: Belt
        get() = Belt.fromString(beltName)

    val group: JudoGroup
        get() = JudoGroup.fromString(groupCode)
}
