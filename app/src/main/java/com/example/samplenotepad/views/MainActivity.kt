package com.example.samplenotepad.views

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.samplenotepad.*
import com.example.samplenotepad.viewModels.MemoInputViewModel
import com.example.samplenotepad.viewModels.MemoOptionViewModel
import com.example.samplenotepad.views.FragmentFactories.getInputFragment
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_memo_main.*
import kotlin.Exception


class MainActivity : AppCompatActivity() {

    private inner class MemoPagerAdapter(fragment: FragmentActivity) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> getInputFragment().apply { setValues(inputViewModel, optionViewModel) }
                1 -> MemoOptionFragment().apply { setValues(inputViewModel, optionViewModel) }
                else -> throw Exception("total number of fragments is different from getItemCount() ")
            }
        }
    }


    private val inputViewModel: MemoInputViewModel =
        ViewModelProvider.NewInstanceFactory().create(MemoInputViewModel::class.java)
    private val optionViewModel: MemoOptionViewModel =
        ViewModelProvider.NewInstanceFactory().create(MemoOptionViewModel::class.java)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(mainToolbar)

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

        TabLayoutMediator(memoTabLayout, memoPager) { tab, position ->
            when (position) {
                0 -> tab.text = "メモ本文"
                1 -> tab.text = "オプション設定"
            }
        }.attach()
    }


    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBackPressed() {
        when (memoPager.currentItem) {
            0 -> super.onBackPressed()
            else -> memoPager.currentItem = memoPager.currentItem - 1
        }
    }
}
