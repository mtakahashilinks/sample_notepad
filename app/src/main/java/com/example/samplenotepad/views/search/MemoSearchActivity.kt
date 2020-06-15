package com.example.samplenotepad.views.search

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import com.example.samplenotepad.R
import com.example.samplenotepad.data.loadDataSetForCategoryListFromDatabase
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.main.MainActivity
import kotlinx.android.synthetic.main.memo_search_activity.*


class MemoSearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.memo_search_activity)
        setSupportActionBar(searchToolbar)

        val searchViewModel: SearchViewModel = SearchViewModel.getInstanceOrCreateNewOne()

        //ViewModelのcategoryDataSetListをセット
        searchViewModel.updateDataSetForCategoryList { loadDataSetForCategoryListFromDatabase(this) }


        supportFragmentManager.beginTransaction()
            .replace(R.id.container, SearchTopFragment())
            .commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModelStore.clear()
    }


    //オプションメニューを作成
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search_appbar_menu, menu)
        return true
    }

    //オプションメニューのItemタップ時の処理
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.createNewMemo -> {
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                startActivity(intent)
                finish()

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
    }
}
