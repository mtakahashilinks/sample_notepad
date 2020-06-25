package com.example.samplenotepad.views.search

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.observe
import com.example.samplenotepad.R
import com.example.samplenotepad.data.getShowMassageForSavedLiveData
import com.example.samplenotepad.data.resetValueOfShowMassageForSavedLiveData
import com.example.samplenotepad.entities.DisplayExistMemo
import com.example.samplenotepad.entities.DisplayFragment
import com.example.samplenotepad.entities.MEMO_Id
import com.example.samplenotepad.usecases.initMemoContentsOperation
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.main.MainActivity
import kotlinx.android.synthetic.main.fragment_display_memo.*
import kotlinx.android.synthetic.main.reminder_states_dialog.view.*


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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_display_memo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchViewModel = MemoSearchActivity.searchViewModel

        getShowMassageForSavedLiveData().observe(viewLifecycleOwner) { fragmentType ->
            if (fragmentType is DisplayFragment) this.showSnackbarForSavedMassageAtDisplayMemo()
        }

        if (searchViewModel.getMemoInfo().reminderDate != null)
            reminderStatesImgBtn.visibility = View.VISIBLE

        reminderStatesImgBtn.setOnClickListener {
            val layoutView = this.requireActivity().layoutInflater.inflate(
                R.layout.reminder_states_dialog, null, false
            )

            layoutView.apply {
                val memoInfo = searchViewModel.getMemoInfo()
                val mTargetReminderTime = memoInfo.reminderTime.toString()
                val targetReminderTime = when (mTargetReminderTime.length == 3) {
                    true -> "0".plus(mTargetReminderTime)
                    false -> mTargetReminderTime
                }
                Log.d("場所:DisplayMemoFragment", "reminderDate=${memoInfo.reminderDate} reminderTime=${memoInfo.reminderTime}")

                targetDateTimeBodyTextView.setTargetDateTimeTextView(
                    memoInfo.reminderDate.toString() , targetReminderTime
                )

                memoInfo.preAlarmTime.let {
                    preAlarmBodyTextView.setText(getPreAlarmTextFromPosition(it))
                }

                memoInfo.postAlarmTime.let {
                    postAlarmBodyTextView.setText(getPostAlarmTextFromPosition(it))
                }
            }

            AlertDialog.Builder(this.requireContext()).apply {
                setTitle(R.string.dialog_show_reminder_states_title)
                setView(layoutView)
            }.show()
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

    override fun onDestroy() {
        super.onDestroy()

        clearDisplayMemoFragmentInstanceFlag()
        resetValueOfShowMassageForSavedLiveData()
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

    private fun getPreAlarmTextFromPosition(position: Int) = when (position) {
        0 -> R.string.alarm_none
        1 -> R.string.pre_alarm_5m
        2 -> R.string.pre_alarm_10m
        3 -> R.string.pre_alarm_30m
        4 -> R.string.pre_alarm_1h
        else -> R.string.pre_alarm_24h
    }

    private fun getPostAlarmTextFromPosition(position: Int) = when (position) {
        0 -> R.string.alarm_none
        1 -> R.string.post_alarm_5m
        2 -> R.string.post_alarm_10m
        3 -> R.string.post_alarm_30m
        4 -> R.string.post_alarm_1h
        else -> R.string.post_alarm_24h
    }

    private fun TextView.setTargetDateTimeTextView(
        targetDate: String,
        targetTime: String
    ) {
        this.text = String.format(
            "%s/%s/%s  %s:%s",
            targetDate.slice(0..3),
            targetDate.slice(4..5),
            targetDate.slice(6..7),
            targetTime.slice(0..1),
            targetTime.slice(2..3)
        )
    }
}
