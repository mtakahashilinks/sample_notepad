package com.example.samplenotepad.data

import androidx.room.*
import com.example.samplenotepad.entities.DataSetForCategoryList
import com.example.samplenotepad.entities.DataSetForMemoList
import com.example.samplenotepad.entities.MemoInfo


@Dao
interface MemoInfoDao {
    @Insert(entity = MemoInfo::class)
    suspend fun insertMemoInfo(memoInfo: MemoInfo): Long

    @Update(entity = MemoInfo::class)
    suspend fun updateMemoInfo(memoInfo: MemoInfo)

    @Delete(entity = MemoInfo::class)
    suspend fun deleteMemoInfo(memoInfo: MemoInfo)

    @Query("""
        DELETE
        FROM memoInfoTable
        WHERE memoId == :id
    """)
    suspend fun deleteById(id: Long)

    @Query("""
        DELETE
        FROM memoInfoTable
        WHERE category == :category
    """)
    suspend fun deleteByCategory(category: String)

    @Query("""
        UPDATE memoInfoTable
        SET createdDateTime = :timeStamp, contents = :newContents, contentsText = :newContentsText
        WHERE memoId == :id
    """)
    suspend fun updateContents(id: Long, timeStamp: Long, newContents: String, newContentsText: String)

    @Query("""
        UPDATE memoInfoTable
        SET category = :newCategoryName
        WHERE category == :oldCategoryName
    """)
    suspend fun renameCategory(oldCategoryName: String, newCategoryName: String)


    @Query("""
        SELECT * 
        FROM memoInfoTable 
        WHERE memoId == :id
        """)
    suspend fun getMemoInfoById(id: Long): MemoInfo

    @Query("""
        SELECT category
        FROM MemoInfoTable
        GROUP BY category
        ORDER BY category ASC
        """)
    suspend fun getCategoryList(): List<String>

    @Query("""
        SELECT category, COUNT(*)
        FROM MemoInfoTable
        GROUP BY category
        ORDER BY category ASC
        """)
    suspend fun getDataSetForCategoryList(): List<DataSetForCategoryList>


    @Query("""
        SELECT memoId, createdDateTime, title, category, contentsText, reminderDate
        FROM MemoInfoTable
        WHERE category LIKE :category
        ORDER BY createdDateTime DESC
        """)
    suspend fun getDataSetForMemoList(category: String): List<DataSetForMemoList>

    @Query("""
        SELECT memoId, createdDateTime, title, category, contentsText, reminderDate 
        FROM memoInfoTable 
        WHERE title LIKE :word OR category LIKE :word OR contentsText LIKE :word
        ORDER BY createdDateTime DESC
        """)
    suspend fun searchMemoInfoForSearchTop(word: String): List<DataSetForMemoList>

    @Query("""
        SELECT memoId, createdDateTime, title, category, contentsText, reminderDate 
        FROM memoInfoTable 
        WHERE category LIKE :category AND (title LIKE :word OR contentsText LIKE :word)
        ORDER BY createdDateTime DESC
        """)
    suspend fun searchMemoInfoForSearchInACategory(category: String, word: String): List<DataSetForMemoList>


//      @Query("""
//          SELECT *
//          FROM MemoInfoTable
//          WHERE contentsText LIKE :word
//          ORDER BY createdDateTime ASC
//          """)
//      suspend fun searchMemoInfoByText(word: String): MemoInfo //wildcardはwordに"%ab_c%"のようなものを渡す
}
