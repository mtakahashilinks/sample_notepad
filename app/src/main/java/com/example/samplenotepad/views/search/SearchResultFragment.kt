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
import com.example.samplenotepad.usecases.searchInACategoryRecyclerView.SearchInACategoryAdapter
import com.example.samplenotepad.usecases.searchInACategoryRecyclerView.getCallbackForItemTouchHelper
import com.example.samplenotepad.viewModels.SearchViewModel
import kotlinx.android.synthetic.main.fragment_search_result.*


class SearchResultFragment : Fragment() {

    companion object {
        private var instance: SearchResultFragment? = null

        internal fun getInstanceOrCreateNew(): SearchResultFragment =
            instance ?: SearchResultFragment().apply { if (instance == null) instance = this }

        internal fun clearSearchResultFragmentInstanceFlag() {
            instance = null
        }
    }


    private lateinit var searchViewModel: SearchViewModel
    private lateinit var searchWord: String


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

        setSearchWordText()

        //検索ワードに合うものが無い場合に表示する
        if (searchViewModel.getDataSetForMemoList().isEmpty())
            noMatchResultTextView.visibility = View.VISIBLE

        //RecyclerViewの設定
        val searchEachMemoListAdapter = SearchInACategoryAdapter(searchViewModel) { moveToDisplayMemo() }

        searchResultRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@SearchResultFragment.context)
            adapter = searchEachMemoListAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

            //スワイプでリストItemを削除する為の処理
            ItemTouchHelper(
                getCallbackForItemTouchHelper(
                    this@SearchResultFragment.requireActivity(),
                    searchViewModel,
                    searchEachMemoListAdapter
                )
            ).attachToRecyclerView(this)
        }


        //SearchViewの設定
        memoSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = when (query == null) {
                true -> false
                false -> {
                    searchViewModel.searchingMemoInfoAndSetValueInViewModel(query)
                    searchEachMemoListAdapter.searchAgainAndShowResult()
                    true
                }
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        //アプリバーのタイトルをセット
        activity?.title = getString(R.string.appbar_title_search_result)
    }

    override fun onDestroy() {
        super.onDestroy()

        clearSearchResultFragmentInstanceFlag()
    }


    private fun moveToDisplayMemo() {
        requireActivity().supportFragmentManager.beginTransaction()
            .addToBackStack(null)
            .replace(R.id.searchContainer, DisplayMemoFragment.getInstanceOrCreateNew())
            .commit()
    }

    //新しいFragmentで検索結果を表示
    private fun SearchInACategoryAdapter.searchAgainAndShowResult() {
        when (searchViewModel.getDataSetForMemoList().isEmpty()) {
            true -> noMatchResultTextView.visibility = View.VISIBLE
            false -> noMatchResultTextView.visibility = View.GONE
        }

        setSearchWordText()
        this.notifyDataSetChanged()
    }

    private fun setSearchWordText() {
        searchWord = searchViewModel.getSearchWord()
        memoSearchView.setQuery(searchWord, false)
        searchWordTextView.text = getString(R.string.search_word_text, searchWord)
    }
}
