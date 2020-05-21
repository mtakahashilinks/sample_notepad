package com.example.samplenotepad.data

import android.content.Context
import androidx.room.*
import com.example.samplenotepad.entities.MemoInfo


@Database(entities = [MemoInfo::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoInfoDao(): MemoInfoDao

    companion object {
        @Volatile
        private var dbInstance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            val instance = dbInstance

            return when (instance == null) {
                true -> {
                    synchronized(this) {
                        val tempInstance = Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "AppDatabase"
                        ).build()

                        dbInstance = tempInstance
                        tempInstance
                    }
                }
                false -> instance
            }
        }
    }
}
