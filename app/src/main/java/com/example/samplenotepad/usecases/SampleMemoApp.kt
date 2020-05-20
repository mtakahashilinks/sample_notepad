package com.example.samplenotepad.usecases

import android.app.Application
import android.util.Log
import com.example.samplenotepad.data.AppDatabase
import com.example.samplenotepad.data.setIsAppExistForIO


class SampleMemoApp : Application(){

    override fun onCreate() {
        super.onCreate()
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d("場所:SampleMemoApp#onTerminate", "AppのonTerminateが呼ばれた")

        setIsAppExistForIO(false)

        AppDatabase.getDatabase(this).close()
    }
}
