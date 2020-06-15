package com.example.samplenotepad.views.main

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.samplenotepad.R
import com.example.samplenotepad.data.saveTemplateNameListToFile
import com.example.samplenotepad.data.saveTemplateToFile
import com.example.samplenotepad.usecases.memoTemplateRecyclerView.MemoTemplateAdapter
import com.example.samplenotepad.usecases.memoTemplateRecyclerView.getItemTouchHelperCallback
import com.example.samplenotepad.viewModels.MemoEditViewModel
import kotlinx.android.synthetic.main.fragment_memo_edit.*
import kotlinx.android.synthetic.main.fragment_template_list_dialog.view.*


class TemplateListDialogFragment : DialogFragment() {
    companion object {
        private lateinit var instanceOfFragment: TemplateListDialogFragment

        internal fun getInstanceOrCreateNewOne(): TemplateListDialogFragment =
            when (::instanceOfFragment.isInitialized) {
                true -> instanceOfFragment
                false -> {
                    val dialogFragment = TemplateListDialogFragment()
                    instanceOfFragment = dialogFragment
                    dialogFragment
                }
            }
    }


    private val editFragment = MemoEditFragment.getInstanceOrCreateNewOne()
    private val editViewModel = MemoEditViewModel.getInstanceOrCreateNewOne()


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view =
            requireActivity().layoutInflater.inflate(R.layout.fragment_template_list_dialog, null, false)
        val errorTextView = view.templateNameErrorTextView

        if (errorTextView.visibility == View.VISIBLE) errorTextView.visibility = View.GONE

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

            when {
                newTemplateName.isEmpty() -> {
                    errorTextView.showErrorText(R.string.template_add_error_not_input)
                    return@setOnClickListener
                }
                editViewModel.getTemplateNameList().contains(newTemplateName) -> {
                    errorTextView.showErrorText(R.string.template_add_error_has_same)
                    return@setOnClickListener
                }
                editViewModel.getTemplateNameList().size == 5 -> {
                    errorTextView.showErrorText(R.string.template_add_error_max_amount)
                    return@setOnClickListener
                }
            }

            editFragment.memoContentsContainerLayout.clearFocus()
            editViewModel.updateTemplateNameList { list -> list.plus(newTemplateName) }

            saveTemplateNameListToFile(editFragment.requireContext(), editViewModel.getTemplateNameList())

            saveTemplateToFile(
                editFragment.requireContext(), newTemplateName, editViewModel.getMemoContents()
            )

            dialog.dismiss()

            editFragment.getFocusAndShowSoftwareKeyboard(editFragment.memoContentsContainerLayout)
        }

        return dialog
    }

    private fun TextView.showErrorText(massageId: Int) {
        this.apply {
            visibility = View.VISIBLE
            setText(massageId)
        }
    }
}
