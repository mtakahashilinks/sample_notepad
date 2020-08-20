package com.example.samplenotepad.views.display

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.samplenotepad.R
import com.example.samplenotepad.data.deserializeToMemoContents
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.usecases.getMemoContentsOperationActor
import com.example.samplenotepad.usecases.getShowMassageForSavedLiveData
import com.example.samplenotepad.usecases.initMemoContentsOperation
import com.example.samplenotepad.usecases.initValueOfShowMassageForSavedLiveData
import com.example.samplenotepad.viewModels.DisplayViewModel
import com.example.samplenotepad.views.moveToMainActivityForEditExistMemo
import com.example.samplenotepad.views.search.SearchActivity
import kotlinx.android.synthetic.main.fragment_display.*
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch


class DisplayFragment : Fragment() {

    companion object {
        private var instance: DisplayFragment? = null

        internal fun instanceToAddOnActivity(): DisplayFragment {
            val mInstance = instance

            return when (mInstance != null && !mInstance.isAdded) {
                true -> mInstance
                false -> DisplayFragment()
                    .apply { instance = this }
            }
        }

        internal fun clearDisplayMemoFragmentInstanceFlag() {
            instance = null
        }
    }


    private lateinit var displayViewModel: DisplayViewModel
    private var isShowingPopupWindow = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_display, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        displayViewModel = MemoDisplayActivity.displayViewModel

        //メモの保存時にSnackbarを表示
        lifecycleScope.launch {
            getShowMassageForSavedLiveData().observe(viewLifecycleOwner, { typeOfFragment ->
                if (typeOfFragment is com.example.samplenotepad.entities.DisplayFragment
                    && reminderStatesImgBtn.visibility != View.INVISIBLE) {
                    this@DisplayFragment.showSnackbarForSavedMassageAtDisplayMemo()
                }

                initValueOfShowMassageForSavedLiveData()
            })
        }

        val memoInfo = displayViewModel.getMemoInfo()
        val memoContents = memoInfo.contents.deserializeToMemoContents()

        //
        displayViewModel.apply {
            viewModelScope.launch {
                getMemoContentsOperationActor().send(SetMemoContents(memoContents))
                updateSavePointOfMemoContents()
            }
        }


        //リマインダーがあればボタンを表示
        if (displayViewModel.getMemoInfo().baseDateTimeForAlarm.isNotEmpty())
            reminderStatesImgBtn.visibility = View.VISIBLE

        //reminderの設定表示用のPopupWindowの設定
        reminderStatesImgBtn.setOnClickListener {
            val popupWindow = getReminderStatesPopupWindow(this)

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
            when (displayViewModel.isSavedMemoContents()) {
                true -> {
                    finishSearchActivityIfInstanced()
                    requireActivity().moveToMainActivityForEditExistMemo(
                        displayViewModel.getMemoInfo().rowid
                    )
                }
                false -> {
                    displayViewModel.saveMemoInfo()
                    finishSearchActivityIfInstanced()
                    requireActivity().moveToMainActivityForEditExistMemo(
                        displayViewModel.getMemoInfo().rowid
                    )
                }
            }
        }

        displaySaveImgBtn.setOnClickListener {
            displayViewModel.saveMemoInfo()
        }
    }

    @ObsoleteCoroutinesApi
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initMemoContentsOperation(
            this, displayViewModel, displayMemoContentsContainerLayout, DisplayExistMemo
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


    private fun finishSearchActivityIfInstanced() {
        if (SearchActivity.isInstance()) SearchActivity.instance.finish()
    }
    internal fun setIsShowingPopupWindow(value: Boolean) {
        isShowingPopupWindow = value
    }
}
