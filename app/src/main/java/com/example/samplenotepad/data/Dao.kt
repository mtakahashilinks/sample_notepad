package com.example.samplenotepad.data

import androidx.room.*
import com.example.samplenotepad.entities.DataSetForCategoryList
import com.example.samplenotepad.entities.DataSetForMemoList
import com.example.samplenotepad.entities.MemoInfo


@Dao
interface MemoInfoDao {
    @Insert(entity = MemoInfo::class)
    suspend fun insertMemoInfoDao(memoInfo: MemoInfo): Long

    @Update(entity = MemoInfo::class)
    suspend fun updateMemoInfoDao(memoInfo: MemoInfo)

    @Delete(entity = MemoInfo::class)
    suspend fun deleteMemoInfoDao(memoInfo: MemoInfo)

    @Query("""
        DELETE
        FROM memoInfoTable
        WHERE memoId == :id
    """)
    suspend fun deleteByIdDao(id: Long)

    @Query("""
        DELETE
        FROM memoInfoTable
        WHERE category == :category
    """)
    suspend fun deleteByCategoryDao(category: String)

    @Query("""
        UPDATE memoInfoTable
        SET category = :newCategoryName
        WHERE category == :oldCategoryName
    """)
    suspend fun updateCategoryDao(oldCategoryName: String, newCategoryName: String)

    @Query("""
        UPDATE memoInfoTable
        SET reminderDateTime = "", preAlarm = 0, postAlarm = 0
        WHERE memoId == :id
    """)
    suspend fun clearReminderValuesByIdDao(id: Long)

    @Query("""
        SELECT * 
        FROM memoInfoTable 
        WHERE memoId == :id
        """)
    suspend fun getMemoInfoByIdDao(id: Long): MemoInfo

    @Query("""
        SELECT category
        FROM MemoInfoTable
        GROUP BY category
        ORDER BY category ASC
        """)
    suspend fun getCategoryListDao(): List<String>

    @Query("""
        SELECT category, COUNT(*)
        FROM MemoInfoTable
        GROUP BY category
        ORDER BY category ASC
        """)
    suspend fun getDataSetForCategoryListDao(): List<DataSetForCategoryList>


    @Query("""
        SELECT memoId, createdDateTime, title, category, contentsText, reminderDateTime
        FROM MemoInfoTable
        WHERE category LIKE :category
        ORDER BY createdDateTime DESC
        """)
    suspend fun getDataSetForMemoListDao(category: String): List<DataSetForMemoList>

    @Query("""
        SELECT memoId, createdDateTime, title, category, contentsText, reminderDateTime 
        FROM memoInfoTable 
        WHERE title LIKE :word OR category LIKE :word OR contentsText LIKE :word
        ORDER BY createdDateTime DESC
        """)
    suspend fun searchMemoByASearchWordDao(word: String): List<DataSetForMemoList>

    @Query("""
        SELECT memoId, createdDateTime, title, category, contentsText, reminderDateTime 
        FROM memoInfoTable 
        WHERE category LIKE :category AND (title LIKE :word OR contentsText LIKE :word)
        ORDER BY createdDateTime DESC
        """)
    suspend fun searchMemoByASearchWordAndCategoryDao(
        category: String,
        word: String
    ): List<DataSetForMemoList>
}
