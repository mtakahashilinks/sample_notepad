package com.example.samplenotepad

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class MemoOptionViewModel : ViewModel() {

    val memoTitle = MutableLiveData<String>()
    val memoCategory = MutableLiveData<String>()
    val memoCategoriesList = MutableLiveData<MutableList<String>>()
    val memoDate = MutableLiveData<String>()
    val memoTime = MutableLiveData<String>()

    internal fun initMemoDate() {
        memoDate.value = getCurrentDay()
    }

    internal fun setMemoDate(date: String) {
        memoDate.value = date
    }


    override fun onCleared() {
        super.onCleared()
    }
}