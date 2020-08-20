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
import com.example.samplenotepad.entities.WithReminder
import com.example.samplenotepad.usecases.searchMemoListRecyclerView.SearchMemoListAdapter
import com.example.samplenotepad.usecases.searchMemoListRecyclerView.getCallbackForItemTouchHelper
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.moveToDisplayActivity
import com.example.samplenotepad.views.moveToSearchByCalendar
import com.example.samplenotepad.views.moveToSearchResult
import kotlinx.android.synthetic.main.fragment_search_result.*

class SearchWithReminderFragment : Fragment() {

    companion object {
        private var instance: SearchWithReminderFragment? = null

        internal fun instanceToAddOnActivity(): SearchWithReminderFragment {
            val mInstance = instance

            return when (mInstance != null && !mInstance.isAdded) {
                true -> mInstance
                false -> SearchWithReminderFragment().apply { instance = this }
            }
        }

        internal fun clearSearchWithReminderFragmentInstanceFlag() {
            instance = null
        }
    }


    private lateinit var searchViewModel: SearchViewModel
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

        searchViewModel = SearchActivity.searchViewModel

        listAdapter = SearchMemoListAdapter(searchViewModel) { memoInfoId ->
            requireActivity().moveToDisplayActivity(memoInfoId)
        }

        //SearchViewの設定
        memoSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = when (query == null) {
                true -> false
                false -> {
                    searchViewModel
                        .loadAndSetDataSetForMemoListFindBySearchWordWithReminder(query)
                    requireActivity().moveToSearchResult(WithReminder)
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
            layoutManager = LinearLayoutManager(this@SearchWithReminderFragment.context)
            adapter = listAdapter
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )

            //スワイプでリストItemを削除する為の処理
            ItemTouchHelper(
                listAdapter.getCallbackForItemTouchHelper(
                    this@SearchWithReminderFragment.requireActivity(),
                    searchViewModel
                )
            ).attachToRecyclerView(this)
        }

        //必要ないのでtextViewを非表示にする
        searchSubjectTextView.visibility = View.GONE
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        //アプリバーのタイトルをセット
        activity?.title = getString(R.string.appbar_title_search_with_reminder)

        //DataSetForMemoListを更新してからlistAdapterも更新
        val dataSetForMemoList = searchViewModel.loadAndSetDataSetForMemoListFindByWithReminder()
        listAdapter.notifyDataSetChanged()

        //リマインダー付きMemoの有無によるViewの表示・非表示の分岐
        when (dataSetForMemoList.isEmpty()) {
            true -> {
                listActionTextView.visibility = View.INVISIBLE
                moveToCalendarBtn.visibility = View.GONE
                noMatchResultTextView.visibility = View.VISIBLE
            }
            false -> {
                listActionTextView.visibility = View.VISIBLE
                noMatchResultTextView.visibility = View.GONE
                //カレンダー検索へのボタンの表示とクリックリスナー登録
                moveToCalendarBtn.apply {
                    visibility = View.VISIBLE
                    setOnClickListener {
                        this@SearchWithReminderFragment.requireActivity().moveToSearchByCalendar()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        clearSearchWithReminderFragmentInstanceFlag()
    }
}
