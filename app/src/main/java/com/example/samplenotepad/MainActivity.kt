package com.example.samplenotepad

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.Exception


class MainActivity : AppCompatActivity() {

//    private val mainViewModel: MemoMainViewModel =
//        ViewModelProvider.NewInstanceFactory().create(MemoMainViewModel::class.java)
//    private val optionViewModel: MemoOptionViewModel =
//        ViewModelProvider.NewInstanceFactory().create(MemoOptionViewModel::class.java)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(mainToolbar)

        memoPager.adapter = MemoPagerAdapter(this)

        TabLayoutMediator(memoTabLayout, memoPager) { tab, position ->
            when (position) {
                0 -> tab.text = "メモ本文"
                1 -> tab.text = "オプション設定"
            }
        }.attach()

        //viewPagerのバグでfragmentの表示時にViewのフォーカスが外れてしまうので、
        // 改めてフォーカスを取得してsoftwareKeyboardをrestartする
        memoPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> MemoMainViewModel.ForFirstFocusInMainFragment.setFocusAndSoftWareKeyboard()
                    else -> super.onPageSelected(position)
                }
            }
        })
    }

    override fun onBackPressed() {
        when (memoPager.currentItem) {
            0 -> super.onBackPressed()
            else -> memoPager.currentItem = memoPager.currentItem - 1
        }
    }


    private inner class MemoPagerAdapter(fragment: FragmentActivity) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> MemoMainFragment()
                1 -> MemoOptionFragment()
                else -> throw Exception("total number of fragments is different from getItemCount() ")
            }
        }
    }
}
