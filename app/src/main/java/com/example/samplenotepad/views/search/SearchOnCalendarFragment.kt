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
import kotlinx.android.synthetic.main.fragment_search_on_calendar.*
import java.text.SimpleDateFormat
import java.util.*

class SearchOnCalendarFragment : Fragment() {

    companion object {
        private var instance: SearchOnCalendarFragment? = null

        internal fun instanceToAddOnActivity(): SearchOnCalendarFragment {
            val mInstance = instance

            return when (mInstance != null && !mInstance.isAdded) {
                true -> mInstance
                false -> SearchOnCalendarFragment().apply { instance = this }
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
        return inflater.inflate(R.layout.fragment_search_on_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        //アプリバーのタイトルをセット
        activity?.title = getString(R.string.appbar_title_search_with_reminder)

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

                this@SearchOnCalendarFragment.requireActivity().moveToSearchResult(OnCalendar)
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
            EventDay(getCalendarFrom(memoInfo.baseDateTimeForAlarm), R.drawable.ic_calendar_event_8)
        }
    }

    private fun getCalendarFrom(date: String) =
        Calendar.getInstance().apply {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            formatter.parse(date)?.let { time = it }
        }
}