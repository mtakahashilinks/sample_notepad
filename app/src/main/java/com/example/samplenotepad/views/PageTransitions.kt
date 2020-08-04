package com.example.samplenotepad.views

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.example.samplenotepad.R
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.views.display.MemoDisplayActivity
import com.example.samplenotepad.views.main.MainActivity
import com.example.samplenotepad.views.search.*


internal fun FragmentActivity.moveToMainActivity() {
    val intent = Intent(this, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    startActivity(intent)
    finish()
}

internal fun FragmentActivity.moveToMainActivityForEditExistMemo(memoInfoId: Long) {
    val intent = Intent(this, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(ConstValForMemo.MEMO_Id, memoInfoId)
    }

    startActivity(intent)
    finish()
}

internal fun FragmentActivity.moveToSearchActivity(searchId: String) {
    val intent = Intent(this, MemoSearchActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(ConstValForSearch.SEARCH_ID, searchId)
    }

    startActivity(intent)
    finish()
}

internal fun FragmentActivity.moveToDisplayActivity(memoInfoId: Long) {
    val intent = Intent(this, MemoDisplayActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(ConstValForMemo.MEMO_Id, memoInfoId)
    }

    startActivity(intent)
}


internal fun FragmentActivity.moveToSearchTopAndCancelAllStacks() {
    supportFragmentManager.apply {
        popBackStack(ConstValForSearch.SEARCH_TOP, FragmentManager.POP_BACK_STACK_INCLUSIVE)

        beginTransaction()
            .replace(
                R.id.searchContainer,
                SearchTopFragment.instanceToAddOnActivity(),
                ConstValForSearch.SEARCH_TOP
            )
            .commit()
    }
}

internal fun FragmentActivity.moveToSearchInCategory() {
    supportFragmentManager.beginTransaction()
        .addToBackStack(null)
        .replace(R.id.searchContainer, SearchInCategoryFragment.instanceToAddOnActivity())
        .commit()
}

internal fun FragmentActivity.moveToReminderList() {
    supportFragmentManager.beginTransaction()
        .addToBackStack(ConstValForSearch.REMINDER_LIST)
        .replace(
            R.id.searchContainer,
            SearchWithReminderFragment.instanceToAddOnActivity(),
            ConstValForSearch.REMINDER_LIST
        )
        .commit()
}

internal fun FragmentActivity.moveToSearchByCalendar() {
    supportFragmentManager.beginTransaction()
        .addToBackStack(ConstValForSearch.SEARCH_BY_CALENDAR)
        .replace(
            R.id.searchContainer,
            SearchByCalendarFragment.instanceToAddOnActivity(),
            ConstValForSearch.SEARCH_BY_CALENDAR
        )
        .commit()
}

internal fun FragmentActivity.moveToSearchResult(typeOfSearch: TypeOfSearch) {
    supportFragmentManager.beginTransaction()
        .replace(
            R.id.searchContainer,
            SearchResultFragment.instanceToAddOnActivity(typeOfSearch)
        )
        .addToBackStack(null)
        .commit()
}
