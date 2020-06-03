package com.example.samplenotepad.views.search

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.samplenotepad.R
import com.example.samplenotepad.viewModels.SearchViewModel
import kotlinx.android.synthetic.main.fragment_search_each_memo.*
import com.example.samplenotepad.data.loadDataSetForEachMemoListFromDatabase
import com.example.samplenotepad.usecases.searchEachMemoRecyclerView.SearchEachMemoListAdapter
import com.example.samplenotepad.usecases.searchEachMemoRecyclerView.getCallbackForItemTouchHelper


class SearchEachMemoFragment : Fragment() {

    private lateinit var searchViewModel: SearchViewModel
    private lateinit var category: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search_each_memo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        categoryTextView.text = getString(R.string.search_each_memo_category_name_text, category)

        searchViewModel.updateDataSetForEachMemoList { loadDataSetForEachMemoListFromDatabase(this, category) }

        //RecyclerViewの設定
        searchEachMemoRecyclerView.apply {
            val searchEachMemoListAdapter = SearchEachMemoListAdapter(
                this@SearchEachMemoFragment,
                searchViewModel
            )

            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@SearchEachMemoFragment.context)
            adapter = searchEachMemoListAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

            //スワイプでリストItemを削除する為の処理
            ItemTouchHelper(getCallbackForItemTouchHelper(
                this@SearchEachMemoFragment, searchViewModel, searchEachMemoListAdapter, category
            )).attachToRecyclerView(this)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        //アプリバーのタイトルをセット
        activity?.title = getString(R.string.appbar_title_search_each_memo)
    }


    internal fun setValues(searchVM: SearchViewModel, mCategory: String) {
        searchViewModel = searchVM
        category = mCategory
    }

    internal fun moveToDisplayMemo() {
        requireActivity().supportFragmentManager.beginTransaction()
            .addToBackStack(null)
            .replace(R.id.container, DisplayMemoFragment().apply { setValues(searchViewModel) })
            .commit()
    }
}
