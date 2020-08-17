package com.example.samplenotepad.usecases.searchMemoListRecyclerView

import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.samplenotepad.R
import com.example.samplenotepad.data.deleteMemoByIdIO
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.MemoAlertDialog


//SwipeでリストのItemを削除するためのCallback
internal fun SearchMemoListAdapter.getCallbackForItemTouchHelper(
    activity: FragmentActivity,
    searchViewModel: SearchViewModel
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
                R.string.dialog_delete_memo_message,
                R.string.dialog_delete_memo_positive_button,
                R.string.dialog_delete_memo_negative_button,
                { dialog, id ->
                    this@getCallbackForItemTouchHelper.apply {
                        deleteSelectedItemFromDataSetList(viewHolder.adapterPosition)
                        notifyDataSetChanged()
                    }
                },
                { dialog, id -> this@getCallbackForItemTouchHelper.notifyDataSetChanged() }
            ).show(
                activity.supportFragmentManager,
                "search_each_memo_swipe_delete_dialog"
            )
        }


        private fun deleteSelectedItemFromDataSetList(adapterPosition: Int) {
            val targetMemoInfo = searchViewModel.getDataSetForMemoList()[adapterPosition]
            val targetMemoInfoId = targetMemoInfo.rowid

            deleteMemoByIdIO(targetMemoInfoId)

            searchViewModel.apply {
                targetMemoInfo.cancelAllAlarm()

                //削除したMemoInfoをリストから除外する
                updateDataSetForMemoList { dataSetList ->
                    dataSetList.filterNot { memoInfo -> memoInfo.rowid == targetMemoInfoId }
                }
            }
        }
    }