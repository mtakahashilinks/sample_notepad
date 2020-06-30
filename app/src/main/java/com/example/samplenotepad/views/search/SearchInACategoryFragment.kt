package com.example.samplenotepad.views.search

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.samplenotepad.R
import kotlinx.android.synthetic.main.fragment_search_in_a_category.*
import com.example.samplenotepad.usecases.searchInACategoryRecyclerView.SearchInACategoryAdapter
import com.example.samplenotepad.usecases.searchInACategoryRecyclerView.getCallbackForItemTouchHelper
import com.example.samplenotepad.viewModels.SearchViewModel


class SearchInACategoryFragment : Fragment() {

    companion object {
        private var instance: SearchInACategoryFragment? = null

        internal fun getInstanceOrCreateNew(): SearchInACategoryFragment =
            instance ?: SearchInACategoryFragment().apply { if (instance == null) instance = this }

        internal fun clearSearchInACategoryFragmentInstanceFlag() {
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
        return inflater.inflate(R.layout.fragment_search_in_a_category, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchViewModel = MemoSearchActivity.searchViewModel

        Log.d("場所:SearchInACategoryFragment", "viewModel=$searchViewModel")
        category = searchViewModel.getDataSetForMemoList()[0].memoCategory
        categoryTextView.text = getString(R.string.search_each_memo_category_name_text, category)

        //SearchViewの設定
        memoSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = when (query == null) {
                true -> false
                false -> {
                    searchViewModel.searchMemoInfoAndSetWordAndResultForSearchInACategory(category, query)
                    moveToSearchResult()
                    true
                }
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        //RecyclerViewの設定
        searchInACategoryRecyclerView.apply {
            val searchMemoListAdapter =
                SearchInACategoryAdapter(searchViewModel) { moveToDisplayMemo() }

            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@SearchInACategoryFragment.context)
            adapter = searchMemoListAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

            //スワイプでリストItemを削除する為の処理
            ItemTouchHelper(getCallbackForItemTouchHelper(
                this@SearchInACategoryFragment.requireActivity(),
                searchViewModel,
                searchMemoListAdapter
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

        clearSearchInACategoryFragmentInstanceFlag()
    }


    private fun moveToDisplayMemo() {
        requireActivity().supportFragmentManager.beginTransaction()
            .addToBackStack(null)
            .replace(R.id.searchContainer, DisplayMemoFragment.getInstanceOrCreateNew())
            .commit()
    }

    private fun moveToSearchResult() {
        requireActivity().supportFragmentManager.beginTransaction()
            .addToBackStack(null)
            .replace(R.id.searchContainer, SearchResultFragment.getInstanceOrCreateNew())
            .commit()
    }
}
