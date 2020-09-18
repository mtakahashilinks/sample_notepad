package com.example.samplenotepad.usecases.memoTemplateListRecyclerView

import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.samplenotepad.R
import com.example.samplenotepad.data.deleteTemplateIO
import com.example.samplenotepad.data.deleteTemplateNameListIO
import com.example.samplenotepad.data.saveTemplateNameListIO
import com.example.samplenotepad.entities.AdapterPosition
import com.example.samplenotepad.viewModels.MainViewModel
import com.example.samplenotepad.views.MemoAlertDialog
import com.example.samplenotepad.views.main.MemoEditFragment
import kotlinx.android.synthetic.main.template_popup_window.view.*


//SwipeでリストのItemを削除するためのCallback
internal fun MemoTemplateListAdapter.getItemTouchHelperCallback(
    editFragment: MemoEditFragment,
    mainViewModel: MainViewModel,
    contentLayout: View
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
                R.string.dialog_template_swipe_delete_message,
                R.string.dialog_template_swipe_delete_positive_button,
                R.string.dialog_template_swipe_delete_negative_button,
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
            val templateNameList = mainViewModel.getTemplateNameList()
            val targetTemplateName = templateNameList[this]
            val modifiedTemplateList = templateNameList.filterIndexed { index, s -> index != this }

            deleteTemplateIO(targetTemplateName)

            when (modifiedTemplateList.isEmpty()) {
                true -> deleteTemplateNameListIO()
                false -> saveTemplateNameListIO(modifiedTemplateList)
            }

            mainViewModel.updateTemplateNameList { modifiedTemplateList }

            //エラーメッセージが表示されていたら非表示にする(５個以上保存できないエラーメッセージの為)
            if (contentLayout.templateNameErrorTextView.visibility == View.VISIBLE)
                contentLayout.templateNameErrorTextView.visibility = View.GONE

            if (modifiedTemplateList.isEmpty())
                contentLayout.listActionTextView.visibility = View.GONE
        }
    }
