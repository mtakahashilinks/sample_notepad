package com.example.samplenotepad.views.search

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.samplenotepad.R
import com.example.samplenotepad.viewModels.SearchViewModel


class DisplayMemoFragment : Fragment() {

    private lateinit var searchViewModel:SearchViewModel


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_display_memo, container, false)
    }


    internal fun setValues(searchVM: SearchViewModel) {
        searchViewModel = searchVM
    }


}
