package com.example.samplenotepad.views.search

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.samplenotepad.R
import kotlinx.android.synthetic.main.fragment_search_each_memo.*
import com.example.samplenotepad.usecases.searchEachMemoRecyclerView.SearchEachMemoListAdapter
import com.example.samplenotepad.usecases.searchEachMemoRecyclerView.getCallbackForItemTouchHelper
import com.example.samplenotepad.viewModels.SearchViewModel


class SearchEachMemoFragment : Fragment() {

    companion object {
        private var instance: SearchEachMemoFragment? = null

        internal fun getInstanceOrCreateNew(): SearchEachMemoFragment =
            instance ?: SearchEachMemoFragment().apply { if (instance == null) instance = this }

        internal fun clearSearchEachMemoFragmentInstanceFlag() {
            instance = null
        }
    }


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

        searchViewModel = MemoSearchActivity.searchViewModel

        Log.d("場所:SearchEachMemoFragment", "viewModel=$searchViewModel")
        category = searchViewModel.getSelectedCategory()
        categoryTextView.text = getString(R.string.search_each_memo_category_name_text, category)

        searchViewModel.initViewModelForSearchEachMemo(category)

        //RecyclerViewの設定
        searchEachMemoRecyclerView.apply {
            val searchEachMemoListAdapter =
                SearchEachMemoListAdapter(this@SearchEachMemoFragment, searchViewModel)

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

    override fun onDestroy() {
        super.onDestroy()

        clearSearchEachMemoFragmentInstanceFlag()
    }


    internal fun moveToDisplayMemo() {
        requireActivity().supportFragmentManager.beginTransaction()
            .addToBackStack(null)
            .replace(R.id.searchContainer, DisplayMemoFragment.getInstanceOrCreateNew())
            .commit()
    }
}
