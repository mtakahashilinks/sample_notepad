package com.example.samplenotepad.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


class MemoInputViewModelFactory() : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MemoInputViewModel() as T
    }
}

class MemoOptionViewModelFactory() : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MemoOptionViewModel() as T
    }
}
