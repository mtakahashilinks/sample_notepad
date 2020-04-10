package com.example.samplenotepad

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider


class MemoOptionFragment : Fragment() {

    private lateinit var optionViewModel: MemoOptionViewModel
//    private lateinit var mainViewModel: MemoMainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        optionViewModel = requireActivity().run {
            ViewModelProvider.NewInstanceFactory().create(MemoOptionViewModel::class.java)
        }
 //       mainViewModel = requireActivity().run {
 //           ViewModelProvider.NewInstanceFactory().create(MemoMainViewModel::class.java)
 //       }

    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_memo_option, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        //ツールバーのタイトルをセット
        activity?.title = getString(R.string.optional_setting)
    }
}
