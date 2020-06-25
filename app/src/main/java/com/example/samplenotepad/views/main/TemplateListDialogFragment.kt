package com.example.samplenotepad.views.main

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.samplenotepad.R
import com.example.samplenotepad.usecases.memoTemplateRecyclerView.MemoTemplateAdapter
import com.example.samplenotepad.usecases.memoTemplateRecyclerView.getItemTouchHelperCallback
import com.example.samplenotepad.viewModels.MemoEditViewModel
import kotlinx.android.synthetic.main.fragment_memo_edit.*
import kotlinx.android.synthetic.main.fragment_template_list_dialog.view.*


class TemplateListDialogFragment : DialogFragment() {

    companion object {
        private var instance: TemplateListDialogFragment? = null

        internal fun getInstanceOrCreateNew(): TemplateListDialogFragment =
            instance ?: TemplateListDialogFragment().apply { if (instance == null) instance = this }

        internal fun clearTemplateListDialogFragmentInstanceFlag() {
            instance = null
        }
    }


    private lateinit var editFragment: MemoEditFragment
    private lateinit var editViewModel: MemoEditViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        editFragment = MemoEditFragment.getInstanceOrCreateNew()
        editViewModel = MainActivity.editViewModel

        val view = requireActivity().layoutInflater.inflate(
            R.layout.fragment_template_list_dialog, null, false
        ).apply { templateNameEditText.requestFocus() }

        view.templateRecyclerView.apply {
            val mAdapter = MemoTemplateAdapter(editFragment, editViewModel)

            layoutManager = LinearLayoutManager(this@TemplateListDialogFragment.requireContext())
            adapter = mAdapter
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            //スワイプでリストItemを削除する為の処理
            ItemTouchHelper(getItemTouchHelperCallback(editFragment, editViewModel, mAdapter))
                .attachToRecyclerView(this)
        }

        val dialog = activity.let { AlertDialog.Builder(requireContext()) }.apply {
            setTitle(R.string.template_list_title)
            setView(view)
        }.create()

        view.addNewTemplateBtn.setOnClickListener {
            val newTemplateName = view.templateNameEditText.text.toString()
            val errorTextView = view.templateNameErrorTextView

            when {
                newTemplateName.isEmpty() -> {
                    errorTextView.showErrorText(R.string.error_not_input_new_name)
                    return@setOnClickListener
                }
                editViewModel.getTemplateNameList().contains(newTemplateName) -> {
                    errorTextView.showErrorText(R.string.error_already_has_same_name)
                    return@setOnClickListener
                }
                editViewModel.getTemplateNameList().size >= 5 -> {
                    errorTextView.showErrorText(R.string.template_add_error_max_amount)
                    return@setOnClickListener
                }
            }

            editFragment.memoContentsContainerLayout.clearFocus()

            editViewModel.addItemInTemplateNameListAndSaveTemplateFile(newTemplateName)

            dialog.dismiss()
        }

        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        clearTemplateListDialogFragmentInstanceFlag()
    }

    private fun TextView.showErrorText(massageId: Int) =
        this.apply {
            visibility = View.VISIBLE
            setText(massageId)
        }
}
