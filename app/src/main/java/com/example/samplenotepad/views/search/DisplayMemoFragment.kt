package com.example.samplenotepad.views.search

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.samplenotepad.R
import com.example.samplenotepad.data.updateMemoContentsInDatabase
import com.example.samplenotepad.entities.DisplayExistMemo
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.usecases.initMemoContentsOperation
import com.example.samplenotepad.views.main.MEMO_Id
import com.example.samplenotepad.views.main.MainActivity
import kotlinx.android.synthetic.main.fragment_display_memo.*


class DisplayMemoFragment : Fragment() {
    private val searchViewModel = SearchViewModel.getInstanceOrCreateNewOne()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_display_memo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        displayToEditImgBtn.setOnClickListener {
            val memoId = searchViewModel.getMemoInfo().rowid

            when (searchViewModel.compareMemoContentsWithSavePoint()) {
                true -> moveToMainActivityForEditMemo(memoId)
                false -> {
                    val memoContents = searchViewModel.getMemoContents()

                    updateMemoContentsInDatabase(this, memoId, memoContents)
                    moveToMainActivityForEditMemo(memoId)
                }
            }
        }

        displaySaveImgBtn.setOnClickListener {
            val memoId = searchViewModel.getMemoInfo().rowid
            val memoContents = searchViewModel.getMemoContents()

            searchViewModel.updateMemoContentsAtSavePoint()
            updateMemoContentsInDatabase(this, memoId, memoContents)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initMemoContentsOperation(
            this, searchViewModel, displayMemoContentsContainerLayout, DisplayExistMemo
        )
        Log.d("場所:DisplayMemoFragment#initMemoContentsOperation後#fromVM", "memoId=${searchViewModel.getMemoInfo().rowid} memoContents=${searchViewModel.getMemoContents()}")

    }


    private fun moveToMainActivityForEditMemo(memoId: Long) {
        Log.d("場所:DisplayMemoFragment#moveToMainActivityForEditMemo#fromVM", "memoId=$memoId memoContents=${searchViewModel.getMemoContents()}")

        val intent = Intent(this.requireActivity(), MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(MEMO_Id, memoId)
        }

        requireActivity().startActivity(intent)
        requireActivity().finish()
    }
}
