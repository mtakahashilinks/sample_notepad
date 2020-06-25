package com.example.samplenotepad.views.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.samplenotepad.*
import com.example.samplenotepad.data.AppDatabase
import com.example.samplenotepad.entities.CreateNewMemo
import com.example.samplenotepad.entities.MEMO_Id
import com.example.samplenotepad.viewModels.MemoEditViewModel
import com.example.samplenotepad.viewModels.MemoOptionViewModel
import com.example.samplenotepad.views.MemoAlertDialog
import com.example.samplenotepad.views.search.MemoSearchActivity
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.Exception
import com.example.samplenotepad.usecases.*


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

        val existMemoId = intent.getLongExtra(MEMO_Id, -1L)

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

        if (existMemoId != -1L)
            editViewModel.initViewModelForExistMemo(existMemoId)
    }


    override fun onDestroy() {
        super.onDestroy()

        viewModelStore.clear()
        AppDatabase.apply {
            getDatabase(this@MainActivity).close()
            clearDBInstanceFlag()
        }

        editViewModel.resetValueOfClearAllFocusInMemoContainerLiveData()
    }


    //オプションメニューを作成
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_appbar_menu, menu)
        return true
    }

    //オプションメニューのItemタップ時の処理
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.createNewMemo -> {
                when (editViewModel.compareMemoContentsWithSavePoint()) {
                    true -> {
                        editViewModel.resetEditStatesForCreateNewMemo()
                        resetOptionStatesForCreateNewMemo()
                        true
                    }
                    false -> {
                        showAlertDialogToRebootAndCreateNewMemo()
                        true
                    }
                }
            }
            R.id.toSearchTop -> {
                when (editViewModel.compareMemoContentsWithSavePoint()) {
                    true -> {
                        moveToMemoSearchActivity()
                        true
                    }
                    false -> {
                        showAlertDialogToSearchTop()
                        true
                    }
                }
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        when (memoPager.currentItem) {
            0 -> {
                when (editViewModel.compareMemoContentsWithSavePoint()) {
                    true -> {
                        viewModelStore.clear()
                        finish()
                        super.onBackPressed()
                    }
                    false -> showAlertDialogToCloseMainActivity()
                }
            }
            else -> memoPager.currentItem = memoPager.currentItem - 1
        }
    }


    private fun moveToMemoSearchActivity() {
        val intent = Intent(this, MemoSearchActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        startActivity(intent)
        viewModelStore.clear()
        finish()
    }

    private fun resetOptionStatesForCreateNewMemo() {
        if (MemoOptionFragment.isInstance())
            MemoOptionFragment.getInstanceOrCreateNew().resetValueOfAllView()
    }


    private fun showAlertDialogToSearchTop() {
        MemoAlertDialog(
            R.string.dialog_close_edit_title,
            R.string.dialog_close_edit_message,
            R.string.dialog_close_edit_positive_button,
            R.string.dialog_close_edit_negative_button,
            { dialog, id ->
                saveMemo(CreateNewMemo)
                moveToMemoSearchActivity()
            },
            { dialog, id -> moveToMemoSearchActivity() }
        ).show(supportFragmentManager, "main_to_search_dialog")
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
                editViewModel.resetEditStatesForCreateNewMemo()
                resetOptionStatesForCreateNewMemo()
            },
            { dialog, id ->
                editViewModel.resetEditStatesForCreateNewMemo()
                resetOptionStatesForCreateNewMemo()
            }
        ).show(supportFragmentManager, "reboot_and_create_new_memo_dialog")
    }
}
