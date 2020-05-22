package com.example.samplenotepad.views.search

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.samplenotepad.R
import com.example.samplenotepad.viewModels.SearchTopViewModel


class SearchTopFragment : Fragment() {

    private lateinit var topViewModel: SearchTopViewModel


    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.search_top_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        //ツールバーのタイトルをセット
        activity?.title = getString(R.string.category_list)
    }


    internal fun setValues(topVM: SearchTopViewModel) {
        topViewModel = topVM
    }

}
