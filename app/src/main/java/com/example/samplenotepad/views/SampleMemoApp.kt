package com.example.samplenotepad.views

import android.app.Application
import android.util.Log


class SampleMemoApp : Application(){

    override fun onCreate() {
        super.onCreate()
        Log.d("場所:SampleMemoApp#onCreate", "AppのonCreateが呼ばれた")
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d("場所:SampleMemoApp#onTerminate", "AppのonTerminateが呼ばれた")
    }
}
