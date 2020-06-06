package com.example.samplenotepad.views.search

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.samplenotepad.R
import com.example.samplenotepad.entities.DisplayExistMemo
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.usecases.initMemoContentsOperation
import kotlinx.android.synthetic.main.fragment_display_memo.*


class DisplayMemoFragment : Fragment() {

    private lateinit var searchViewModel:SearchViewModel


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_display_memo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initMemoContentsOperation(
            this, searchViewModel, displayMemoContentsContainerLayout, DisplayExistMemo
        )
    }


    internal fun setValues(searchVM: SearchViewModel) {
        searchViewModel = searchVM
    }


}
