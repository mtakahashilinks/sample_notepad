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
        SELECT * 
        FROM memoInfoTable 
        WHERE category == :category
        """)
    suspend fun getMemoInfoListByCategoryDao(category: String): List<MemoInfo>

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


    @Query(
        """
        SELECT *
        FROM MemoInfoTable
        WHERE category LIKE :category
        ORDER BY createdDateTime DESC
        """
    )
    suspend fun getDataSetForMemoListDao(category: String): List<MemoInfo>

    @Query(
        """
        SELECT * 
        FROM memoInfoTable 
        WHERE title LIKE :word OR category LIKE :word OR contentsForSearchByWord LIKE :word
        ORDER BY createdDateTime DESC
        """
    )
    suspend fun searchMemoByWordDao(word: String): List<MemoInfo>

    @Query(
        """
        SELECT * 
        FROM memoInfoTable 
        WHERE category LIKE :category AND (title LIKE :word OR contentsForSearchByWord LIKE :word)
        ORDER BY createdDateTime DESC
        """
    )
    suspend fun searchMemoByWordAndCategoryDao(
        category: String,
        word: String
    ): List<MemoInfo>

    @Query(
        """
        SELECT * 
        FROM memoInfoTable 
        WHERE reminderDateTime != "" AND (title LIKE :word OR category LIKE :word OR contentsForSearchByWord LIKE :word)
        ORDER BY createdDateTime DESC
        """
    )
    suspend fun searchMemoByWordWithReminderDao(word: String): List<MemoInfo>
}
