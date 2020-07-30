package com.example.samplenotepad.views.search

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import com.example.samplenotepad.R
import com.example.samplenotepad.data.AppDatabase
import com.example.samplenotepad.entities.ConstValForSearch
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.*
import com.example.samplenotepad.views.moveToMainActivity
import com.example.samplenotepad.views.moveToReminderList
import com.example.samplenotepad.views.moveToSearchTopAndCancelAllStacks
import kotlinx.android.synthetic.main.activity_memo_search.*


class MemoSearchActivity : AppCompatActivity() {

    companion object {
        lateinit var instanceOfActivity: Activity private set
        lateinit var searchViewModel: SearchViewModel private set
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memo_search)
        setSupportActionBar(searchToolbar)

        instanceOfActivity = this

        searchViewModel = ViewModelProvider(this).get(SearchViewModel::class.java).apply {
            createMemoContentsOperationActor()
        }


        //searchIdによって遷移先のFragmentを選択
        when (intent.getStringExtra(ConstValForSearch.SEARCH_ID)) {
            ConstValForSearch.SEARCH_TOP ->
                moveToSearchTopAndCancelAllStacks()
            ConstValForSearch.REMINDER_LIST -> {
                supportFragmentManager.apply {
                    moveToSearchTopAndCancelAllStacks()
                    moveToReminderList()
                }
            }
            ConstValForSearch.SEARCH_BY_CALENDAR -> {
                supportFragmentManager.apply {
                    moveToSearchTopAndCancelAllStacks()
                    moveToSearchByCalendar()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("場所:MemoSearchActivity", "onDestroyが呼ばれた activity=$this")

        viewModelStore.clear()

        AppDatabase.apply {
            getDatabase(this@MemoSearchActivity).close()
            clearDBInstanceFlag()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Log.d("場所:SearchActivity#onBackPressed", "tag=${supportFragmentManager.fragments.last().tag}")
    }


    //オプションメニューを作成
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.appbar_menu, menu)
        return true
    }

    //オプションメニューのItemタップ時の処理
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d("場所:SearchActivity#onOptionsItemSelected", "tagOfFragment=${supportFragmentManager.fragments.last().tag}")
        return when (item.itemId) {
            R.id.createNewMemo -> {
                moveToMainActivity()
                true
            }
            R.id.toSearchTop ->
                checkIfNeedAction(ConstValForSearch.SEARCH_TOP) {
                    moveToSearchTopAndCancelAllStacks()
                }
            R.id.toReminderList ->
                checkIfNeedAction(ConstValForSearch.REMINDER_LIST) {
                    moveToReminderList()
                }
            R.id.toSearchByCalendar ->
                checkIfNeedAction(ConstValForSearch.SEARCH_BY_CALENDAR) {
                    moveToSearchByCalendar()
                }
            R.id.finishApp -> {
                showAlertDialogForFinishApp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkIfNeedAction(tagOfFragment: String, action: () -> Unit) =
        when (supportFragmentManager.fragments.last().tag) {
            tagOfFragment -> false
            else -> {
                action()
                true
            }
        }

    private fun showAlertDialogForFinishApp() {
        MemoAlertDialog(
            R.string.dialog_finish_app_title,
            R.string.dialog_finish_app_message,
            R.string.dialog_finish_app_positive_button,
            R.string.dialog_finish_app_negative_button,
            { dialog, id ->
                finishAndRemoveTask()
            },
            { dialog, id -> dialog.dismiss() }
        ).show(supportFragmentManager, "finish_app_dialog")
    }
}
