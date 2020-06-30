package com.example.samplenotepad.views.search

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.samplenotepad.R
import com.example.samplenotepad.data.getShowMassageForSavedFlow
import com.example.samplenotepad.data.resetValueOfShowMassageForSavedFlow
import com.example.samplenotepad.entities.DisplayExistMemo
import com.example.samplenotepad.entities.DisplayFragment
import com.example.samplenotepad.entities.MEMO_Id
import com.example.samplenotepad.usecases.initMemoContentsOperation
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.main.MainActivity
import kotlinx.android.synthetic.main.fragment_display_memo.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class DisplayMemoFragment : Fragment() {

    companion object {
        private var instance: DisplayMemoFragment? = null

        internal fun getInstanceOrCreateNew(): DisplayMemoFragment =
            instance ?: DisplayMemoFragment().apply { if (instance == null) instance = this }

        internal fun clearDisplayMemoFragmentInstanceFlag() {
            instance = null
        }
    }


    private lateinit var searchViewModel: SearchViewModel
    private var isShowingPopupWindow = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_display_memo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchViewModel = MemoSearchActivity.searchViewModel

        CoroutineScope(Dispatchers.Main).launch {
            getShowMassageForSavedFlow().collect { fragmentType ->
                if (fragmentType is DisplayFragment)
                    this@DisplayMemoFragment.showSnackbarForSavedMassageAtDisplayMemo()

                resetValueOfShowMassageForSavedFlow()
            }
        }

        if (searchViewModel.getMemoInfo().reminderDate != null)
            reminderStatesImgBtn.visibility = View.VISIBLE

        //reminderの設定表示用のPopupWindowの設定
        reminderStatesImgBtn.setOnClickListener {
            val popupWindow = getReminderStatesPopupWindow()

            isShowingPopupWindow = when (isShowingPopupWindow) {
                true -> {
                    popupWindow.dismissReminderStatesPopupWindow(this)
                    false
                }
                false -> {
                    popupWindow.showAsDropDown(reminderStatesImgBtn)
                    true
                }
            }
        }

        displayToEditImgBtn.setOnClickListener {
            when (searchViewModel.compareMemoContentsWithSavePoint()) {
                true -> moveToMainActivityForEditMemo()
                false -> {
                    searchViewModel.updateMemoContentsInDatabaseAndSavePoint(DisplayExistMemo)
                    moveToMainActivityForEditMemo()
                }
            }
        }

        displaySaveImgBtn.setOnClickListener {
            searchViewModel.updateMemoContentsInDatabaseAndSavePoint(DisplayExistMemo)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initMemoContentsOperation(
            this, searchViewModel, displayMemoContentsContainerLayout, DisplayExistMemo
        )
        Log.d("場所:DisplayMemoFragment#initMemoContentsOperation後#fromVM", "memoId=${searchViewModel.getMemoInfo().rowid} memoContents=${searchViewModel.getMemoContents()}")

    }

    override fun onResume() {
        super.onResume()

        //アプリバーのタイトルをセット
        activity?.title = getString(R.string.appbar_title_display_memo)
    }

    override fun onDestroy() {
        super.onDestroy()

        clearDisplayMemoFragmentInstanceFlag()
    }


    private fun moveToMainActivityForEditMemo() {
        Log.d("場所:DisplayMemoFragment#moveToMainActivityForEditMemo#fromVM", "memoId=${searchViewModel.getMemoInfo().rowid} memoContents=${searchViewModel.getMemoContents()}")

        val intent = Intent(this.requireActivity(), MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(MEMO_Id, searchViewModel.getMemoInfo().rowid)
        }

        viewModelStore.clear()

        requireActivity().apply {
            startActivity(intent)
            finish()
        }
    }

    internal fun setIsShowingPopupWindow(value: Boolean) {
        isShowingPopupWindow = value
    }
}
