package com.example.samplenotepad.views.search

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import com.example.samplenotepad.R
import com.example.samplenotepad.data.AppDatabase
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.main.MainActivity
import kotlinx.android.synthetic.main.memo_search_activity.*


class MemoSearchActivity : AppCompatActivity() {

    companion object {
        lateinit var instanceOfActivity: Activity private set
        lateinit var searchViewModel: SearchViewModel private set
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.memo_search_activity)
        setSupportActionBar(searchToolbar)

        instanceOfActivity = this
        searchViewModel = ViewModelProvider(this).get(SearchViewModel::class.java)

        searchViewModel.initViewModelForSearchTop()

        supportFragmentManager.beginTransaction()
            .replace(R.id.searchContainer, SearchTopFragment.getInstanceOrCreateNew())
            .commit()
    }

    override fun onDestroy() {
        super.onDestroy()

        viewModelStore.clear()
        AppDatabase.apply {
            getDatabase(this@MemoSearchActivity).close()
            clearDBInstanceFlag()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
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
                viewModelStore.clear()
                finish()

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


}
