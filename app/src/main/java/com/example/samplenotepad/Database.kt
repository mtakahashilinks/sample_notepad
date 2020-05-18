package com.example.samplenotepad

import android.content.Context
import androidx.room.*

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


//各メモの情報(データベースに保存する)
@Entity(tableName = "memoInfoTable")
data class MemoInfo(
    @PrimaryKey(autoGenerate = true) val rowid: Long,
    val createdDateTime: Long,
    val title: String,
    val category: String,
    val contents: String, //MemoContentsをシリアライズしたもの
    val contentsText: String, //中身の検索用(MemoContentsの全てのTextを繋げてシリアライズしたもの)
    val reminderDate: Int?,
    val reminderTime: Int?,
    val preAlarmTime: Int?,
    val postAlarmTime: Int?
) { companion object }


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
