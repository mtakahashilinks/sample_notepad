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
import com.example.samplenotepad.entities.BySearchWord
import com.example.samplenotepad.usecases.searchTopListRecyclerView.SearchTopListAdapter
import com.example.samplenotepad.usecases.searchTopListRecyclerView.getItemTouchHelperCallback
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.moveToSearchResult
import kotlinx.android.synthetic.main.fragment_search_top.*
import kotlinx.coroutines.runBlocking


class SearchTopFragment : Fragment() {

    companion object {
        private var instance: SearchTopFragment? = null

        internal fun instanceToAddOnActivity(): SearchTopFragment {
            val mInstance = instance

            return when (mInstance != null && !mInstance.isAdded) {
                true -> mInstance
                false -> SearchTopFragment().apply { instance = this }
            }
        }

        internal fun clearSearchTopFragmentInstanceFlag() {
            instance = null
        }
    }


    private lateinit var searchViewModel: SearchViewModel
    private lateinit var listAdapter: SearchTopListAdapter


    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_search_top, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = runBlocking<Unit> {
        super.onViewCreated(view, savedInstanceState)
        Log.d("場所:SearchTopFragment", "onViewCreatedが呼ばれた fragment=${this@SearchTopFragment.id}")

        searchViewModel = MemoSearchActivity.searchViewModel

        listAdapter = SearchTopListAdapter(this@SearchTopFragment, searchViewModel)

        //SearchViewの設定
        memoSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = when (query == null) {
                true -> false
                false -> {
                    searchViewModel.loadAndSetDataSetForMemoListFindBySearchWord(query)
                    requireActivity().moveToSearchResult(BySearchWord)
                    true
                }
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        //RecyclerViewの設定
        searchTopRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@SearchTopFragment.context)
            adapter = listAdapter

            //スワイプでリストItemを削除する為の処理
            ItemTouchHelper(
                searchViewModel.getItemTouchHelperCallback(this@SearchTopFragment, listAdapter)
            ).attachToRecyclerView(this)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        Log.d("場所:SearchTopFragment", "onResumeが呼ばれた fragment=${this.id}")

        //アプリバーのタイトルをセット
        activity?.title = getString(R.string.appbar_title_search_top)

        //DataSetForCategoryListを更新してからlistAdapterも更新
        searchViewModel.loadAndSetDataSetForCategoryList()
        listAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("場所:SearchTopFragment", "onDestroyが呼ばれた fragment=${this.id}")

        clearSearchTopFragmentInstanceFlag()
    }
}
