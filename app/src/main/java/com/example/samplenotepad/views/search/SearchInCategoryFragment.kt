package com.example.samplenotepad.views.search

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
import com.example.samplenotepad.entities.BySearchWordAndCategory
import com.example.samplenotepad.usecases.searchMemoListRecyclerView.SearchMemoListAdapter
import com.example.samplenotepad.usecases.searchMemoListRecyclerView.getCallbackForItemTouchHelper
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.moveToDisplayActivity
import com.example.samplenotepad.views.moveToSearchResult
import kotlinx.android.synthetic.main.fragment_search_result.*


class SearchInCategoryFragment : Fragment() {

    companion object {
        private var instance: SearchInCategoryFragment? = null

        internal fun instanceToAddOnActivity(): SearchInCategoryFragment {
            val mInstance = instance

            return when (mInstance != null && !mInstance.isAdded) {
                true -> mInstance
                false -> SearchInCategoryFragment().apply { instance = this }
            }
        }

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

        searchViewModel = MemoSearchActivity.searchViewModel.apply {
            category = getSelectedCategory()
        }

        listAdapter = SearchMemoListAdapter(searchViewModel) { memoInfoId ->
            requireActivity().moveToDisplayActivity(memoInfoId)
        }

        //選択されたカテゴリーをViewのTextにセット
        searchSubjectTextView.text =
            getString(R.string.search_in_category_category_name_text, category)

        //SearchViewの設定
        memoSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = when (query == null) {
                true -> false
                false -> {
                    searchViewModel
                        .loadAndSetDataSetForMemoListFindBySearchWordAndCategory(query)
                    requireActivity().moveToSearchResult(BySearchWordAndCategory)
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
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )

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

        //DataSetForMemoListを更新してからlistAdapterも更新
        searchViewModel.loadAndSetDataSetForMemoListFindByCategory()
        listAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()

        clearSearchInACategoryFragmentInstanceFlag()
    }
}
