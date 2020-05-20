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
            val instance =
                dbInstance

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


@Dao
interface MemoInfoDao {
    @Insert(entity = MemoInfo::class)
    suspend fun insertMemoInfo(memoInfo: MemoInfo): Long

    @Update(entity = MemoInfo::class)
    suspend fun updateMemoInfo(memoInfo: MemoInfo)

    @Delete(entity = MemoInfo::class)
    suspend fun deleteMemoInfo(memoInfo: MemoInfo)

    @Query("SELECT * FROM memoInfoTable WHERE rowid == :id")
    suspend fun getMemoInfo(id: Long): MemoInfo

 //   @Query("""
 //       SELECT * FROM MemoInfo
 //       WHERE title LIKE :title AND category LIKE :category AND contentsText LIKE :text
 //       ORDER BY createdDateTime ASC
 //       """)
 //   suspend fun searchMemoInfo(title: String, category: String, text: String): MemoContents
//
 //   @Query("""
 //       SELECT * FROM MemoInfo
 //       WHERE title LIKE :title OR contentsText LIKE :text
 //       ORDER BY createdDateTime ASC
 //       """)
 //   suspend fun searchMemoInfoByContents(title: String, text: String): MemoContents
//
 //   @Query("""
 //       SELECT * FROM MemoInfo
 //       WHERE category LIKE :category
 //       ORDER BY createdDateTime ASC
 //       """)
 //   suspend fun searchMemoInfoByCategory(category: String): MemoContents
}
