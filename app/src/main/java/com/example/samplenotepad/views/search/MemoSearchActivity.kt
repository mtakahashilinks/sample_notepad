package com.example.samplenotepad.views.search

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.example.samplenotepad.R
import com.example.samplenotepad.data.AppDatabase
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.MemoAlertDialog
import com.example.samplenotepad.views.main.MainActivity
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

        supportFragmentManager.beginTransaction()
            .replace(R.id.searchContainer, SearchTopFragment.getInstanceOrCreateNew())
            .commit()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

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
    }


    //オプションメニューを作成
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search_and_display_appbar_menu, menu)
        return true
    }

    //オプションメニューのItemタップ時の処理
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.createNewMemo -> {
                moveToMainActivity()
                true
            }
            R.id.toSearchTop -> {
                moveToSearchTopAndCancelAllStacks()
                true
            }
            R.id.toReminderList -> {
                moveToReminderList()
                true
            }
            R.id.toSearchByCalendar -> {
                true
            }
            R.id.finishApp -> {
                showAlertDialogForFinishApp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun moveToMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        startActivity(intent)
        viewModelStore.clear()
        finish()
    }

    private fun moveToSearchTopAndCancelAllStacks() {
        supportFragmentManager.apply {
            popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

            beginTransaction()
                .replace(R.id.searchContainer, SearchTopFragment.getInstanceOrCreateNew())
                .commit()
        }
    }

    private fun moveToReminderList() {
        supportFragmentManager.beginTransaction()
            .addToBackStack(null)
            .replace(
                R.id.searchContainer, SearchWithReminderFragment.getInstanceOrCreateNew()
            )
            .commit()
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
