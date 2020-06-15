package com.example.samplenotepad.usecases.memoTemplateRecyclerView

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.samplenotepad.R
import com.example.samplenotepad.data.deleteTemplateFile
import com.example.samplenotepad.data.saveTemplateNameListToFile
import com.example.samplenotepad.viewModels.MemoEditViewModel
import com.example.samplenotepad.views.MemoAlertDialog
import com.example.samplenotepad.views.main.MemoEditFragment


//SwipeでリストのItemを削除するためのCallback
internal fun RecyclerView.getItemTouchHelperCallback(
    editFragment: MemoEditFragment,
    editViewModel: MemoEditViewModel,
    adapter: MemoTemplateAdapter
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
                    adapter.apply {
                        deleteTemplateFileAndUpdateNameList(viewHolder.adapterPosition)
                        notifyDataSetChanged()
                    }
                },
                { dialog, id -> adapter.notifyDataSetChanged() }
            ).show(
                editFragment.requireActivity().supportFragmentManager,
                "memo_template_swipe_delete_dialog"
            )
        }


        fun MemoTemplateAdapter.deleteTemplateFileAndUpdateNameList(adapterPosition: Int) {
            val templateNameList = editViewModel.getTemplateNameList()
            val targetTemplateName = templateNameList[adapterPosition]
            val modifiedTemplateList = templateNameList.take(adapterPosition)
                .plus(templateNameList.drop(adapterPosition + 1))

            deleteTemplateFile(editFragment, targetTemplateName)

            saveTemplateNameListToFile(editFragment.requireContext(), modifiedTemplateList)
            editViewModel.updateTemplateNameList { modifiedTemplateList }
        }
    }
