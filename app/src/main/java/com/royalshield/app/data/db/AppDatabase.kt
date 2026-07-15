package com.royalshield.app.data.db

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context



import kotlinx.coroutines.flow.Flow
import com.royalshield.app.features.smarthome.data.SmartDeviceDao
import com.royalshield.app.features.smarthome.data.SmartDeviceEntity

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automation_rules")
    fun getAllRules(): Flow<List<AutomationRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AutomationRule)

    @Update
    suspend fun updateRule(rule: AutomationRule)

    @Delete
    suspend fun deleteRule(rule: AutomationRule)
}

@Database(entities = [AutomationRule::class, SmartDeviceEntity::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun automationDao(): AutomationDao
    abstract fun smartDeviceDao(): SmartDeviceDao

    companion object {
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `smart_devices` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `isConnected` INTEGER NOT NULL,
                        `isOn` INTEGER NOT NULL,
                        `brightness` REAL NOT NULL,
                        `lightColorArgb` INTEGER NOT NULL,
                        `provider` TEXT NOT NULL,
                        `endpointLabel` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )""".trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "royal_shield_db"
                )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


