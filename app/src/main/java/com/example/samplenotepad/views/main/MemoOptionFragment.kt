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
import androidx.appcompat.widget.SwitchCompat
import com.example.samplenotepad.*
import com.example.samplenotepad.entities.MemoInfo
import com.example.samplenotepad.entities.ValuesOfOptionSetting
import com.example.samplenotepad.viewModels.MainViewModel
import com.example.samplenotepad.views.DatePickerFragment
import com.example.samplenotepad.views.SampleMemoApplication
import com.example.samplenotepad.views.TimePickerFragment
import kotlinx.android.synthetic.main.fragment_memo_option.*
import java.text.SimpleDateFormat
import java.util.*


class MemoOptionFragment : Fragment() {

    companion object {
        private var instance: MemoOptionFragment? = null

        internal fun instanceToAddOnActivity(): MemoOptionFragment {
            val mInstance = instance

            return when (mInstance != null && !mInstance.isAdded) {
                true -> mInstance
                false -> MemoOptionFragment().apply { instance = this }
            }
        }

        internal fun instance(): MemoOptionFragment? = instance

        private fun clearOptionFragmentInstanceFlag() {
            instance = null
        }


        internal fun getOptionValuesForSave(): ValuesOfOptionSetting? =
            instance()?.let { optionFragment ->
                val reminderSwitch = optionFragment.reminderOnOffSwitchView
                val title = when (optionFragment.titleBodyTextView.text.isEmpty()) {
                    true -> null
                    false -> optionFragment.titleBodyTextView.text.toString()
                }
                val category = when (optionFragment.categoryTextView.text.isEmpty()) {
                    true -> null
                    false -> optionFragment.categoryTextView.text.toString()
                }
                val reminderDateTime = optionFragment.getReminderDateTimeParams(reminderSwitch)
                val preAlarm =
                    optionFragment.preAlarmSpinnerView.getPreAndPostAlarmParams(reminderSwitch)
                val postAlarm =
                    optionFragment.postAlarmSpinnerView.getPreAndPostAlarmParams(reminderSwitch)

                ValuesOfOptionSetting(
                    title, category, reminderDateTime, reminderDateTime, preAlarm, postAlarm
                )
            }

        private fun MemoOptionFragment.getReminderDateTimeParams(
            switchView: SwitchCompat
        ): String? = when (switchView.isChecked) {
            true -> {
                val reminderDate = this.reminderDateView.text.toString().replace('/', '-')
                val reminderTime = this.reminderTimeView.text.toString().replace(" ", "")

                "$reminderDate $reminderTime"
            }
            false -> null
        }

        private fun Spinner.getPreAndPostAlarmParams(switchView: SwitchCompat): Int? =
            when (switchView.isChecked) {
                true -> this.selectedItemPosition
                false -> null
            }
    }


    private lateinit var mainViewModel: MainViewModel

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

        mainViewModel = MainActivity.mainViewModel
        mainViewModel.getMemoInfo().initValueOfAllViewWithMemoInfo()

        //カテゴリー選択リストの処理
        mainViewModel.getCategoryList().drop(1).toTypedArray().setCategoryImageButton()

        //リマインダー登録スイッチのON・Off切り替えによる処理
        reminderOnOffSwitchView.setOnCheckedChangeListener { buttonView, isChecked ->
            when (isChecked) {
                true -> {
                    view.hideSoftwareKeyBoard(requireContext())
                    changeStateForReminderSwitch(true, View.VISIBLE)
 //                   preAlarmSpinnerView.setListenerOnAlarmSpinner()
 //                   postAlarmSpinnerView.setListenerOnAlarmSpinner()
                }
                false -> changeStateForReminderSwitch(false, View.INVISIBLE)
            }

            mainViewModel.apply { valueChanged() }
        }

        //リマインダーの日付にDialogで選択した値をセットする処理
        reminderDateView.setOnClickListener {
            DatePickerFragment { year, month, day ->
                reminderDateView.text = String.format("%04d/%02d/%02d", year, month + 1, day)
                mainViewModel.apply { valueChanged() }
            }.show(requireActivity().supportFragmentManager, "date_dialog")
        }

        //リマインダーの時間にDialogで選択した値をセットする処理
        reminderTimeView.setOnClickListener {
            TimePickerFragment { hour, minute ->
                reminderTimeView.text = String.format("%02d : %02d", hour, minute)
                mainViewModel.apply { valueChanged() }
            }.show(requireActivity().supportFragmentManager, "time_dialog")
        }

        preAlarmSpinnerView.setListenerOnAlarmSpinner()
        postAlarmSpinnerView.setListenerOnAlarmSpinner()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()

        clearOptionFragmentInstanceFlag()
    }


    //spinnerのItemがクリックされたらisChangedValueInOptionFragmentをtrueに変更
    private fun Spinner.setListenerOnAlarmSpinner() {
        this.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) { }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                mainViewModel.apply { valueChanged() }
            }
        }
    }

    private fun MemoInfo?.initValueOfAllViewWithMemoInfo() {
        titleBodyTextView.setCounterText (titleCounterView, 15) //TitleのViewに文字数カウンターをセット
        categoryTextView.setCounterText(categoryCounterView, 15) //CategoryのViewに文字数カウンターをセット
        Log.d("MemoOptionFragment#initValueOfAllViewWithMemoInfo", "MemoInfo=$this")
        this?.setViewsProperties() ?: setCurrentValueInReminderDateTimeView()
    }

    private fun MemoInfo.setViewsProperties() {
        when (this.baseDateTimeForAlarm.isNotEmpty()){
            true -> this.setReminderSwitchOnForExistMemo(
                this@setViewsProperties.baseDateTimeForAlarm.split(" ")
            )
            false -> this.apply {
                setTitleTextWithMemoInfo()
                setCategoryTextWithMemoInfo()
                setCurrentValueInReminderDateTimeView()
            }
        }
    }

    private fun MemoInfo.setReminderSwitchOnForExistMemo(reminderDateTimeList: List<String>) {
        reminderOnOffSwitchView.isChecked = true
        changeStateForReminderSwitch(true, View.VISIBLE)
        //preAlarmSpinnerView.setListenerOnAlarmSpinner()
        //postAlarmSpinnerView.setListenerOnAlarmSpinner()

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

    private fun Array<String>.setCategoryImageButton() {
        when (this.isEmpty()) {
            true -> categoryDropDownImgBtn.visibility = View.GONE
            false -> {
                categoryDropDownImgBtn.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { this@setCategoryImageButton.showCategoryPopUpList() }
                }
            }
        }
    }

    private fun Array<String>.showCategoryPopUpList() {
        ListPopupWindow(requireContext()).apply {
            val adapter = ArrayAdapter(
                requireContext(),
                R.layout.memo_option_category_list_row,
                this@showCategoryPopUpList
            )

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

    private fun List<String>.setReminderTargetDateTimeWithMemoInfo() {
        reminderDateView.text = this[0].replace('-', '/')
        reminderTimeView.text = this[1].replace(":", " : ")
    }

    private fun MemoInfo.setReminderPreAndPostAlarmWithMemoInfo() {
        preAlarmSpinnerView.setSelection(this.preAlarmPosition)
        postAlarmSpinnerView.setSelection(this.postAlarmPosition)
    }


    internal fun resetValueOfAllView(viewModel: MainViewModel) {
        titleBodyTextView.setText("")
        categoryTextView.setText("")
        viewModel.getCategoryList().drop(1).toTypedArray().setCategoryImageButton()
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
                mainViewModel.apply { valueChanged() }
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
