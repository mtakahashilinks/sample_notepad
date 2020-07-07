package com.example.samplenotepad.data

import android.content.Context
import androidx.room.*
import com.example.samplenotepad.entities.MemoInfo


@Database(entities = [MemoInfo::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        @Volatile
        private var dbInstance: AppDatabase? = null

        internal fun getDatabase(context: Context): AppDatabase =
            dbInstance ?: Room.databaseBuilder(
                context.applicationContext, AppDatabase::class.java, "AppDatabase"
            ).build().apply { dbInstance = this }

        internal fun clearDBInstanceFlag() {
            dbInstance = null
        }
    }


    abstract fun memoInfoDao(): MemoInfoDao
}
