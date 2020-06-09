package com.example.samplenotepad.views.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import arrow.core.None
import com.example.samplenotepad.*
import com.example.samplenotepad.data.AppDatabase
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.usecases.*
import com.example.samplenotepad.usecases.clearAll
import com.example.samplenotepad.usecases.initMemoContentsOperation
import com.example.samplenotepad.usecases.checkBoxOperation
import com.example.samplenotepad.viewModels.MemoEditViewModel
import com.example.samplenotepad.viewModels.MemoOptionViewModel
import com.example.samplenotepad.views.MemoAlertDialog
import kotlinx.android.synthetic.main.fragment_memo_edit.*
import kotlinx.coroutines.launch


class MemoEditFragment() : Fragment() {

    companion object {
        private lateinit var editViewModel: MemoEditViewModel
        private lateinit var optionViewModel: MemoOptionViewModel
        private lateinit var memoContainer: ConstraintLayout
    }


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

        memoContainer = memoContentsContainerLayout

        editViewModel.initEditViewModel(this)

        //メモテキスト編集に使うイメージボタンのクリックリスナー登録
        templateImgBtn.setOnClickListener {
            lifecycleScope.launch {
                val dao = AppDatabase.getDatabase(this@MemoEditFragment.requireContext()).memoInfoDao()
                val b = dao.getMemoInfoById(32)
          //      dao.deleteMemoInfo(b)
         //       val c = dao.getMemoInfo(1)
                Log.d("場所:aaaa", "memoInfo(1b)=$b")
         //       Log.d("場所:aaaa", "memoInfo(1c)=$c")
            }
        }

        checkBoxImgBtn.setOnClickListener {
            val targetMemoRow = memoContentsContainerLayout.findFocus()
            Log.d("場所:checkBoxImgBtn.setOnClickListener", "targetMemoRowのId=${targetMemoRow.id}")
            if (targetMemoRow is MemoRow) targetMemoRow.checkBoxOperation()
        }

        bulletListImgBtn.setOnClickListener {
            val targetMemoRow = memoContentsContainerLayout.findFocus()
            Log.d("場所:bulletListImgBtn.setOnClickListener", "targetMemoRowのId=${targetMemoRow.id}")
            if (targetMemoRow is MemoRow) targetMemoRow.dotOperation()
        }

        clearAllImgBtn.setOnClickListener {
            showClearAllAlertDialog()
        }

        saveImgBtn.setOnClickListener {
            saveMemo()
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
    }


    internal fun setValues(editVM: MemoEditViewModel,
                           optionVM: MemoOptionViewModel) {
        editViewModel = editVM
        optionViewModel = optionVM
    }

    private fun showClearAllAlertDialog() {
        MemoAlertDialog(
            R.string.dialog_clear_all_title,
            R.string.dialog_clear_all_message,
            R.string.dialog_clear_all_positive_button,
            R.string.dialog_clear_all_negative_button,
            { dialog, id -> clearAll() },
            { dialog, id -> dialog.cancel() }
        ).show(requireActivity().supportFragmentManager, "clear_all")
    }
}
