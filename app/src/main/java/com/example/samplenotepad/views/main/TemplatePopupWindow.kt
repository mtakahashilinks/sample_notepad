package com.example.samplenotepad.views.main

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.samplenotepad.R
import com.example.samplenotepad.usecases.memoTemplateRecyclerView.MemoTemplateAdapter
import com.example.samplenotepad.usecases.memoTemplateRecyclerView.getItemTouchHelperCallback
import com.example.samplenotepad.viewModels.MemoEditViewModel
import kotlinx.android.synthetic.main.fragment_memo_edit.*
import kotlinx.android.synthetic.main.template_popup_window.view.*


private var popupWindow: PopupWindow? = null

internal fun getTemplatePopupWindow(editFragment: MemoEditFragment): PopupWindow =
    popupWindow
        ?: createPopupWindow(editFragment).apply { if (popupWindow == null) popupWindow = this }

internal fun clearTemplatePopupWindowFlag() {
    popupWindow = null
}

internal fun PopupWindow.dismissTemplatePopupWindow(fragment: MemoEditFragment) {
    this.dismiss()
    clearTemplatePopupWindowFlag()
    fragment.setIsShowingPopupWindow(false)
}

private fun createPopupWindow(editFragment: MemoEditFragment): PopupWindow {
    val layoutView = editFragment.requireActivity().layoutInflater.inflate(
        R.layout.template_popup_window, null, false
    ).apply {
        //textが変更されたときエラーメッセージが表示されていれば非表示にする
        templateNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                if (templateNameErrorTextView.visibility == View.VISIBLE)
                    templateNameErrorTextView.visibility = View.GONE
            }
        })

        //listが空かどうかで、listActionの説明文の表示・非表示を切り替える
        when (MainActivity.editViewModel.getTemplateNameList().isNotEmpty()) {
            true -> listActionTextView.visibility = View.VISIBLE
            false -> listActionTextView.visibility = View.GONE
        }
    }

    return PopupWindow(editFragment.context).apply {
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        contentView = layoutView
        isOutsideTouchable = true
        isFocusable = true
        setBackgroundDrawable(
            ResourcesCompat.getDrawable(
                editFragment.resources,
                R.drawable.popup_background,
                editFragment.requireActivity().theme
            )
        )

        setOnDismissListener {
            clearTemplatePopupWindowFlag()
            editFragment.setIsShowingPopupWindow(false)
        }
    }.apply {
        setTextForTemplatePopupWindow(editFragment, layoutView)
        layoutView.templateNameEditText.requestFocus()

        //dismissボタンのリスナー登録
        layoutView.dismissImgBtn.setOnClickListener { this.dismiss() }
    }
}

private fun PopupWindow.setTextForTemplatePopupWindow(editFragment: MemoEditFragment, layout: View) {
    val editViewModel = MainActivity.editViewModel

    layout.apply {
        setRecyclerViewOnPopupWindow(editFragment, editViewModel, this)
        setClickListenerOnPopupWindow(editFragment, editViewModel, this@setTextForTemplatePopupWindow)
    }
}

private fun View.setRecyclerViewOnPopupWindow(
    fragment: MemoEditFragment,
    viewModel: MemoEditViewModel,
    contentLayout: View
) {
    this.apply {
        templateRecyclerView.apply {
            val memoTemplateAdapter = MemoTemplateAdapter(fragment, viewModel)

            layoutManager = LinearLayoutManager(fragment.requireContext())
            adapter = memoTemplateAdapter
            setHasFixedSize(true)
            addItemDecoration(
                DividerItemDecoration(
                    fragment.requireContext(), DividerItemDecoration.VERTICAL
                )
            )

            //スワイプでリストItemを削除する為の処理
            ItemTouchHelper(memoTemplateAdapter
                .getItemTouchHelperCallback(fragment, viewModel, contentLayout))
                .attachToRecyclerView(this)
        }
    }
}

private fun View.setClickListenerOnPopupWindow(
    fragment: MemoEditFragment,
    viewModel: MemoEditViewModel,
    popupWindow: PopupWindow
) {
    this.apply {
        addNewTemplateBtn.setOnClickListener {
            val newTemplateName = this.templateNameEditText.text.toString()
            val errorTextView = this.templateNameErrorTextView

            when {
                viewModel.getTemplateNameList().size >= 5 -> {
                    errorTextView.showErrorText(R.string.template_add_error_max_amount)
                    return@setOnClickListener
                }
                newTemplateName.isEmpty() -> {
                    errorTextView.showErrorText(R.string.error_not_input_new_name)
                    return@setOnClickListener
                }
                viewModel.getTemplateNameList().contains(newTemplateName) -> {
                    errorTextView.showErrorText(R.string.error_already_has_same_name)
                    return@setOnClickListener
                }
            }

            fragment.memoContentsContainerLayout.clearFocus()

            viewModel.addItemInTemplateNameListAndSaveTemplateFile(newTemplateName)

            popupWindow.dismissTemplatePopupWindow(fragment)
        }
    }
}

private fun TextView.showErrorText(massageId: Int) =
    this.apply {
        visibility = View.VISIBLE
        setText(massageId)
    }
