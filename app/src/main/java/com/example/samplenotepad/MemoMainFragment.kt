package com.example.samplenotepad

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.samplenotepad.MemoMainViewModel.MemoContentsOperation
import kotlinx.android.synthetic.main.fragment_memo_main.*


class MemoMainFragment : Fragment() {

    private lateinit var viewModel: MemoMainViewModel
    private lateinit var memoContainer: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = requireActivity().run {
            ViewModelProvider.NewInstanceFactory().create(MemoMainViewModel::class.java)
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_memo_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        memoContainer = memoContentsContainerLayout

        // memoContentsリストが空の場合、最初のMemoRowをセットする
        when (MemoContentsOperation.getMemoContents().contentsList.isEmpty()) {
            true ->
                MemoContentsOperation.executeMemoOperation(
                    viewModel, CreateFirstMemoRow(this, memoContainer, Text(""))
                )
            false -> return
        }

        //メモテキスト編集に使うイメージボタンのクリックリスナー登録
        checkBoxImgBtn.setOnClickListener {
            val targetMemoRow = memoContentsContainerLayout.findFocus()
            Log.d("場所:checkBoxImgBtn.setOnClickListener", "targetMemoRowのId=${targetMemoRow.id}")
            if (targetMemoRow is MemoRow) targetMemoRow.operationCheckBox(this, viewModel)
        }

        bulletListImgBtn.setOnClickListener {
            val targetMemoRow = memoContentsContainerLayout.findFocus()
            Log.d("場所:bulletListImgBtn.setOnClickListener", "targetMemoRowのId=${targetMemoRow.id}")
            if (targetMemoRow is MemoRow) targetMemoRow.operationBullet(this, viewModel)
        }

        clearAllImgBtn.setOnClickListener {
            memoContainer.apply {
                removeAllViews()
                MemoContentsOperation.executeMemoOperation(viewModel, ClearAll())
                MemoContentsOperation.executeMemoOperation(
                    viewModel, CreateFirstMemoRow(this@MemoMainFragment, memoContainer , Text(""))
                )
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        //ツールバーのタイトルをセット
        activity?.title = getString(R.string.input_new_memo)
    }
}
