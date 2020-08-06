package com.example.samplenotepad.views.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.samplenotepad.*
import com.example.samplenotepad.data.AppDatabase
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.viewModels.MemoEditViewModel
import com.example.samplenotepad.viewModels.MemoOptionViewModel
import com.example.samplenotepad.views.MemoAlertDialog
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.Exception
import com.example.samplenotepad.usecases.*
import com.example.samplenotepad.views.moveToSearchActivity


class MainActivity : AppCompatActivity() {

    companion object {
        lateinit var mainActivity: MainActivity private set
        lateinit var editViewModel: MemoEditViewModel private set
        lateinit var optionViewModel: MemoOptionViewModel private set
    }


    //PagerにセットするAdapter
    private inner class MemoPagerAdapter(
        fragment: FragmentActivity
    ) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> MemoEditFragment.instanceToAddOnActivity()
                1 -> MemoOptionFragment.instanceToAddOnActivity()
                else -> throw Exception("total number of fragments is different from getItemCount() ")
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(mainToolbar)

        mainActivity = this
        editViewModel = ViewModelProvider(this).get(MemoEditViewModel::class.java)
        optionViewModel = ViewModelProvider(this).get(MemoOptionViewModel::class.java)

        val existMemoId = intent.getLongExtra(ConstValForMemo.MEMO_Id, -1L)

        editViewModel.createNewMemoContentsExecuteActor()

        if (existMemoId != -1L)
            editViewModel.initViewModelForExistMemo(existMemoId)

        //Pagerの設定
        memoPager.apply {
            adapter = MemoPagerAdapter(this@MainActivity)

            memoPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    when (position) {
                        0 -> super.onPageSelected(position)
                        else -> super.onPageSelected(position)
                    }
                }
            })
        }

        //TabLayoutの設定
        TabLayoutMediator(memoTabLayout, memoPager) { tab, position ->
            when (position) {
                0 -> tab.text = getText(R.string.tab_title_for_edit_fragment)
                1 -> tab.text = getText(R.string.tab_title_for_option_fragment)
            }
        }.attach()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("場所:MainActivity", "onNewIntentが呼ばれた")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.d("場所:MainActivity", "onRestoreInstanceStateが呼ばれた")
    }

    override fun onResume() {
        super.onResume()
        Log.d("場所:MainActivity", "onResumeが呼ばれた")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("場所:MainActivity", "onDestroyが呼ばれた activity=$this")

        AppDatabase.apply {
            getDatabase(this@MainActivity).close()
            clearDBInstanceFlag()
        }
    }

    override fun onBackPressed() {
        when (memoPager.currentItem == 0) {
            true -> showAlertDialogForFinishApp()
            false -> memoPager.currentItem = memoPager.currentItem - 1
        }
    }

    //オプションメニューを作成
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.appbar_menu, menu)
        return true
    }

    //オプションメニューのItemタップ時の処理
    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.createNewMemo -> createNewMemo()
            R.id.toSearchTop -> moveToSearchTop()
            R.id.toReminderList -> moveToReminderList()
            R.id.toSearchByCalendar -> moveToSearchByCalendar()
            R.id.finishApp -> showAlertDialogForFinishApp()
            else -> super.onOptionsItemSelected(item)
        }



    private fun createNewMemo() =
        when (editViewModel.isSaved()) {
            true -> {
                editViewModel.resetViewsAndStatesForCreateNewMemo()
                true
            }
            false -> {
                showAlertDialogIfSaveMemo { editViewModel.resetViewsAndStatesForCreateNewMemo() }
                true
            }
        }

    private fun moveToSearchTop() =
        checkIfNeedToShowDialogForOptionMenu {
            moveToSearchActivity(ConstValForSearch.SEARCH_TOP)
        }

    private fun moveToReminderList() =
        checkIfNeedToShowDialogForOptionMenu {
            moveToSearchActivity(ConstValForSearch.REMINDER_LIST)
        }

    private fun moveToSearchByCalendar() =
        checkIfNeedToShowDialogForOptionMenu {
            moveToSearchActivity(ConstValForSearch.SEARCH_BY_CALENDAR)
        }


    private fun checkIfNeedToShowDialogForOptionMenu(action: () -> Unit) =
        when (editViewModel.isSaved()) {
            true -> {
                action()
                true
            }
            false -> {
                showAlertDialogIfSaveMemo { action() }
                true
            }
        }


    private fun showAlertDialogForFinishApp(): Boolean {
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

        return true
    }

    private fun showAlertDialogIfSaveMemo(function: () -> Unit) {
        MemoAlertDialog(
            R.string.dialog_confirm_save_memo_title,
            R.string.dialog_confirm_save_memo_message,
            R.string.dialog_confirm_save_memo_positive_button,
            R.string.dialog_confirm_save_memo_negative_button,
            { dialog, id ->
                saveMemo(CreateNewMemo)
                function()
            },
            { dialog, id -> function() }
        ).show(supportFragmentManager, "confirm_save_memo_dialog")
    }
}
