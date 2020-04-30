package com.example.samplenotepad

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.widget.ListPopupWindow
import kotlinx.android.synthetic.main.fragment_memo_option.*


class MemoOptionFragment : Fragment() {

    private lateinit var optionViewModel: MemoOptionViewModel
    private lateinit var mainViewModel: MemoMainViewModel

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

        optionViewModel.apply {
            setCounterText(titleTextView, titleCounterView, 15) //TitleのViewに文字数カウンターをセット
            setCounterText(categoryTextView, categoryCounterView, 15) //CategoryのViewに文字数カウンターをセット
            initReminderDateTime(reminderDateView, reminderTimeView)
        }

        //カテゴリーのTextを初期化
        if (categoryTextView.text.isEmpty()) categoryTextView.setText(optionViewModel.categoryList[0])

        //カテゴリー選択リストの処理
        categoryDropDownImgBtn.setOnClickListener {
            ListPopupWindow(requireContext()).apply {
                val data = arrayOf("その他", "仕事", "プライベート")
                val adapter = ArrayAdapter(requireContext(), R.layout.memo_category_list_row, data)

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
                true -> {
                    reminderDateLabelView.isEnabled = true
                    reminderDateView.isEnabled = true
                    reminderTimeLabelView.isEnabled = true
                    reminderTimeView.isEnabled = true
                    preAlarmLabelView.isEnabled = true
                    preAlarmSpinnerView.visibility = View.VISIBLE
                    postAlarmLabelView.isEnabled = true
                    postAlarmSpinnerView.visibility = View.VISIBLE
                }
                false -> {
                    reminderDateLabelView.isEnabled = false
                    reminderDateView.isEnabled = false
                    reminderTimeLabelView.isEnabled = false
                    reminderTimeView.isEnabled = false
                    preAlarmLabelView.isEnabled = false
                    preAlarmSpinnerView.visibility = View.INVISIBLE
                    postAlarmLabelView.isEnabled = false
                    postAlarmSpinnerView.visibility = View.INVISIBLE
                }
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


    internal fun setViewModel(mainVM: MemoMainViewModel, optionVM: MemoOptionViewModel) {
        mainViewModel = mainVM
        optionViewModel = optionVM
    }
}
