package com.example.samplenotepad.views.search

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.example.samplenotepad.R
import com.example.samplenotepad.data.loadDataSetForCategoryListFromDatabase
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.main.MEMO_Id
import com.example.samplenotepad.views.main.MainActivity
import kotlinx.android.synthetic.main.memo_search_activity.*


class MemoSearchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.memo_search_activity)
        setSupportActionBar(searchToolbar)

        val searchViewModel: SearchViewModel =
            ViewModelProvider.NewInstanceFactory().create(SearchViewModel::class.java)

        //ViewModelのcategoryDataSetListをセット
        searchViewModel.updateDataSetForCategoryList { loadDataSetForCategoryListFromDatabase(this) }


        supportFragmentManager.beginTransaction()
            .replace(R.id.container, SearchTopFragment().apply { setValues(searchViewModel) })
            .commit()
    }


    override fun onBackPressed() {
        super.onBackPressed()
    }
}
