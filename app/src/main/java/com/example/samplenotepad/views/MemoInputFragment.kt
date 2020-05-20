package com.example.samplenotepad.views

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
import com.example.samplenotepad.viewModels.MemoOptionViewModel.Companion.getOptionValuesForSave
import com.example.samplenotepad.data.AppDatabase
import com.example.samplenotepad.entities.MemoRow
import com.example.samplenotepad.entities.SaveMemoInfo
import com.example.samplenotepad.usecases.*
import com.example.samplenotepad.usecases.clearAll
import com.example.samplenotepad.usecases.initMemoContentsOperation
import com.example.samplenotepad.usecases.operationCheckBox
import com.example.samplenotepad.usecases.saveOperation
import com.example.samplenotepad.viewModels.MemoInputViewModel
import com.example.samplenotepad.viewModels.MemoOptionViewModel
import kotlinx.android.synthetic.main.fragment_memo_main.*
import kotlinx.coroutines.launch


class MemoInputFragment : Fragment() {

    companion object {
        private lateinit var inputViewModel: MemoInputViewModel
        private lateinit var optionViewModel: MemoOptionViewModel
        private lateinit var memoContainer: ConstraintLayout
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_memo_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        memoContainer = memoContentsContainerLayout

        inputViewModel.initMainViewModel(this)

        //メモテキスト編集に使うイメージボタンのクリックリスナー登録
        menuImgBtn.setOnClickListener {
            lifecycleScope.launch {
                val dao = AppDatabase.getDatabase(this@MemoInputFragment.requireContext()).memoInfoDao()
                val b = dao.getMemoInfo(1)
          //      dao.deleteMemoInfo(b)
         //       val c = dao.getMemoInfo(1)
                Log.d("場所:aaaa", "memoInfo(1b)=$b")
         //       Log.d("場所:aaaa", "memoInfo(1c)=$c")
            }
        }

        checkBoxImgBtn.setOnClickListener {
            val targetMemoRow = memoContentsContainerLayout.findFocus()
            Log.d("場所:checkBoxImgBtn.setOnClickListener", "targetMemoRowのId=${targetMemoRow.id}")
            if (targetMemoRow is MemoRow) targetMemoRow.operationCheckBox()
        }

        bulletListImgBtn.setOnClickListener {
            val targetMemoRow = memoContentsContainerLayout.findFocus()
            Log.d("場所:bulletListImgBtn.setOnClickListener", "targetMemoRowのId=${targetMemoRow.id}")
            if (targetMemoRow is MemoRow) targetMemoRow.dotOperation()
        }

        clearAllImgBtn.setOnClickListener { clearAll() }

        saveImgBtn.setOnClickListener {
            saveOperation(
                SaveMemoInfo(
                    getOptionValuesForSave()
                )
            )
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initMemoContentsOperation(
            this,
            inputViewModel,
            memoContainer,
            None
        )
    }

    override fun onResume() {
        super.onResume()

        //ツールバーのタイトルをセット
        activity?.title = getString(R.string.input_new_memo)
    }


    internal fun setValues(inputVM: MemoInputViewModel, optionVM: MemoOptionViewModel) {
        inputViewModel = inputVM
        optionViewModel = optionVM
    }
}
