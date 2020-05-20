package com.example.samplenotepad.entities

import androidx.room.Entity
import androidx.room.PrimaryKey


//各メモの情報
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
