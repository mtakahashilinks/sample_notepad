package com.example.samplenotepad.data

import androidx.room.*
import com.example.samplenotepad.entities.DataSetForCategoryList
import com.example.samplenotepad.entities.DataSetForEachMemoList
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
    suspend fun getCategoriesAndSize(): List<DataSetForCategoryList>

    @Query("""
        DELETE
        FROM memoInfoTable
        WHERE category == :category
    """)
    suspend fun deleteByCategory(category: String)

    @Query("""
         SELECT memoId, createdDateTime, title, contentsText
         FROM MemoInfoTable
         WHERE category Like :category
         ORDER BY createdDateTime ASC
          """)
    suspend fun getDataSetInCategory(category: String): List<DataSetForEachMemoList>


//      @Query("""
//          SELECT *
//          FROM MemoInfoTable
//          WHERE contentsText LIKE :word
//          ORDER BY createdDateTime ASC
//          """)
//      suspend fun searchMemoInfoByText(word: String): MemoInfo //wildcardはwordに"%ab_c%"のようなものを渡す
}
