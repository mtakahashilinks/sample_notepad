package com.example.samplenotepad.views

import android.app.Application
import android.util.Log


class SampleMemoApplication : Application(){

    companion object {
        lateinit var instance: Application private set
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("場所:SampleMemoApp#onCreate", "AppのonCreateが呼ばれた")

        instance = this
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d("場所:SampleMemoApp#onTerminate", "AppのonTerminateが呼ばれた")
    }
}
