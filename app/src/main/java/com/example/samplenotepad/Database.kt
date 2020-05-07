package com.example.samplenotepad

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey


//各メモの情報(データベースに保存する)
@Fts4
@Entity
data class MemoInfo(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val createdDateTime: Long,
    val title: String,
    val category: String,
    @Embedded val contents: MemoContents,
    @Embedded val reminderInfo: ReminderInfo?
) { companion object }
