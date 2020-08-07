package com.example.samplenotepad.views.search

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.applandeo.materialcalendarview.EventDay
import com.example.samplenotepad.R
import com.example.samplenotepad.entities.OnCalendar
import com.example.samplenotepad.views.moveToSearchResult
import kotlinx.android.synthetic.main.fragment_search_by_calendar.*
import java.text.SimpleDateFormat
import java.util.*

class SearchByCalendarFragment : Fragment() {

    companion object {
        private var instance: SearchByCalendarFragment? = null

        internal fun instanceToAddOnActivity(): SearchByCalendarFragment {
            val mInstance = instance

            return when (mInstance != null && !mInstance.isAdded) {
                true -> mInstance
                false -> SearchByCalendarFragment().apply { instance = this }
            }
        }

        private fun resetFlagsInFragment() {
            instance = null
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search_by_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        calendarView.apply {
            setDate(Date())
            setEvents(createEventDayList())
            setOnDayClickListener { eventDay ->
                val selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(eventDay.calendar.time)

                MemoSearchActivity.searchViewModel.apply {
                    setSelectedDateOnCalendar(selectedDate)
                    setSearchWord("")
                }

                this@SearchByCalendarFragment.requireActivity().moveToSearchResult(OnCalendar)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        resetFlagsInFragment()
    }

    private fun createEventDayList(): List<EventDay> {
        val memoList =
            MemoSearchActivity.searchViewModel.loadAndSetDataSetForMemoListFindByWithReminder()

        return memoList.map { memoInfo ->
            EventDay(getCalendarFrom(memoInfo.baseDateTimeForAlarm), R.drawable.ic_alarm_black_24dp)
        }
    }

    private fun getCalendarFrom(date: String) =
        Calendar.getInstance().apply {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            formatter.parse(date)?.let { time = it }
        }
}