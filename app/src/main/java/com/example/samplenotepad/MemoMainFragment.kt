package com.example.samplenotepad

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.fragment_memo_main.*


class MemoMainFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var memoContainer: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = requireActivity().run {
            ViewModelProvider.NewInstanceFactory().create(MainViewModel::class.java)
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

        // memoContentsリストが空の場合、最初のEditTextをセットする
        when (viewModel.getMemoContents().contentsList.isEmpty()) {
            true ->
                MainViewModel.Queue4MemoContents.execute(
                    this, viewModel, CREATE_FIRST_MEMO_ROW, EditText(this.context), Text("")
                )
            false -> return
        }

        //メモテキスト編集に使うイメージボタンのクリックリスナー登録
        checkBoxImgBtn.setOnClickListener {
            val targetEditText = memoContentsContainerLayout.findFocus()
            Log.d("場所:checkBoxImgBtn.setOnClickListener", "targetEditTextId=${targetEditText.id}")
            if (targetEditText is EditText) targetEditText.setCheckBox(this, viewModel)
        }

        bulletListImgBtn.setOnClickListener {
            val targetEditText = memoContentsContainerLayout.findFocus()
            Log.d("場所:bulletListImgBtn.setOnClickListener", "targetEditTextId=${targetEditText.id}")
            if (targetEditText is EditText) targetEditText.setBullet(this, viewModel)
        }

        clearAllImgBtn.setOnClickListener {
            memoContainer.apply {
                removeAllViews()
                MainViewModel.Queue4MemoContents.execute(
                    this@MemoMainFragment, viewModel, CLEAR_ALL,
                    EditText(this@MemoMainFragment.context), Text("")
                )
                MainViewModel.Queue4MemoContents.execute(
                    this@MemoMainFragment, viewModel, CREATE_FIRST_MEMO_ROW,
                    EditText(this@MemoMainFragment.context), Text("")
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
