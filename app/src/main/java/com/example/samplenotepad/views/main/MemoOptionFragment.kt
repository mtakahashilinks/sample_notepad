package com.example.samplenotepad.views.main

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.ListPopupWindow
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.example.samplenotepad.*
import com.example.samplenotepad.entities.MemoInfo
import com.example.samplenotepad.entities.ValuesOfOptionSetting
import com.example.samplenotepad.viewModels.MemoEditViewModel
import com.example.samplenotepad.views.DatePickerFragment
import com.example.samplenotepad.views.SampleMemoApplication
import com.example.samplenotepad.views.TimePickerFragment
import kotlinx.android.synthetic.main.fragment_memo_option.*
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


        internal fun getOptionValuesForSave(): ValuesOfOptionSetting {
            return when (MemoOptionFragment.isInstance()) {
                true -> {
                    val optionFragment = MemoOptionFragment.getInstanceOrCreateNew()
                    val reminderSwitch = optionFragment.reminderOnOffSwitchView
                    val title = when (optionFragment.titleBodyTextView.text.isEmpty()) {
                        true -> None
                        false -> Some(optionFragment.titleBodyTextView.text.toString())
                    }
                    val category = when (optionFragment.categoryTextView.text.isEmpty()) {
                        true -> None
                        false -> Some(optionFragment.categoryTextView.text.toString())
                    }
                    val targetDate = optionFragment.reminderDateView.getTargetDateTimeParams(reminderSwitch)
                    val targetTime = optionFragment.reminderTimeView.getTargetDateTimeParams(reminderSwitch)
                    val preAlarm =
                        optionFragment.preAlarmSpinnerView.getPreAndPostAlarmParams(reminderSwitch)
                    val postAlarm =
                        optionFragment.postAlarmSpinnerView.getPreAndPostAlarmParams(reminderSwitch)

                    ValuesOfOptionSetting(title, category, targetDate, targetTime, preAlarm, postAlarm)
                }
                false -> ValuesOfOptionSetting(None, None, None, None, None, None)
            }
        }

        //例）"2020/03/05" -> 20200305 , "01:03" -> 103
        private fun String.convertTargetDateTime(): Some<Int> {
            val matchedResults = Regex("""\d+""").findAll(this)
            val result = matchedResults.map { it.value }.joinToString("")

            return Some(result.toInt())
        }

        private fun Button.getTargetDateTimeParams(switchView: Switch): Option<Int> =
            when (switchView.isChecked) {
                true -> this.text.toString().convertTargetDateTime()
                false -> None
            }

        private fun Spinner.getPreAndPostAlarmParams(switchView: Switch): Option<Int> =
            when (switchView.isChecked) {
                true -> Some(this.selectedItemPosition)
                false -> None
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
        when (this?.reminderDate != null) {
            true -> {
                titleBodyTextView.setCounterText (titleCounterView, 15) //TitleのViewに文字数カウンターをセット
                categoryTextView.setCounterText(categoryCounterView, 15) //CategoryのViewに文字数カウンターをセット

                this.setReminderSwitchOnForExistMemo()
            }
            false -> {
                titleBodyTextView.setCounterText (titleCounterView, 15) //TitleのViewに文字数カウンターをセット
                categoryTextView.setCounterText(categoryCounterView, 15) //CategoryのViewに文字数カウンターをセット

                setCurrentValueInReminderDateTimeView()

                this.apply {
                    setTitleTextWithMemoInfo()
                    setCategoryTextWithMemoInfo()
                }
            }
        }
    }

    private fun MemoInfo?.setReminderSwitchOnForExistMemo() {
        reminderOnOffSwitchView.isChecked = true
        changeStateForReminderSwitch(true, View.VISIBLE)

        this. apply {
            setTitleTextWithMemoInfo()
            setCategoryTextWithMemoInfo()
            setReminderTargetDateTimeWithMemoInfo()
            setReminderPreAndPostAlarmWithMemoInfo()
        }
    }

    private fun MemoInfo?.setTitleTextWithMemoInfo() {
        when (this?.title) {
            SampleMemoApplication.instance.getString(R.string.memo_title_default_value) ->
                titleBodyTextView.setText("")
            else -> titleBodyTextView.setText(this?.title)
        }
    }

    private fun MemoInfo?.setCategoryTextWithMemoInfo() {
        when (this?.category) {
            SampleMemoApplication.instance.getString(R.string.memo_category_default_value) ->
                categoryTextView.setText("")
            else -> categoryTextView.setText(this?.category)
        }
    }

    private fun MemoInfo?.setReminderTargetDateTimeWithMemoInfo() {
        val targetDate = this?.reminderDate.toString()
        val mTargetTime = this?.reminderTime.toString()
        val targetTime = when (mTargetTime.length == 3) {
            true -> "0".plus(mTargetTime)
            false -> mTargetTime
        }

        reminderDateView.text =
            String.format("%s/%s/%s", targetDate.slice(0..3), targetDate.slice(4..5), targetDate.slice(6..7))

        reminderTimeView.text = String.format("%s : %s" , targetTime.slice(0..1), targetTime.slice(2..3))
    }

    private fun MemoInfo?.setReminderPreAndPostAlarmWithMemoInfo() {
        this?.let { preAlarmSpinnerView.setSelection(it.preAlarmTime) }
        this?.let { postAlarmSpinnerView.setSelection(it.postAlarmTime) }
    }

    internal fun resetValueOfAllView() {
        titleBodyTextView.setText("")
        categoryTextView.setText("")
        reminderOnOffSwitchView.isEnabled = false
        setCurrentValueInReminderDateTimeView()
        preAlarmSpinnerView.setSelection(0)
        postAlarmSpinnerView.setSelection(0)
    }

    //ReminderのTargetDateTimeを現在時刻にSetする
    private fun setCurrentValueInReminderDateTimeView() {
        val calendar = Calendar.getInstance()

        this.reminderDateView.text =
            android.text.format.DateFormat.format("yyyy/MM/dd", calendar).toString()
        this.reminderTimeView.text =
            android.text.format.DateFormat.format("HH : mm", calendar.time).toString()
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
