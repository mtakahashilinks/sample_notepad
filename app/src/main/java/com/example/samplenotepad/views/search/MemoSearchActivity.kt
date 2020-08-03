package com.example.samplenotepad.views.search

import android.app.Activity
import android.content.Intent
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
        private var isInstance = false
        lateinit var instance: Activity private set
        lateinit var searchViewModel: SearchViewModel private set

        internal fun isInstance() = isInstance
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memo_search)
        setSupportActionBar(searchToolbar)

        instance = this.apply { isInstance = true }

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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("場所:SearchActivity", "onNewIntentが呼ばれた activity=$this")
        Log.d("場所:SearchActivity#onNewIntent", "intent=${intent?.getStringExtra(ConstValForSearch.SEARCH_ID)}")
        if (intent != null) {
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
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.d("場所:SearchActivity", "onRestoreInstanceStateが呼ばれた")
    }

    override fun onResume() {
        super.onResume()
        Log.d("場所:SearchActivity", "onResumeが呼ばれた activity=$this")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("場所:SearchActivity", "onDestroyが呼ばれた activity=$this")

        AppDatabase.apply {
            getDatabase(this@MemoSearchActivity).close()
            clearDBInstanceFlag()
        }

        isInstance = false
    }

    override fun onBackPressed() {
        when (supportFragmentManager.fragments.last().tag == ConstValForSearch.SEARCH_TOP) {
            true -> showAlertDialogForFinishApp()
            false -> super.onBackPressed()
        }
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
            { dialog, id -> finishAndRemoveTask() },
            { dialog, id -> dialog.dismiss() }
        ).show(supportFragmentManager, "finish_app_dialog")
    }
}
