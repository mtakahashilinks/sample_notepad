package com.example.samplenotepad.views.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.example.samplenotepad.*
import com.example.samplenotepad.data.getShowMassageForSavedFlow
import com.example.samplenotepad.data.resetValueOfShowMassageForSavedFlow
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.usecases.*
import com.example.samplenotepad.usecases.clearAll
import com.example.samplenotepad.usecases.initMemoContentsOperation
import com.example.samplenotepad.usecases.checkBoxOperation
import com.example.samplenotepad.viewModels.MemoEditViewModel
import com.example.samplenotepad.views.MemoAlertDialog
import kotlinx.android.synthetic.main.fragment_memo_edit.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class MemoEditFragment : Fragment() {

    companion object {
        private var instance: MemoEditFragment? = null

        internal fun getInstanceOrCreateNew(): MemoEditFragment =
            instance ?: MemoEditFragment().apply { if (instance == null) instance = this }

        private fun clearEditFragmentInstanceFlag() {
            instance = null
        }
    }


    private lateinit var editViewModel: MemoEditViewModel
    private lateinit var memoContainer: ConstraintLayout
    private var isShowingPopupWindow = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_memo_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editViewModel = MainActivity.editViewModel
        memoContainer = memoContentsContainerLayout

        CoroutineScope(Dispatchers.Main).launch(start = CoroutineStart.LAZY) {
            getShowMassageForSavedFlow().collect { fragmentType ->
                if (fragmentType is EditFragment)
                    this@MemoEditFragment.showSnackbarForSavedMassageAtEditMemo()

                resetValueOfShowMassageForSavedFlow()
            }
        }

        CoroutineScope(Dispatchers.Main).launch(start = CoroutineStart.LAZY) {
            editViewModel.getClearAllFocusInMemoContainerFlow().collect { flag ->
                if (flag) memoContentsContainerLayout.clearFocus()

                editViewModel.resetValueOfClearAllFocusInMemoContainerFlow()
            }
        }

        editViewModel.initEditViewModel()

        //メモテキスト編集に使うイメージボタンのクリックリスナー群
        templateImgBtn.setOnClickListener {
            val popupWindow = getTemplatePopupWindow()

            isShowingPopupWindow = when (isShowingPopupWindow) {
                true -> {
                    popupWindow.dismissTemplatePopupWindow(this)
                    false
                }
                false -> {
                    popupWindow.apply {
                        update()
                        showAsDropDown(templateImgBtn)
                    }
                    true
                }
            }

//            TemplateListDialogFragment.getInstanceOrCreateNew().show(
//                this@MemoEditFragment.requireActivity().supportFragmentManager, "template_list_dialog"
 //           )
        }

        checkBoxImgBtn.setOnClickListener {
            if (memoContainer.findFocus() != null) {
                val targetMemoRow = memoContainer.findFocus()
                Log.d("場所:checkBoxImgBtn.setOnClickListener", "targetMemoRowのId=${targetMemoRow.id}")
                if (targetMemoRow is MemoRow) targetMemoRow.checkBoxOperation()
            }
        }

        bulletListImgBtn.setOnClickListener {
            if (memoContainer.findFocus() != null) {
                val targetMemoRow = memoContainer.findFocus()
                Log.d("場所:bulletListImgBtn.setOnClickListener", "targetMemoRowのId=${targetMemoRow.id}")
                if (targetMemoRow is MemoRow) targetMemoRow.dotOperation()
            }
        }

        clearAllImgBtn.setOnClickListener {
            showAlertDialogToClearAll()
        }

        saveImgBtn.setOnClickListener {
            saveMemo(CreateNewMemo)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        when (editViewModel.getMemoInfo()) {
            null -> initMemoContentsOperation(this, editViewModel, memoContainer, CreateNewMemo)
            else -> initMemoContentsOperation(this, editViewModel, memoContainer, EditExistMemo)
        }
    }

    override fun onResume() {
        super.onResume()

        //ツールバーのタイトルをセット
        activity?.title = getString(R.string.appbar_title_for_edit_fragment)

        //ViewPagerとTabでFragmentを切り替えたときにFocusが外れるので取得しなおす
        if (memoContainer.focusedChild == null) firstMemoRow.requestFocus()
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onDestroy() {
        super.onDestroy()

        clearEditFragmentInstanceFlag()
    }


    private fun showAlertDialogToClearAll() {
        MemoAlertDialog(
            R.string.dialog_clear_all_title,
            R.string.dialog_clear_all_message,
            R.string.dialog_clear_all_positive_button,
            R.string.dialog_clear_all_negative_button,
            { dialog, id -> clearAll() },
            { dialog, id -> dialog.cancel() }
        ).show(requireActivity().supportFragmentManager, "clear_all_dialog")
    }

    internal fun setIsShowingPopupWindow(value: Boolean) {
        isShowingPopupWindow = value
    }
}
