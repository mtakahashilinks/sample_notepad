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
                R.string.dialog_search_in_category_swipe_delete_title,
                R.string.dialog_search_in_category_swipe_delete_message,
                R.string.dialog_search_in_category_swipe_delete_positive_button,
                R.string.dialog_search_in_category_swipe_delete_negative_button,
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
            val targetMemoId = targetMemoInfo.rowid
            val targetCategory = targetMemoInfo.category

            deleteMemoByIdIO(targetMemoId)

            searchViewModel.apply {
                targetMemoInfo.cancelAllAlarm()

                //削除したMemoInfoをリストから除外する
                updateDataSetForMemoList { dataSetList ->
                    dataSetList.filterNot { memoInfo -> memoInfo.rowid == targetMemoId }
                }

                //DataSetForCategoryListを更新。
                //DataSetForCategoryListはSearchTopの表示のためだけにあるので現在ではここで更新する
                //必要はなく今後のアプリ変更のために念のためここで更新している。
                updateDataSetForCategoryList { dataSetList ->
                    val targetIndex =
                        dataSetList.indexOf(dataSetList.find { it.name == targetCategory })
                    val targetListSize = dataSetList[targetIndex].listSize
                    val updatedTarget = dataSetList[targetIndex].copy(listSize = targetListSize - 1)

                    //CategoryがDefaultValueでなく且つ中身が空になった場合はそのcategoryをリストから削除する。
                    //それ以外の場合はcategoryの中身の数だけ減らす
                    when (targetListSize == 1 && targetCategory != activity.getString(
                        R.string.memo_category_default_value
                    )) {
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
    }