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
    val createdDateTime: String, //"yyyy-MM-dd HH:mm"
    val title: String,
    val category: String,
    val contents: String, //MemoContentsをシリアライズしたもの
    val contentsForSearchByWord: String, //中身の検索用(MemoContentsの全てのTextを繋げてシリアライズしたもの)
    val baseDateTimeForAlarm: String, //"yyyy-MM-dd HH:mm" pre,postAlarmの基準日時。また、この値が空以外ならReminderはセットされている
    val reminderDateTime: String, //"yyyy-MM-dd HH:mm"
    val preAlarmPosition: Int, //スピナーのposition
    val postAlarmPosition: Int //スピナーのposition
) { companion object }
