package com.example.samplenotepad.views.search

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.samplenotepad.R
import com.example.samplenotepad.entities.ConstValForMemo
import com.example.samplenotepad.usecases.searchMemoListRecyclerView.SearchMemoListAdapter
import com.example.samplenotepad.usecases.searchMemoListRecyclerView.getCallbackForItemTouchHelper
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.display.MemoDisplayActivity
import kotlinx.android.synthetic.main.fragment_search_result.*


class SearchInCategoryFragment : Fragment() {

    companion object {
        private var instance: SearchInCategoryFragment? = null

        internal fun getInstanceOrCreateNew(): SearchInCategoryFragment =
            instance ?: SearchInCategoryFragment().apply { if (instance == null) instance = this }

        internal fun clearSearchInACategoryFragmentInstanceFlag() {
            instance = null
        }
    }


    private lateinit var searchViewModel: SearchViewModel
    private lateinit var category: String
    private lateinit var listAdapter: SearchMemoListAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchViewModel = MemoSearchActivity.searchViewModel

        listAdapter =
            SearchMemoListAdapter(searchViewModel) { memoInfoId -> moveToMemoDisplay(memoInfoId) }


        searchViewModel.loadAndSetDataSetForMemoList().apply {
            //選択されたカテゴリーをViewのTextにセット
            when (this.isEmpty()) {
                true -> {
                    category = getString(R.string.memo_category_default_value)
                    searchWordTextView.text =
                        getString(R.string.search_in_category_category_name_text, category)
                }
                false -> {
                    category = searchViewModel.getSelectedCategory()
                    searchWordTextView.text =
                        getString(R.string.search_in_category_category_name_text, category)
                }
            }

        }

        //SearchViewの設定
        memoSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = when (query == null) {
                true -> false
                false -> {
                    searchViewModel.searchByWordAndCategoryThenUpdateDataSetForMemoList(category, query)
                    moveToSearchResult()
                    true
                }
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        //RecyclerViewの設定
        searchResultRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@SearchInCategoryFragment.context)
            adapter = listAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

            //スワイプでリストItemを削除する為の処理
            ItemTouchHelper(listAdapter.getCallbackForItemTouchHelper(
                this@SearchInCategoryFragment.requireActivity(),
                searchViewModel
            )).attachToRecyclerView(this)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        //アプリバーのタイトルをセット
        activity?.title = getString(R.string.appbar_title_search_in_category)

        searchViewModel.loadAndSetDataSetForMemoList()
        listAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()

        clearSearchInACategoryFragmentInstanceFlag()
    }


    private fun moveToMemoDisplay(memoInfoId: Long) {
        val displayActivity = requireActivity()
        val intent = Intent(displayActivity, MemoDisplayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(ConstValForMemo.MEMO_Id, memoInfoId)
        }

        startActivity(intent)
    }

    private fun moveToSearchResult() {
        requireActivity().supportFragmentManager.beginTransaction()
            .addToBackStack(null)
            .replace(R.id.searchContainer, SearchResultFragment.getInstanceOrCreateNew())
            .commit()
    }
}
