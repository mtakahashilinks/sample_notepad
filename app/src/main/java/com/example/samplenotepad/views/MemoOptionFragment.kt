package com.example.samplenotepad.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.widget.ListPopupWindow
import com.example.samplenotepad.*
import com.example.samplenotepad.data.AppDatabase
import com.example.samplenotepad.viewModels.MemoInputViewModel
import com.example.samplenotepad.viewModels.MemoOptionViewModel
import kotlinx.android.synthetic.main.fragment_memo_option.*


class MemoOptionFragment : Fragment() {

    companion object {
        private lateinit var optionViewModel: MemoOptionViewModel
        private lateinit var inputViewModel: MemoInputViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_memo_option, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        optionViewModel.initOptionViewModel(this)

        optionViewModel.apply {
            setCounterText(titleTextView, titleCounterView, 15) //TitleのViewに文字数カウンターをセット
            setCounterText(categoryTextView, categoryCounterView, 15) //CategoryのViewに文字数カウンターをセット
            initReminderDateTime(reminderDateView, reminderTimeView)
        }

        //カテゴリーのTextを初期化
        if (categoryTextView.text.isEmpty()) categoryTextView.setText("その他")

        //カテゴリー選択リストの処理
        categoryDropDownImgBtn.setOnClickListener {
            ListPopupWindow(requireContext()).apply {
                val data = inputViewModel.categoryList.toTypedArray()
                val adapter = ArrayAdapter(requireContext(),
                    R.layout.memo_category_list_row, data)

                anchorView = categoryTextView
                setAdapter(adapter)
                isModal = true
                width = ListPopupWindow.WRAP_CONTENT
                height = ListPopupWindow.WRAP_CONTENT

                setOnItemClickListener { parent, view, position, id ->
                    val selectedItem = adapter.getItem(position)
                    categoryTextView.setText(selectedItem)
                    dismiss()
                }

                show()
            }
        }


        //リマインダー登録スイッチのON・Off切り替えによる処理
        reminderOnOffSwitchView.setOnCheckedChangeListener { buttonView, isChecked ->
            when (isChecked) {
                true -> changeStateForReminderSwitch(true, View.VISIBLE)
                false -> changeStateForReminderSwitch(false, View.INVISIBLE)
            }
        }

        //リマインダーの日付のセット処理
        reminderDateView.setOnClickListener {
            DatePickerFragment { year, month, day ->
                reminderDateView.text = String.format("%04d/%02d/%02d", year, month + 1, day)
            }.show(requireActivity().supportFragmentManager, "date_dialog")
        }

        //リマインダーの時間のセット処理
        reminderTimeView.setOnClickListener {
            TimePickerFragment { hour, minute ->
                reminderTimeView.text = String.format("%02d : %02d", hour, minute)
            }.show(requireActivity().supportFragmentManager, "time_dialog")
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        //ツールバーのタイトルをセット
        activity?.title = getString(R.string.optional_setting)
    }


    private fun changeStateForReminderSwitch(enable: Boolean, visibility: Int) {
        reminderDateLabelView.isEnabled = enable
        reminderDateView.isEnabled = enable
        reminderTimeLabelView.isEnabled = enable
        reminderTimeView.isEnabled = enable
        preAlarmLabelView.isEnabled = enable
        preAlarmSpinnerView.visibility = visibility
        postAlarmLabelView.isEnabled = enable
        postAlarmSpinnerView.visibility = visibility
    }

    internal fun setValues(inputVM: MemoInputViewModel, optionVM: MemoOptionViewModel) {
        inputViewModel = inputVM
        optionViewModel = optionVM
    }
}
