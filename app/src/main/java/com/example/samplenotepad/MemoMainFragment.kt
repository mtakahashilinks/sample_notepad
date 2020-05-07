package com.example.samplenotepad

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import arrow.core.None
import com.example.samplenotepad.MemoMainViewModel.MemoContentsOperation.clearAll
import com.example.samplenotepad.MemoMainViewModel.MemoContentsOperation.initMemoContentsOperation
import com.example.samplenotepad.MemoMainViewModel.MemoContentsOperation.operationCheckBox
import com.example.samplenotepad.MemoMainViewModel.MemoContentsOperation.dotOperation
import kotlinx.android.synthetic.main.fragment_memo_main.*


class MemoMainFragment : Fragment() {

    private lateinit var mainViewModel: MemoMainViewModel
    private lateinit var optionViewModel: MemoOptionViewModel
    private lateinit var memoContainer: ConstraintLayout

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


        //メモテキスト編集に使うイメージボタンのクリックリスナー登録
        menuImgBtn.setOnClickListener {
            showMenuPopup(menuImgBtn, context)
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
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        //ツールバーのタイトルをセット
        activity?.title = getString(R.string.input_new_memo)
    }


    internal fun setViewModel(mainVM: MemoMainViewModel, optionVM: MemoOptionViewModel) {
        mainViewModel = mainVM
        optionViewModel = optionVM
    }
}
