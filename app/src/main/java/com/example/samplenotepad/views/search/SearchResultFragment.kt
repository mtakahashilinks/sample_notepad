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
    private lateinit var listAdapter: SearchMemoListAdapter
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

        listAdapter =
            SearchMemoListAdapter(searchViewModel) { memoInfoId -> moveToMemoDisplay(memoInfoId) }

        //SearchViewの設定
        memoSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = when (query == null) {
                true -> false
                false -> {
                    searchViewModel.loadAndSetDataSetForMemoListFindBySearchWord(query)
                    listAdapter.searchAgainAndShowResult()
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
            layoutManager = LinearLayoutManager(this@SearchResultFragment.context)
            adapter = listAdapter
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )

            //スワイプでリストItemを削除する為の処理
            ItemTouchHelper(
                listAdapter.getCallbackForItemTouchHelper(
                    this@SearchResultFragment.requireActivity(),
                    searchViewModel
                )
            ).attachToRecyclerView(this)
        }

        setSearchWordText()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        //アプリバーのタイトルをセット
        activity?.title = getString(R.string.appbar_title_search_result)

        //DataSetForMemoListを更新してからlistAdapterも更新
        val dataSetForMemoList = searchViewModel.loadAndSetDataSetForMemoListFindBySearchWord(
            searchViewModel.getSearchWord()
        )
        listAdapter.notifyDataSetChanged()

        //検索ワードに合うものが無い場合に表示する
        when (dataSetForMemoList.isEmpty()) {
            true -> noMatchResultTextView.visibility = View.VISIBLE
            false -> noMatchResultTextView.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        clearSearchResultFragmentInstanceFlag()
    }


    private fun moveToMemoDisplay(memoInfoId: Long) {
        val displayActivity = requireActivity()
        val intent = Intent(displayActivity, MemoDisplayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(ConstValForMemo.MEMO_Id, memoInfoId)
        }

        startActivity(intent)
    }

    //新しいFragmentで検索結果を表示
    private fun SearchMemoListAdapter.searchAgainAndShowResult() {
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
