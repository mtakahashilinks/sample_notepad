package com.example.samplenotepad.usecases.memoTemplateRecyclerView

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.samplenotepad.R
import com.example.samplenotepad.data.deleteTemplateFileIO
import com.example.samplenotepad.data.saveTemplateNameListToFileIO
import com.example.samplenotepad.entities.AdapterPosition
import com.example.samplenotepad.viewModels.MemoEditViewModel
import com.example.samplenotepad.views.MemoAlertDialog
import com.example.samplenotepad.views.main.MemoEditFragment


//SwipeでリストのItemを削除するためのCallback
internal fun MemoTemplateAdapter.getItemTouchHelperCallback(
    editFragment: MemoEditFragment,
    editViewModel: MemoEditViewModel
) =
    object : ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.END)
        }

        override fun onMove(recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            MemoAlertDialog(
                R.string.dialog_memo_template_swipe_delete_title,
                R.string.dialog_memo_template_swipe_delete_message,
                R.string.dialog_memo_template_swipe_delete_positive_button,
                R.string.dialog_memo_template_swipe_delete_negative_button,
                { dialog, id ->
                    viewHolder.adapterPosition.deleteTemplateFileAndUpdateNameList()
                    this@getItemTouchHelperCallback.notifyDataSetChanged()
                },
                { dialog, id -> this@getItemTouchHelperCallback.notifyDataSetChanged() }
            ).show(
                editFragment.requireActivity().supportFragmentManager,
                "memo_template_swipe_delete_dialog"
            )
        }


        private fun AdapterPosition.deleteTemplateFileAndUpdateNameList() {
            val templateNameList = editViewModel.getTemplateNameList()
            val targetTemplateName = templateNameList[this]
            val modifiedTemplateList = templateNameList.take(this)
                .plus(templateNameList.drop(this + 1))

            deleteTemplateFileIO(targetTemplateName)

            saveTemplateNameListToFileIO(modifiedTemplateList)
            editViewModel.updateTemplateNameList { modifiedTemplateList }
        }
    }
