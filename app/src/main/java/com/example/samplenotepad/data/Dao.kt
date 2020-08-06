package com.example.samplenotepad.data

import androidx.room.*
import com.example.samplenotepad.entities.DataSetForCategoryList
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
    suspend fun deleteMemoInfoByIdDao(id: Long)

    @Query("""
        DELETE
        FROM memoInfoTable
        WHERE category == :category
    """)
    suspend fun deleteMemoInfoByCategoryDao(category: String)

    @Query("""
        UPDATE memoInfoTable
        SET category = :newCategoryName
        WHERE category == :oldCategoryName
    """)
    suspend fun updateCategoryDao(oldCategoryName: String, newCategoryName: String)

    @Query(
        """
        UPDATE memoInfoTable
        SET baseDateTimeForAlarm = "", reminderDateTime = "", preAlarmPosition = 0, postAlarmPosition = 0
        WHERE memoId == :id
    """
    )
    suspend fun clearAllReminderValueByIdDao(id: Long)

    @Query("""
        SELECT * 
        FROM memoInfoTable 
        WHERE memoId == :id
        """)
    suspend fun getMemoInfoByIdDao(id: Long): MemoInfo

    @Query("""
        SELECT * 
        FROM memoInfoTable 
        WHERE category == :category 
        ORDER BY createdDateTime DESC
        """)
    suspend fun getMemoInfoListByCategoryDao(category: String): List<MemoInfo>


    @Query(
        """
        SELECT * 
        FROM memoInfoTable 
        WHERE title LIKE :searchWord OR category LIKE :searchWord OR contentsForSearchByWord LIKE :searchWord
        ORDER BY createdDateTime DESC
        """
    )
    suspend fun getMemoInfoListBySearchWordDao(searchWord: String): List<MemoInfo>

    @Query(
        """
        SELECT * 
        FROM memoInfoTable 
        WHERE category LIKE :category AND (title LIKE :searchWord OR contentsForSearchByWord LIKE :searchWord)
        ORDER BY createdDateTime DESC
        """
    )
    suspend fun getMemoInfoListBySearchWordAndCategoryDao(
        category: String,
        searchWord: String
    ): List<MemoInfo>

    @Query(
        """
        SELECT * 
        FROM memoInfoTable 
        WHERE baseDateTimeForAlarm != ""
        ORDER BY createdDateTime DESC
        """
    )
    suspend fun getMemoInfoListWithReminderDao(): List<MemoInfo>

    @Query(
        """
        SELECT * 
        FROM memoInfoTable 
        WHERE baseDateTimeForAlarm != "" AND (title LIKE :searchWord OR category LIKE :searchWord OR contentsForSearchByWord LIKE :searchWord)
        ORDER BY createdDateTime DESC
        """
    )
    suspend fun getMemoInfoListBySearchWordWithReminderDao(searchWord: String): List<MemoInfo>

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
}
