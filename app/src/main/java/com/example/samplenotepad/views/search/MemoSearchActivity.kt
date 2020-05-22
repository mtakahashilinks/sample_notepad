package com.example.samplenotepad.views.search

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.example.samplenotepad.R
import com.example.samplenotepad.viewModels.SearchTopViewModel
import kotlinx.android.synthetic.main.memo_search_activity.*


class MemoSearchActivity : AppCompatActivity() {

    private val topViewModel: SearchTopViewModel =
        ViewModelProvider.NewInstanceFactory().create(SearchTopViewModel::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.memo_search_activity)

        setSupportActionBar(searchToolbar)

        supportFragmentManager.beginTransaction()
            .replace(R.id.container, SearchTopFragment().apply { setValues(topViewModel) })
            .commitNow()
    }
}
