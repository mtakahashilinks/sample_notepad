package com.example.samplenotepad.usecases.memoTemplateRecyclerView

import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.samplenotepad.R
import com.example.samplenotepad.entities.EditExistMemo
import com.example.samplenotepad.usecases.clearAll
import com.example.samplenotepad.usecases.initMemoContentsOperation
import com.example.samplenotepad.viewModels.MemoEditViewModel
import com.example.samplenotepad.views.main.MemoEditFragment
import com.example.samplenotepad.views.main.dismissTemplatePopupWindow
import com.example.samplenotepad.views.main.getTemplatePopupWindow
import kotlinx.android.synthetic.main.fragment_rename_dialog.view.*
import kotlinx.android.synthetic.main.fragment_memo_edit.*
import kotlinx.android.synthetic.main.template_list_row.view.*


class MemoTemplateAdapter(
    private val editFragment: MemoEditFragment,
    private val editViewModel: MemoEditViewModel
) : RecyclerView.Adapter<MemoTemplateAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val templateNameView: TextView = view.memoTemplateNameTextView
        val templateNumberView: TextView = view.templateNumberTextView
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.template_list_row, parent, false)
        val viewHolder = ViewHolder(view)

        viewHolder.itemView.setOnClickListener {
            clearAll()
            editViewModel.loadTemplateAndUpdateMemoContents(viewHolder.templateNameView.text.toString())

            getTemplatePopupWindow().dismissTemplatePopupWindow(editFragment)

            initMemoContentsOperation(
                editFragment,
                editViewModel,
                editFragment.memoContentsContainerLayout,
                EditExistMemo
            )
        }

        viewHolder.itemView.setOnLongClickListener { view ->
            val layoutView = editFragment.requireActivity().layoutInflater.inflate(
                R.layout.fragment_rename_dialog, null, false
            ).apply {
                newNameEitText.requestFocus()
            }
            val targetTemplateName = editViewModel.getTemplateNameList()[viewHolder.adapterPosition]

            layoutView.nameBeforeChangeTextView.text = String.format(
                editFragment.resources.getString(
                    R.string.dialog_rename_name_before_change
                ), targetTemplateName
            )

            AlertDialog.Builder(editFragment.requireContext()).apply {
                setTitle(R.string.dialog_rename_template_title)
                setView(layoutView)
                setPositiveButton(R.string.dialog_rename_positive_button, null)
                setNegativeButton(R.string.dialog_rename_negative_button) { dialog, id ->
                    dialog.dismiss()
                }
            }.show()
                .setPositiveButtonAction(layoutView, targetTemplateName)

            true
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val templateName = editViewModel.getTemplateNameList()[position]

        holder.templateNumberView.text = String.format(
            editFragment.resources.getString(R.string.template_number_view_text), position + 1
        )
        holder.templateNameView.text = templateName
    }

    override fun getItemCount(): Int = editViewModel.getTemplateNameList().size


    private fun AlertDialog.setPositiveButtonAction(layoutView: View, targetTemplateName: String) =
        this.getButton(DialogInterface.BUTTON_POSITIVE).apply {
            setOnClickListener {
                val newTemplateName = layoutView.newNameEitText.text.toString()
                val errorTextView = layoutView.errorMassageTextView

                when {
                    newTemplateName.isEmpty() -> {
                        errorTextView.showErrorText(R.string.error_not_input_new_name)
                    }
                    editViewModel.getTemplateNameList().contains(newTemplateName) -> {
                        errorTextView.showErrorText(R.string.error_already_has_same_name)
                    }
                    else -> {
                        editViewModel.renameItemInTemplateNameListAndTemplateFilesName(
                            targetTemplateName, newTemplateName
                        )
                        this@MemoTemplateAdapter.notifyDataSetChanged()
                        this@setPositiveButtonAction.dismiss()
                    }
                }
            }
        }

    private fun TextView.showErrorText(massageId: Int) =
        this.apply {
            visibility = View.VISIBLE
            setText(massageId)
        }
}
