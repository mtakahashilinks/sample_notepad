package com.example.samplenotepad.usecases.searchTopListRecyclerView

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.samplenotepad.R
import com.example.samplenotepad.data.deleteMemoByCategoryIO
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.MemoAlertDialog
import com.example.samplenotepad.views.search.SearchTopFragment


//SwipeでリストのItemを削除するためのCallback
internal fun SearchViewModel.getItemTouchHelperCallback(
    fragment: SearchTopFragment,
    adapter: SearchTopListAdapter
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
            when (viewHolder.adapterPosition == 0) {
                true -> {
                    MemoAlertDialog(
                        R.string.dialog_search_top_swipe_delete_others_title,
                        R.string.dialog_search_top_swipe_delete_others_message,
                        R.string.dialog_search_top_swipe_delete_others_positive_button,
                        R.string.dialog_search_top_swipe_delete_others_negative_button,
                        { dialog, id ->
                            adapter.apply {
                                deleteItemFromDataSetForCategoryList(viewHolder.adapterPosition)
                                adapter.notifyDataSetChanged()
                            }
                        },
                        { dialog, id -> adapter.notifyDataSetChanged() }
                    ).show(
                        fragment.requireActivity().supportFragmentManager,
                        "search_top_swipe_delete_dialog"
                    )
                }
                false -> {
                    MemoAlertDialog(
                        R.string.dialog_search_top_swipe_delete_title,
                        R.string.dialog_search_top_swipe_delete_message,
                        R.string.dialog_search_top_swipe_delete_positive_button,
                        R.string.dialog_search_top_swipe_delete_negative_button,
                        { dialog, id ->
                            adapter.apply {
                                deleteItemFromDataSetForCategoryList(viewHolder.adapterPosition)
                                notifyDataSetChanged()
                            }
                        },
                        { dialog, id -> adapter.notifyDataSetChanged() }
                    ).show(
                        fragment.requireActivity().supportFragmentManager,
                        "search_top_swipe_delete_dialog"
                    )
                }
            }
        }


        fun SearchTopListAdapter.deleteItemFromDataSetForCategoryList(adapterPosition: Int) {
            val targetCategory = getDataSetForCategoryList()[adapterPosition].name

            deleteMemoByCategoryIO(targetCategory)

            //リストから削除されたCategoryを除去。ただし先頭(DefaultCategory)の場合はlistSizeを0に変更するだけ
            when (adapterPosition == 0) {
                true -> {
                    updateDataSetForCategoryList { categoryTupleList ->
                        categoryTupleList.mapIndexed { index, categoryTuple ->
                            when (index) {
                                0 -> categoryTuple.copy(listSize = 0)
                                else -> categoryTuple
                            }
                        }
                    }
                }
                false -> updateDataSetForCategoryList { categoryTupleList ->
                    categoryTupleList.filterNot { categoryTuple -> categoryTuple.name == targetCategory }
                }
            }
        }
    }
