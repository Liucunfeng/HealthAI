package com.example.healthai.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [UserProfile::class, AnalysisRecord::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun analysisRecordDao(): AnalysisRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * v1 -> v2 迁移：
         * 1) 重建 user_profile 为多行表（id 自增 + name），并把旧单行迁移过来（name 置 ""）。
         * 2) analysis_records 新增 displayText 列（NOT NULL DEFAULT ''），旧记录保持空串。
         * 旧数据库升级后历史不丢，displayText 为空，由历史页回退 ResultFormatter 美化。
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE user_profile_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        heightCm REAL NOT NULL,
                        weightKg REAL NOT NULL,
                        age INTEGER NOT NULL,
                        gender TEXT NOT NULL,
                        goal TEXT NOT NULL,
                        activity TEXT NOT NULL
                    )"""
                )
                db.execSQL(
                    """INSERT INTO user_profile_new (id, name, heightCm, weightKg, age, gender, goal, activity)
                       SELECT id, '', heightCm, weightKg, age, gender, goal, activity FROM user_profile"""
                )
                db.execSQL("DROP TABLE user_profile")
                db.execSQL("ALTER TABLE user_profile_new RENAME TO user_profile")

                db.execSQL("ALTER TABLE analysis_records ADD COLUMN displayText TEXT NOT NULL DEFAULT ''")
            }
        }

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "health_ai.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
