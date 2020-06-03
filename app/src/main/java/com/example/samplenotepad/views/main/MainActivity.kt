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
import com.example.samplenotepad.entities.MemoContents
import com.example.samplenotepad.entities.MemoInfo
import com.example.samplenotepad.entities.SaveMemoInfo
import com.example.samplenotepad.usecases.saveOperation
import com.example.samplenotepad.viewModels.MemoEditViewModel
import com.example.samplenotepad.viewModels.MemoOptionViewModel
import com.example.samplenotepad.views.FragmentFactories.getInputFragment
import com.example.samplenotepad.views.MemoAlertDialog
import com.example.samplenotepad.views.search.MemoSearchActivity
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_memo_edit.*
import kotlin.Exception


const val categoryArray = "CATEGORY_ARRAY"

class MainActivity : AppCompatActivity() {

    //PagerにセットするAdapter
    private inner class MemoPagerAdapter(fragment: FragmentActivity) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> getInputFragment().apply { setValues(editViewModel, optionViewModel) }
                1 -> MemoOptionFragment().apply { setValues(editViewModel, optionViewModel) }
                else -> throw Exception("total number of fragments is different from getItemCount() ")
            }
        }
    }


    private lateinit var memoInfo: MemoInfo
    private lateinit var memoContents: MemoContents

    private val editViewModel: MemoEditViewModel =
        ViewModelProvider.NewInstanceFactory().create(MemoEditViewModel::class.java)
    private val optionViewModel: MemoOptionViewModel =
        ViewModelProvider.NewInstanceFactory().create(MemoOptionViewModel::class.java)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(mainToolbar)

        //新規ではなく検索したメモの編集の場合
        if (::memoInfo.isInitialized) {
            editViewModel.apply {
                updateMemoInfo { memoInfo }
                updateMemoContents { memoContents }
                updateMemoContentsAtSavePoint()
            }
        }

        //Pagerの設定
        memoPager.apply {
            adapter = MemoPagerAdapter(this@MainActivity)

            //MemoInputFragmentに遷移した時に改めてフォーカスを取得してsoftwareKeyboardをrestartする
            memoPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    when (position) {
                        0 -> {
                            val fragment = getInputFragment()
                            val container = fragment.memoContentsContainerLayout
                            val childCount = container.childCount

                            if (childCount != 0) {
                                Log.d("場所:onPageSelected", "入った")
                              //  val inputManager = fragment.context?.getSystemService(
                              //      Context.INPUT_METHOD_SERVICE) as InputMethodManager

                                container.getChildAt(childCount - 1).apply {
                                    requestFocus()
                                    //動作しないinputManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                                }
                            }
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
    }


    //新規ではなく検索したメモの編集の場合に呼ばれる
    internal fun setValue(mMemoInfo: MemoInfo, mMemoContents: MemoContents) {
        memoInfo = mMemoInfo
        memoContents = mMemoContents
    }


    //オプションメニューを作成
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_appbar_menu, menu)
        return true
    }

    //オプションメニューのItemタップ時の処理
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.toSearchTop -> {
                when (editViewModel.compareMemoContentsWithSavePoint()) {
                    true -> {
                        moveToMemoSearchActivity()
                        true
                    }
                    false -> {
                        showToSearchTopAlertDialog()
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
                    true -> super.onBackPressed()
                    false -> showMainCloseAlertDialog()
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

    private fun showToSearchTopAlertDialog() {
        MemoAlertDialog(
            R.string.dialog_close_edit_title,
            R.string.dialog_close_edit_message,
            R.string.dialog_close_edit_positive_button,
            R.string.dialog_close_edit_negative_button,
            { dialog, id ->
                val memoInfo = editViewModel.getMemoInfo()
                val memoContents = editViewModel.getMemoContents()

                saveOperation(SaveMemoInfo(memoInfo, memoContents))

                moveToMemoSearchActivity()
            },
            { dialog, id -> moveToMemoSearchActivity() }
        ).show(supportFragmentManager, "main_to_Search_dialog")
    }

    private fun showMainCloseAlertDialog() {
        MemoAlertDialog(
            R.string.dialog_close_edit_title,
            R.string.dialog_close_edit_message,
            R.string.dialog_close_edit_positive_button,
            R.string.dialog_close_edit_negative_button,
            { dialog, id ->
                val memoInfo = editViewModel.getMemoInfo()
                val memoContents = editViewModel.getMemoContents()

                saveOperation(SaveMemoInfo(memoInfo, memoContents))

                super.onBackPressed()
            },
            { dialog, id -> super.onBackPressed() }
        ).show(supportFragmentManager, "main_close_dialog")
    }
}
