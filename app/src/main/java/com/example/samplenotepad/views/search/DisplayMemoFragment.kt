package com.example.samplenotepad.views.search

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.samplenotepad.R
import com.example.samplenotepad.entities.DisplayExistMemo
import com.example.samplenotepad.entities.DisplayFragment
import com.example.samplenotepad.entities.MEMO_Id
import com.example.samplenotepad.usecases.getShowMassageForSavedLiveData
import com.example.samplenotepad.usecases.initMemoContentsOperation
import com.example.samplenotepad.usecases.resetValueOfShowMassageForSavedLiveData
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.main.MainActivity
import kotlinx.android.synthetic.main.fragment_display_memo.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

        lifecycleScope.launch {
            getShowMassageForSavedLiveData().observe(viewLifecycleOwner, Observer { typeOfFragment ->
                if (typeOfFragment is DisplayFragment)
                    this@DisplayMemoFragment.showSnackbarForSavedMassageAtDisplayMemo()

                resetValueOfShowMassageForSavedLiveData()
            })
        }

        if (searchViewModel.getMemoInfo().reminderDateTime.isNotEmpty())
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
            when (searchViewModel.isSavedAlready()) {
                true -> moveToMainActivityForEditMemo()
                false -> {
                    searchViewModel.updateMemoInfoDatabase()
                    moveToMainActivityForEditMemo()
                }
            }
        }

        displaySaveImgBtn.setOnClickListener {
            searchViewModel.updateMemoInfoDatabase()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initMemoContentsOperation(
            this, searchViewModel, displayMemoContentsContainerLayout, DisplayExistMemo
        )
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
