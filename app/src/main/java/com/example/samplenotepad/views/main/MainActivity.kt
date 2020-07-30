package com.example.samplenotepad.views.main

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
    private inner class MemoPagerAdapter(fragment: FragmentActivity) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> MemoEditFragment.getInstanceOrCreateNew()
                1 -> MemoOptionFragment.getInstanceOrCreateNew()
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


    override fun onDestroy() {
        super.onDestroy()
        Log.d("場所:MainActivity", "onDestroyが呼ばれた activity=$this")

        viewModelStore.clear()

        AppDatabase.apply {
            getDatabase(this@MainActivity).close()
            clearDBInstanceFlag()
        }
    }

    override fun onBackPressed() {
        if (memoPager.currentItem == 0) {
            when (editViewModel.isSavedMemoContents()) {
                true -> {
                    viewModelStore.clear()
                    finish()
                    super.onBackPressed()
                }
                false -> showAlertDialogToCloseMainActivity()
            }
        }
        else memoPager.currentItem = memoPager.currentItem - 1
    }

    //オプションメニューを作成
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.appbar_menu, menu)
        return true
    }

    //オプションメニューのItemタップ時の処理
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.createNewMemo -> createNewMemo()
            R.id.toSearchTop -> moveToSearchTop()
            R.id.toReminderList -> moveToReminderList()
            R.id.toSearchByCalendar -> moveToSearchByCalendar()
            R.id.finishApp -> showAlertDialogForFinishApp()
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun createNewMemo() =
        when (editViewModel.isSavedMemoContents()) {
            true -> {
                editViewModel.resetViewsAndStatesForCreateNewMemo()
                true
            }
            false -> {
                showAlertDialogToRebootAndCreateNewMemo()
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
        when (editViewModel.isSavedMemoContents()) {
            true -> {
                action()
                true
            }
            false -> {
                showAlertDialogForOptionMenu{ action() }
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

    private fun showAlertDialogForOptionMenu(function: () -> Unit) {
        MemoAlertDialog(
            R.string.dialog_close_edit_title,
            R.string.dialog_close_edit_message,
            R.string.dialog_close_edit_positive_button,
            R.string.dialog_close_edit_negative_button,
            { dialog, id ->
                saveMemo(CreateNewMemo)
                function()
            },
            { dialog, id -> function() }
        ).show(supportFragmentManager, "main_option_menu_dialog")
    }

    private fun showAlertDialogToCloseMainActivity() {
        MemoAlertDialog(
            R.string.dialog_close_edit_title,
            R.string.dialog_close_edit_message,
            R.string.dialog_close_edit_positive_button,
            R.string.dialog_close_edit_negative_button,
            { dialog, id ->
                saveMemo(CreateNewMemo)
                viewModelStore.clear()
                finish()
                super.onBackPressed()
            },
            { dialog, id ->
                viewModelStore.clear()
                finish()
                super.onBackPressed()
            }
        ).show(supportFragmentManager, "main_close_dialog")
    }

    private fun showAlertDialogToRebootAndCreateNewMemo() {
        MemoAlertDialog(
            R.string.dialog_reboot_and_create_new_memo_title,
            R.string.dialog_reboot_and_create_new_memo_message,
            R.string.dialog_reboot_and_create_new_memo_positive_button,
            R.string.dialog_reboot_and_create_new_memo_negative_button,
            { dialog, id ->
                saveMemo(CreateNewMemo)
                editViewModel.resetViewsAndStatesForCreateNewMemo()
            },
            { dialog, id ->
                editViewModel.resetViewsAndStatesForCreateNewMemo()
            }
        ).show(supportFragmentManager, "reboot_and_create_new_memo_dialog")
    }
}
