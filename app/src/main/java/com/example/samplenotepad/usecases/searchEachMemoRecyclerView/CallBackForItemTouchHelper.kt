package com.example.samplenotepad.usecases.searchEachMemoRecyclerView

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.samplenotepad.R
import com.example.samplenotepad.data.deleteMemoByIdFromDatabase
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.MemoAlertDialog
import com.example.samplenotepad.views.search.SearchEachMemoFragment


//SwipeでリストのItemを削除するためのCallback
internal fun RecyclerView.getCallbackForItemTouchHelper(
    fragment: SearchEachMemoFragment,
    searchViewModel: SearchViewModel,
    adapter: SearchEachMemoListAdapter,
    category: String
) =
    object : ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            return makeMovementFlags(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                ItemTouchHelper.END
            )
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            MemoAlertDialog(
                R.string.dialog_search_each_memo_swipe_delete_title,
                R.string.dialog_search_each_memo_swipe_delete_message,
                R.string.dialog_search_each_memo_swipe_delete_positive_button,
                R.string.dialog_search_each_memo_swipe_delete_negative_button,
                { dialog, id ->
                    adapter.apply {
                        deleteItemFromEachMemoDataSet(viewHolder.adapterPosition)
                        notifyDataSetChanged()
                    }
                },
                { dialog, id -> adapter.notifyDataSetChanged() }
            ).show(
                fragment.requireActivity().supportFragmentManager,
                "search_each_memo_swipe_delete_dialog"
            )

            adapter.notifyDataSetChanged()
        }


        fun deleteItemFromEachMemoDataSet(adapterPosition: Int) {
            val targetMemoId = searchViewModel.getDataSetForEachMemoList()[adapterPosition].memoInfoId

            deleteMemoByIdFromDatabase(fragment, targetMemoId)

            searchViewModel.updateDataSetForEachMemoList { dataSetList ->
                dataSetList.filterNot { dataSet -> dataSet.memoInfoId == targetMemoId }
            }

            searchViewModel.updateDataSetForCategoryList { dataSetList ->
                val targetIndex = dataSetList.indexOf(dataSetList.find { it.name == category })
                val targetListSize = dataSetList[targetIndex].listSize
                val updatedTarget = dataSetList[targetIndex].copy(listSize = targetListSize - 1)

                //CategoryがDefaultValueでなく且つ中身が空になった場合はそのcategoryをリストから削除する。
                //それ以外の場合はcategoryの中身の数だけ減らす
                when (targetListSize == 1 && category != fragment.getString(
                    R.string.memo_category_default_value)) {
                    true -> {
                        dataSetList.take(targetIndex).plus(dataSetList.drop(targetIndex + 1))
                    }
                    false -> {
                        dataSetList.take(targetIndex)
                            .plus(updatedTarget)
                            .plus(dataSetList.drop(targetIndex + 1))
                    }
                }
            }
        }
    }
