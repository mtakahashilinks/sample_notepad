package com.example.samplenotepad.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


//各メモの情報
@Entity(tableName = "memoInfoTable")
data class MemoInfo(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "memoId")
    val rowid: Long,
    val createdDateTime: String,
    val title: String,
    val category: String,
    val contents: String, //MemoContentsをシリアライズしたもの
    val contentsText: String, //中身の検索用(MemoContentsの全てのTextを繋げてシリアライズしたもの)
    val reminderDateTime: String, //この値が空ならリマインダーが設定されていない
    val preAlarm: Int,
    val postAlarm: Int
) { companion object }
