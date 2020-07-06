package com.example.samplenotepad.views.main

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.ListPopupWindow
import com.example.samplenotepad.*
import com.example.samplenotepad.entities.MemoInfo
import com.example.samplenotepad.entities.ValuesOfOptionSetting
import com.example.samplenotepad.viewModels.MemoEditViewModel
import com.example.samplenotepad.views.DatePickerFragment
import com.example.samplenotepad.views.SampleMemoApplication
import com.example.samplenotepad.views.TimePickerFragment
import kotlinx.android.synthetic.main.fragment_memo_option.*
import java.text.SimpleDateFormat
import java.util.*


class MemoOptionFragment : Fragment() {

    companion object {
        private var instance: MemoOptionFragment? = null

        internal fun getInstanceOrCreateNew(): MemoOptionFragment =
            instance ?: MemoOptionFragment().apply { if (instance == null) instance = this }

        private fun clearOptionFragmentInstanceFlag() {
            instance = null
        }

        internal fun isInstance() = instance != null


        internal fun getOptionValuesForSave(): ValuesOfOptionSetting? {
            return when (MemoOptionFragment.isInstance()) {
                true -> {
                    val optionFragment = MemoOptionFragment.getInstanceOrCreateNew()
                    val reminderSwitch = optionFragment.reminderOnOffSwitchView
                    val title = when (optionFragment.titleBodyTextView.text.isEmpty()) {
                        true -> null
                        false -> optionFragment.titleBodyTextView.text.toString()
                    }
                    val category = when (optionFragment.categoryTextView.text.isEmpty()) {
                        true -> null
                        false -> optionFragment.categoryTextView.text.toString()
                    }
                    val targetDateTime = optionFragment.getTargetDateTimeParams(reminderSwitch)
                    val preAlarm =
                        optionFragment.preAlarmSpinnerView.getPreAndPostAlarmParams(reminderSwitch)
                    val postAlarm =
                        optionFragment.postAlarmSpinnerView.getPreAndPostAlarmParams(reminderSwitch)

                    ValuesOfOptionSetting(title, category, targetDateTime, preAlarm, postAlarm)
                }
                false -> null
            }
        }

        private fun MemoOptionFragment.getTargetDateTimeParams(switchView: Switch): String? =
            when (switchView.isChecked) {
                true -> {
                    val reminderDate = this.reminderDateView.text.toString().replace('/', '-')
                    val reminderTime = this.reminderTimeView.text.toString().replace(" ", "")

                    "$reminderDate $reminderTime"
                }
                false -> null
            }

        private fun Spinner.getPreAndPostAlarmParams(switchView: Switch): Int? =
            when (switchView.isChecked) {
                true -> this.selectedItemPosition
                false -> null
            }
    }


    private lateinit var editViewModel: MemoEditViewModel

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

        editViewModel = MainActivity.editViewModel
        editViewModel.getMemoInfo().initValueOfAllViewWithMemoInfo()

        //カテゴリー選択リストの処理
        categoryDropDownImgBtn.setOnClickListener {
            ListPopupWindow(requireContext()).apply {
                val data = editViewModel.getCategoryList().drop(1).toTypedArray()
                val adapter = ArrayAdapter(requireContext(), R.layout.memo_category_list_row, data)

                setAdapter(adapter)
                anchorView = categoryTextView
                isModal = true
                width = ListPopupWindow.WRAP_CONTENT
                height = 450

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
                    view.hideSoftwareKeyBoard(requireContext())
                    changeStateForReminderSwitch(true, View.VISIBLE)
                }
                false -> changeStateForReminderSwitch(false, View.INVISIBLE)
            }
        }

        //リマインダーの日付にDialogで選択した値をセットする処理
        reminderDateView.setOnClickListener {
            DatePickerFragment { year, month, day ->
                reminderDateView.text = String.format("%04d/%02d/%02d", year, month + 1, day)
            }.show(requireActivity().supportFragmentManager, "date_dialog")
        }

        //リマインダーの時間にDialogで選択した値をセットする処理
        reminderTimeView.setOnClickListener {
            TimePickerFragment { hour, minute ->
                reminderTimeView.text = String.format("%02d : %02d", hour, minute)
            }.show(requireActivity().supportFragmentManager, "time_dialog")
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()

        clearOptionFragmentInstanceFlag()
    }


    private fun MemoInfo?.initValueOfAllViewWithMemoInfo() {
        titleBodyTextView.setCounterText (titleCounterView, 15) //TitleのViewに文字数カウンターをセット
        categoryTextView.setCounterText(categoryCounterView, 15) //CategoryのViewに文字数カウンターをセット
        Log.d("MemoOptionFragment#initValueOfAllViewWithMemoInfo", "MemoInfo=$this")
        this?.setViewsProperties() ?: setCurrentValueInReminderDateTimeView()
    }

    private fun MemoInfo.setViewsProperties() {
        when (this.reminderDateTime.isEmpty()){
            true -> this.apply {
                setTitleTextWithMemoInfo()
                setCategoryTextWithMemoInfo()
                setCurrentValueInReminderDateTimeView()
            }
            false -> {
                this.setReminderSwitchOnForExistMemo(this@setViewsProperties.reminderDateTime.split(" "))
            }
        }
    }

    private fun MemoInfo.setReminderSwitchOnForExistMemo(reminderDateTimeList: List<String>) {
        reminderOnOffSwitchView.isChecked = true
        changeStateForReminderSwitch(true, View.VISIBLE)

        reminderDateTimeList.setReminderTargetDateTimeWithMemoInfo()

        this.apply {
            setTitleTextWithMemoInfo()
            setCategoryTextWithMemoInfo()
            setReminderPreAndPostAlarmWithMemoInfo()
        }
    }

    private fun MemoInfo.setTitleTextWithMemoInfo() {
        when (this.title) {
            SampleMemoApplication.instance.getString(R.string.memo_title_default_value) ->
                titleBodyTextView.setText("")
            else -> titleBodyTextView.setText(this.title)
        }
    }

    private fun MemoInfo.setCategoryTextWithMemoInfo() {
        when (this.category) {
            SampleMemoApplication.instance.getString(R.string.memo_category_default_value) ->
                categoryTextView.setText("")
            else -> categoryTextView.setText(this.category)
        }
    }

    private fun List<String>.setReminderTargetDateTimeWithMemoInfo() {
        reminderDateView.text = this[0].replace('-', '/')
        reminderTimeView.text = this[1].replace(":", " : ")
    }

    private fun MemoInfo.setReminderPreAndPostAlarmWithMemoInfo() {
        preAlarmSpinnerView.setSelection(this.preAlarm)
        postAlarmSpinnerView.setSelection(this.postAlarm)
    }

    internal fun resetValueOfAllView() {
        titleBodyTextView.setText("")
        categoryTextView.setText("")
        setCurrentValueInReminderDateTimeView()
        preAlarmSpinnerView.setSelection(0)
        postAlarmSpinnerView.setSelection(0)
        reminderOnOffSwitchView.apply {
            isChecked = false
            isEnabled = false
        }
    }

    //ReminderのTargetDateTimeを現在時刻にSetする
    private fun setCurrentValueInReminderDateTimeView() {
        val formatterForDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val formatterForTime = SimpleDateFormat("HH : mm", Locale.getDefault())
        val currentDateTime = System.currentTimeMillis()

        this.reminderDateView.text = formatterForDate.format(currentDateTime)
        this.reminderTimeView.text = formatterForTime.format(currentDateTime)
    }

    //Textの文字数カウンターのセット
    private fun EditText.setCounterText(counterView: TextView, maxCharNumber: Int) {
        val formatter = "%d/${maxCharNumber}"

        counterView.text = formatter.format(0)

        this.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                counterView.text = formatter.format((s?.toString() ?: "0").length)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        } )
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
}
