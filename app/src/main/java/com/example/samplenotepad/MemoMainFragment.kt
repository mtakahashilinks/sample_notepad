package com.example.samplenotepad

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import arrow.core.None
import com.example.samplenotepad.MemoMainViewModel.MemoContentsOperation.clearAll
import com.example.samplenotepad.MemoMainViewModel.MemoContentsOperation.initMemoContentsOperation
import com.example.samplenotepad.MemoMainViewModel.MemoContentsOperation.operationCheckBox
import com.example.samplenotepad.MemoMainViewModel.MemoContentsOperation.dotOperation
import com.example.samplenotepad.MemoOptionViewModel.Companion.getOptionValuesForSave
import kotlinx.android.synthetic.main.fragment_memo_main.*
import kotlinx.coroutines.launch


class MemoMainFragment : Fragment() {

    companion object {
        private lateinit var mainViewModel: MemoMainViewModel
        private lateinit var optionViewModel: MemoOptionViewModel
        private lateinit var memoContainer: ConstraintLayout
        private lateinit var appDatabase: AppDatabase
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

        //viewPagerのバグのためのとりあえずのメソッド
        MemoMainViewModel.ForFirstFocusInMainFragment.setFragmentAndContainer(this, memoContainer)

        initMemoContentsOperation(this, mainViewModel, memoContainer, None)
        val dbDao = AppDatabase.getDatabase(requireContext()).memoInfoDao()


        //メモテキスト編集に使うイメージボタンのクリックリスナー登録
        menuImgBtn.setOnClickListener {
            lifecycleScope.launch {
                val dao = appDatabase.memoInfoDao()
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
            MemoMainViewModel.MemoContentsOperation.saveOperation(
                SaveMemoInfo(getOptionValuesForSave(), appDatabase)
            )
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        //ツールバーのタイトルをセット
        activity?.title = getString(R.string.input_new_memo)
    }


    internal fun setValues(mainVM: MemoMainViewModel, optionVM: MemoOptionViewModel, db: AppDatabase) {
        mainViewModel = mainVM
        optionViewModel = optionVM
        appDatabase = db
    }
}
