package com.example.samplenotepad.usecases

import android.util.Log
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import arrow.core.k
import com.example.samplenotepad.R
import com.example.samplenotepad.entities.CheckBoxState
import com.example.samplenotepad.entities.MemoRow
import com.example.samplenotepad.viewModels.SearchViewModel
import com.example.samplenotepad.views.main.setConstraintForBulletsView
import com.example.samplenotepad.views.search.DisplayMemoFragment


private lateinit var fragment: DisplayMemoFragment
private lateinit var viewModel: SearchViewModel
private lateinit var memoContainer: ConstraintLayout

private fun createNewMemoRowViewForDisplay(memoRowId: Int): MemoRow {
    return EditText(fragment.context, null, 0, R.style.MemoEditTextStyle).apply {
        layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        id = memoRowId
    }
}

private fun CheckBox.setCheckedChangeActionForDisplay(targetMemoRow: MemoRow) {
    this.setOnCheckedChangeListener { buttonView, isChecked ->
        when (isChecked) {
            true -> {
                targetMemoRow.setTextColor(resources.getColor(R.color.colorGray, fragment.activity?.theme))

                changeCheckBoxStateForDisplay(targetMemoRow)
            }
            false -> {
                targetMemoRow.setTextColor(resources.getColor(R.color.colorBlack, fragment.activity?.theme))

                changeCheckBoxStateForDisplay(targetMemoRow)
            }
        }
    }
}

private fun changeCheckBoxStateForDisplay(targetMemoRow: MemoRow) {
    viewModel.updateMemoContents { memoContents ->
        val indexOfMemoRow = memoContents.indexOfFirst { it.memoRowId.value == targetMemoRow.id }
        Log.d("場所:changeCheckBoxState", "変更前:size=${memoContents.size} memoContents=${memoContents}")

        memoContents.flatMap {
            if (it.memoRowId.value == targetMemoRow.id)
                listOf(memoContents[indexOfMemoRow].copy(
                    checkBoxState = CheckBoxState(!it.checkBoxState.value)
                )).k()
            else listOf(it).k()
        }
    }
    Log.d("場所:changeCheckBoxStateForDisplay",
        "変更前:size=${viewModel.getMemoContents().size} memoContents=${viewModel.getMemoContents()}")
}

private fun createNewCheckBoxViewForDisplay(targetMemoRow: MemoRow, checkBoxId: Int): CheckBox {
    return CheckBox(fragment.context).apply {
        layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        ViewGroup.MarginLayoutParams(0, 0)
        id = checkBoxId
        textSize = 0f
        setPadding(4)
        setCheckedChangeActionForDisplay(targetMemoRow)
    }
}

private fun addCheckBoxForDisplay(targetMemoRow: MemoRow, targetCheckBoxId: Int, checkBoxState: Boolean) {
    Log.d("場所:addCheckBoxForDisplay", "addCheckBoxForDisplay追加処理に入った")
    val newCheckBox = createNewCheckBoxViewForDisplay(targetMemoRow, targetCheckBoxId)

    memoContainer.setConstraintForBulletsView(targetMemoRow, newCheckBox, 80)

    when (checkBoxState) {
        true -> targetMemoRow.setTextColor(
            fragment.resources.getColor(R.color.colorGray, fragment.activity?.theme)
        )
        false -> targetMemoRow.setTextColor(
            fragment.resources.getColor(R.color.colorBlack, fragment.activity?.theme)
        )
    }
}


private fun addDotForDisplay(targetMemoRow: MemoRow, targetDotId: Int) {
    Log.d("場所:addDotForDisplay", "dot追加処理に入った")
        val newDot = TextView(fragment.context).apply {
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            id = targetDotId
            setPadding(4)
            setBackgroundResource(R.color.colorTransparent)
            text = "・"
        }

        memoContainer.setConstraintForBulletsView(targetMemoRow, newDot, 60, 20)
}

