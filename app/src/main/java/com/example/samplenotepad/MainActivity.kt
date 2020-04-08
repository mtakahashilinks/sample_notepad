package com.example.samplenotepad

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val viewModelMemo: MemoMainViewModel =
        ViewModelProvider.NewInstanceFactory().create(MemoMainViewModel::class.java)

    private val fragmentManager = supportFragmentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(mainToolbar)

        fragmentManager.beginTransaction().run {
            add(R.id.mainFrameLayout, MemoMainFragment())
            commit()
        }
    }
}
