package com.example.samplenotepad.views.search

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.samplenotepad.R
import kotlinx.android.synthetic.main.fragment_search_by_calendar.*
import java.sql.Timestamp
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

        calendarView.setDate(Date())
    }

    override fun onDestroy() {
        super.onDestroy()

        resetFlagsInFragment()
    }
}