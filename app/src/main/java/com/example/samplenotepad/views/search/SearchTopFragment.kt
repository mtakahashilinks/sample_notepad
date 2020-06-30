package com.example.samplenotepad.views.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.samplenotepad.R
import com.example.samplenotepad.usecases.searchTopListRecyclerView.SearchTopListAdapter
import com.example.samplenotepad.usecases.searchTopListRecyclerView.getItemTouchHelperCallback
import com.example.samplenotepad.viewModels.SearchViewModel
import kotlinx.android.synthetic.main.search_top_fragment.*
import kotlinx.coroutines.runBlocking


class SearchTopFragment : Fragment() {

    companion object {
        private var instance: SearchTopFragment? = null

        internal fun getInstanceOrCreateNew(): SearchTopFragment =
            instance ?: SearchTopFragment().apply { if (instance == null) instance = this }

        internal fun clearSearchTopFragmentInstanceFlag() {
            instance = null
        }
    }


    private lateinit var searchViewModel: SearchViewModel

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.search_top_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = runBlocking<Unit> {
        super.onViewCreated(view, savedInstanceState)

        searchViewModel = MemoSearchActivity.searchViewModel

        //SearchViewの設定
        memoSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = when (query == null) {
                true -> false
                false -> {
                    searchViewModel.searchMemoInfoAndSetWordAndResultForSearchTop(query)
                    moveToSearchResult()
                    true
                }
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        //RecyclerViewの設定
        searchTopRecyclerView.apply {
            val mAdapter = SearchTopListAdapter(this@SearchTopFragment, searchViewModel)

            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@SearchTopFragment.context)
            adapter = mAdapter

            //スワイプでリストItemを削除する為の処理
            ItemTouchHelper(searchViewModel.getItemTouchHelperCallback(this@SearchTopFragment, mAdapter))
                .attachToRecyclerView(this)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        //アプリバーのタイトルをセット
        activity?.title = getString(R.string.appbar_title_search_top)
    }

    override fun onDestroy() {
        super.onDestroy()

        clearSearchTopFragmentInstanceFlag()
    }


    internal fun moveToSearchInACategory(selectedCategory: String) {
        searchViewModel.loadDataSetForMemoListAndSetPropertyInViewModel(selectedCategory)

        Log.d("場所:SearchTopFragment", "selectedCategoryをセット category=$selectedCategory viewModel=$searchViewModel")

        requireActivity().supportFragmentManager.beginTransaction()
            .addToBackStack(null)
            .replace(R.id.searchContainer, SearchInACategoryFragment.getInstanceOrCreateNew())
            .commit()
    }

    private fun moveToSearchResult() {
        requireActivity().supportFragmentManager.beginTransaction()
            .addToBackStack(null)
            .replace(R.id.searchContainer, SearchResultFragment.getInstanceOrCreateNew())
            .commit()
    }
}
