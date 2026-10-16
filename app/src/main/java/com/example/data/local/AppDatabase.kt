package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.sync.SyncAuditLogDao
import com.example.data.local.sync.SyncAuditLogEntity
import com.example.data.local.sync.SyncQueueDao
import com.example.data.local.sync.SyncQueueEntity
import com.example.data.model.Attendance
import com.example.data.model.Member

@Database(
    entities = [
        Member::class,
        Attendance::class,
        SyncQueueEntity::class,
        SyncAuditLogEntity::class,
        CategoryEntity::class,
        CoachEntity::class,
        ScheduledSessionEntity::class,
        PastSessionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun syncAuditLogDao(): SyncAuditLogDao
    abstract fun clubDao(): ClubDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_queue (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        queueId TEXT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId TEXT NOT NULL,
                        operation TEXT NOT NULL,
                        payloadJson TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        retryCount INTEGER NOT NULL,
                        lastError TEXT
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_status ON sync_queue (status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_entityType_entityId ON sync_queue (entityType, entityId)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_audit_log (
                        id TEXT PRIMARY KEY NOT NULL,
                        deviceId TEXT NOT NULL,
                        entityName TEXT NOT NULL,
                        entityId TEXT NOT NULL,
                        operationType TEXT NOT NULL,
                        resolvedBy TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        details TEXT NOT NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_audit_log_timestamp ON sync_audit_log (timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_audit_log_entityName_entityId ON sync_audit_log (entityName, entityId)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        groupCode TEXT NOT NULL,
                        coach1Name TEXT NOT NULL,
                        coach2Name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        isActive INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS coaches (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        grade TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        assignedCategoriesJoined TEXT NOT NULL,
                        isActive INTEGER NOT NULL,
                        sessionsCount INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS scheduled_sessions (
                        id TEXT PRIMARY KEY NOT NULL,
                        categoryId TEXT NOT NULL,
                        categoryName TEXT NOT NULL,
                        groupCode TEXT NOT NULL,
                        dayOfWeekName TEXT NOT NULL,
                        dateString TEXT NOT NULL,
                        formattedDisplayDate TEXT NOT NULL,
                        startTime TEXT NOT NULL,
                        endTime TEXT NOT NULL,
                        coachIdsJoined TEXT NOT NULL,
                        coachNamesJoined TEXT NOT NULL,
                        isRecurringWeekly INTEGER NOT NULL,
                        recurrenceEndDate TEXT,
                        seriesId TEXT,
                        notes TEXT NOT NULL,
                        status TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS past_sessions (
                        id TEXT PRIMARY KEY NOT NULL,
                        categoryId TEXT NOT NULL,
                        categoryName TEXT NOT NULL,
                        groupCode TEXT NOT NULL,
                        dateString TEXT NOT NULL,
                        startTime TEXT NOT NULL,
                        endTime TEXT NOT NULL,
                        coach1Name TEXT NOT NULL,
                        coach1Present INTEGER NOT NULL,
                        coach2Name TEXT NOT NULL,
                        coach2Present INTEGER NOT NULL,
                        presentCount INTEGER NOT NULL,
                        absentCount INTEGER NOT NULL,
                        presentMembersJoined TEXT NOT NULL,
                        absentMembersJoined TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "judo_seddouk.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
