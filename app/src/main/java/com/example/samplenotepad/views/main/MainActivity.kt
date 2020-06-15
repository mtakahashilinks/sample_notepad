package com.example.samplenotepad.views.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.samplenotepad.*
import com.example.samplenotepad.data.AppDatabase
import com.example.samplenotepad.data.deserializeMemoContents
import com.example.samplenotepad.data.loadMemoInfoFromDatabase
import com.example.samplenotepad.viewModels.MemoEditViewModel
import com.example.samplenotepad.viewModels.MemoOptionViewModel
import com.example.samplenotepad.views.MemoAlertDialog
import com.example.samplenotepad.views.search.MemoSearchActivity
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_memo_edit.*
import kotlin.Exception
import com.example.samplenotepad.usecases.*


const val MEMO_Id = "MEMO_ID"
const val MEMO_TEMPLATE_NAME_LIST_FILE = "memo_template_name_list"
const val MEMO_TEMPLATE_FILE = "memo_template_"


class MainActivity : AppCompatActivity() {
    //PagerにセットするAdapter
    private inner class MemoPagerAdapter(fragment: FragmentActivity) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> MemoEditFragment.getInstanceOrCreateNewOne()
                1 -> MemoOptionFragment()
                else -> throw Exception("total number of fragments is different from getItemCount() ")
            }
        }
    }


    private val editViewModel: MemoEditViewModel = MemoEditViewModel.getInstanceOrCreateNewOne()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(mainToolbar)

        val existMemoId = intent.getLongExtra(MEMO_Id, -1L)

        if (existMemoId != -1L) {
            val memoInfo = loadMemoInfoFromDatabase(this, existMemoId)
            val memoContents = memoInfo.contents.deserializeMemoContents()
            Log.d("場所:MainActivity#onCreate", "memoId=${memoInfo.rowid}")
            Log.d("場所:MainActivity#onCreate", "memoContents=${memoContents}")

            editViewModel.updateMemoInfo { memoInfo }
            editViewModel.updateMemoContents { memoContents }
        }

        //Pagerの設定
        memoPager.apply {
            adapter = MemoPagerAdapter(this@MainActivity)

            //MemoEditFragmentに遷移した時に改めてフォーカスを取得してsoftwareKeyboardをrestartする
            memoPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    when (position) {
                        0 -> {
                            val fragment = MemoEditFragment.getInstanceOrCreateNewOne()
                            val container = fragment.memoContentsContainerLayout

                            fragment.getFocusAndShowSoftwareKeyboard(container)
                        }
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

        viewModelStore.clear()
        AppDatabase.getDatabase(this).close()
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
                        viewModelStore.clear()
                        MemoOptionViewModel.resetOptionStatesForCreateNewMemo()
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
        finish()
    }

    private fun showAlertDialogToSearchTop() {
        MemoAlertDialog(
            R.string.dialog_close_edit_title,
            R.string.dialog_close_edit_message,
            R.string.dialog_close_edit_positive_button,
            R.string.dialog_close_edit_negative_button,
            { dialog, id ->
                saveMemo()
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
                saveMemo()
                finish()
                super.onBackPressed()
            },
            { dialog, id ->
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
                saveMemo()
                editViewModel.resetEditStatesForCreateNewMemo()
                viewModelStore.clear()
                MemoOptionViewModel.resetOptionStatesForCreateNewMemo()
            },
            { dialog, id ->
                editViewModel.resetEditStatesForCreateNewMemo()
                viewModelStore.clear()
                MemoOptionViewModel.resetOptionStatesForCreateNewMemo()
            }
        ).show(supportFragmentManager, "reboot_and_create_new_memo_dialog")
    }
}
