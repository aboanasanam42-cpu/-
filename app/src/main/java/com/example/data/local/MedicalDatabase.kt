package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MedicalRecord::class,
        Patient::class,
        LabRecord::class,
        RadiologyRecord::class,
        PharmacyRecord::class,
        ExpenseRecord::class,
        ScheduledMessage::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MedicalDatabase : RoomDatabase() {

    abstract fun medicalDao(): MedicalDao

    companion object {
        @Volatile
        private var INSTANCE: MedicalDatabase? = null

        fun getDatabase(context: Context): MedicalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MedicalDatabase::class.java,
                    "medical_accounting_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
