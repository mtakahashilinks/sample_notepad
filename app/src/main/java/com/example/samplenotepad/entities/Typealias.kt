package com.example.samplenotepad.entities

import android.widget.EditText
import arrow.core.ListK


typealias MemoRow = EditText
typealias MemoContents = ListK<MemoRowInfo>
typealias AdapterPosition = Int
