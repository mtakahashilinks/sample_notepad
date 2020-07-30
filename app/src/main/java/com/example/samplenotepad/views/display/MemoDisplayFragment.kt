package com.example.samplenotepad.views.display

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.samplenotepad.R
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.usecases.getMemoContentsOperationActor
import com.example.samplenotepad.usecases.getShowMassageForSavedLiveData
import com.example.samplenotepad.usecases.initMemoContentsOperation
import com.example.samplenotepad.usecases.resetValueOfShowMassageForSavedLiveData
import com.example.samplenotepad.viewModels.MemoDisplayViewModel
import com.example.samplenotepad.views.moveToMainActivityForEditExistMemo
import kotlinx.android.synthetic.main.fragment_display_memo.*
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json


class MemoDisplayFragment : Fragment() {

    companion object {
        private var instance: MemoDisplayFragment? = null

        internal fun getInstanceOrCreateNew(): MemoDisplayFragment =
            instance ?: MemoDisplayFragment().apply { if (instance == null) instance = this }

        internal fun clearDisplayMemoFragmentInstanceFlag() {
            instance = null
        }
    }


    private lateinit var displayViewModel: MemoDisplayViewModel
    private var isShowingPopupWindow = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_display_memo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        displayViewModel = MemoDisplayActivity.displayViewModel

        //メモの保存時にSnackbarを表示
        lifecycleScope.launch {
            getShowMassageForSavedLiveData().observe(viewLifecycleOwner, Observer { typeOfFragment ->
                if (typeOfFragment is DisplayFragment
                    && reminderStatesImgBtn.visibility != View.INVISIBLE) {
                    this@MemoDisplayFragment.showSnackbarForSavedMassageAtDisplayMemo()
                }

                resetValueOfShowMassageForSavedLiveData()
            })
        }

        val memoInfo = displayViewModel.getMemoInfo()
        val memoContents = Json.parse(MemoRowInfo.serializer().list, memoInfo.contents)

        //
        displayViewModel.apply {
            viewModelScope.launch {
                getMemoContentsOperationActor().send(SetMemoContents(memoContents))
                updateSavePointOfMemoContents()
            }
        }


        //リマインダーがあればボタンを表示
        if (displayViewModel.getMemoInfo().reminderDateTime.isNotEmpty())
            reminderStatesImgBtn.visibility = View.VISIBLE

        //reminderの設定表示用のPopupWindowの設定
        reminderStatesImgBtn.setOnClickListener {
            val popupWindow = getReminderStatesPopupWindow()

            isShowingPopupWindow = when (isShowingPopupWindow) {
                true -> {
                    popupWindow.dismissReminderStatesPopupWindow()
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
                true -> requireActivity().moveToMainActivityForEditExistMemo(
                    displayViewModel.getMemoInfo().rowid
                )
                false -> {
                    displayViewModel.saveMemoInfo()
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


    internal fun setIsShowingPopupWindow(value: Boolean) {
        isShowingPopupWindow = value
    }
}
