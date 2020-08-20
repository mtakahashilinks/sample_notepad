package com.example.samplenotepad.views.display

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
import com.example.samplenotepad.entities.ConstValForMemo
import com.example.samplenotepad.entities.ConstValForSearch
import com.example.samplenotepad.viewModels.DisplayViewModel
import com.example.samplenotepad.views.*
import com.example.samplenotepad.views.moveToMainActivity
import com.example.samplenotepad.views.moveToSearchActivity
import com.example.samplenotepad.views.search.SearchActivity
import kotlinx.android.synthetic.main.activity_display.*

class MemoDisplayActivity : AppCompatActivity() {

    companion object {
        lateinit var instanceOfActivity: Activity private set
        lateinit var displayViewModel: DisplayViewModel private set
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display)
        setSupportActionBar(displayToolbar)

        instanceOfActivity = this

        displayViewModel = ViewModelProvider(this).get(DisplayViewModel::class.java)

        displayViewModel.createNewMemoContentsExecuteActor()

        if (intent != null) {
            displayViewModel.loadMemoInfoAndUpdate(
                intent.getLongExtra(ConstValForMemo.MEMO_Id, -1)
            )

            supportFragmentManager.beginTransaction()
                .replace(R.id.displayContainer, DisplayFragment.instanceToAddOnActivity())
                .commit()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("場所:DisplayActivity", "onNewIntentが呼ばれた")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.d("場所:DisplayActivity", "onRestoreInstanceStateが呼ばれた")
    }

    override fun onResume() {
        super.onResume()
        Log.d("場所:DisplayActivity", "onResumeが呼ばれた")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("場所:DisplayActivity", "onDestroyが呼ばれた activity=$this")

        AppDatabase.apply {
            getDatabase(this@MemoDisplayActivity).close()
            clearDBInstanceFlag()
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
    }


    //オプションメニューを作成
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.appbar_display_manu, menu)
        return true
    }

    //オプションメニューのItemタップ時の処理
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.createNewMemo -> {
                finishSearchActivityIfInstanced()
                moveToMainActivity()
                true
            }
            R.id.deleteMemo -> {
                showAlertDialogForDeleteMemo()
                true
            }
            R.id.toSearchTop -> {
                moveToSearchActivity(ConstValForSearch.SEARCH_TOP)
                true
            }
            R.id.toReminderList -> {
                moveToSearchActivity(ConstValForSearch.REMINDER_LIST)
                true
            }
            R.id.finishApp -> {
                showAlertDialogForFinishApp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun showAlertDialogForFinishApp() {
        MemoAlertDialog(
            R.string.dialog_finish_app_message,
            R.string.dialog_finish_app_positive_button,
            R.string.dialog_finish_app_negative_button,
            { dialog, id ->
                finishSearchActivityIfInstanced()
                finishAndRemoveTask()
            },
            { dialog, id -> dialog.dismiss() }
        ).show(supportFragmentManager, "finish_app_dialog")
    }

    private fun showAlertDialogForDeleteMemo() {
        MemoAlertDialog(
            R.string.dialog_delete_memo_message,
            R.string.dialog_delete_memo_positive_button,
            R.string.dialog_delete_memo_negative_button,
            { dialog, id ->
                displayViewModel.deleteMemoInfoById(displayViewModel.getMemoInfo().rowid)
                finishAndRemoveTask()
                moveToSearchActivity(ConstValForSearch.SEARCH_TOP)
            },
            { dialog, id -> dialog.dismiss() }
        ).show(supportFragmentManager, "finish_app_dialog")
    }
}


private fun finishSearchActivityIfInstanced() {
    if (SearchActivity.isInstance()) SearchActivity.instance.finish()
}
