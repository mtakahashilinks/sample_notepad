package com.example.samplenotepad

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import arrow.core.Some


private lateinit var constraintSet: ConstraintSet


internal fun MemoRow.setTextAndCursorPosition(text: Text, selection: Int = 0) {
    this.apply {
        setText(text.value)
        requestFocus()
        setSelection(selection)
        }
}

internal fun ConstraintLayout.setConstraintForFirstMemoRow(targetMemoRow: MemoRow) {
    constraintSet = ConstraintSet()

    this.addView(targetMemoRow)

    constraintSet.apply {
        clone(this@setConstraintForFirstMemoRow)
        connect(targetMemoRow.id, ConstraintSet.TOP, this@setConstraintForFirstMemoRow.id, ConstraintSet.TOP)
        connect(targetMemoRow.id, ConstraintSet.START,
            this@setConstraintForFirstMemoRow.id, ConstraintSet.START)
        connect(targetMemoRow.id, ConstraintSet.END, this@setConstraintForFirstMemoRow.id, ConstraintSet.END)
        applyTo(this@setConstraintForFirstMemoRow)
    }
}

internal fun ConstraintLayout.setConstraintForNextMemoRowWithNoBelow(targetMemoRow: MemoRow,
                                                                     formerMemoRowId: MemoRowId) {
    this.addView(targetMemoRow)

    constraintSet.apply {
        clone(this@setConstraintForNextMemoRowWithNoBelow)
        connect(targetMemoRow.id, ConstraintSet.TOP,
            formerMemoRowId.value, ConstraintSet.BOTTOM)
        connect(targetMemoRow.id, ConstraintSet.START,
            this@setConstraintForNextMemoRowWithNoBelow.id, ConstraintSet.START)
        connect(targetMemoRow.id, ConstraintSet.END,
            this@setConstraintForNextMemoRowWithNoBelow.id, ConstraintSet.END)
        applyTo(this@setConstraintForNextMemoRowWithNoBelow)
    }
}

internal fun ConstraintLayout.setConstraintForNextMemoRowWithBelow(targetMemoRow: MemoRow,
                                                                   formerMemoRowId: MemoRowId,
                                                                   nextMemoRowId: MemoRowId) {
    this.addView(targetMemoRow)

    constraintSet.apply {
        clone(this@setConstraintForNextMemoRowWithBelow)
        clear(nextMemoRowId.value, ConstraintSet.TOP)
        connect(targetMemoRow.id, ConstraintSet.TOP,
            formerMemoRowId.value, ConstraintSet.BOTTOM)
        connect(targetMemoRow.id, ConstraintSet.START,
            this@setConstraintForNextMemoRowWithBelow.id, ConstraintSet.START)
        connect(targetMemoRow.id, ConstraintSet.END,
            this@setConstraintForNextMemoRowWithBelow.id, ConstraintSet.END)
        connect(nextMemoRowId.value, ConstraintSet.TOP,
            targetMemoRow.id, ConstraintSet.BOTTOM)
        applyTo(this@setConstraintForNextMemoRowWithBelow)
    }
}

internal fun ConstraintLayout.setConstraintForDeleteMemoRow(targetMemoRow: MemoRow,
                                                            formerMemoRowId: MemoRowId,
                                                            nextMemoRowId: MemoRowId) {
    constraintSet.apply {
        clone(this@setConstraintForDeleteMemoRow)
        clear(nextMemoRowId.value, ConstraintSet.TOP)
        clear(targetMemoRow.id, ConstraintSet.TOP)
        connect(nextMemoRowId.value, ConstraintSet.TOP, formerMemoRowId.value, ConstraintSet.BOTTOM)
        applyTo(this@setConstraintForDeleteMemoRow)
    }
}

internal fun ConstraintLayout.removeMemoRowFromLayout(fragment: Fragment,
                                                      targetMemoRow: MemoRow,
                                                      formerMemoRow: MemoRow) {
    val inputManager =
        fragment.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputManager.restartInput(formerMemoRow)

    this.removeView(targetMemoRow)
}

internal fun ConstraintLayout.setConstraintForOptView(targetMemoRow: MemoRow, newOptView: View,
                                                      MemoRowMargin: Int, optViewMargin: Int = 0) {
    this.addView(newOptView)

    constraintSet.apply {
        clone(this@setConstraintForOptView)
        clear(targetMemoRow.id, ConstraintSet.START)
        connect(newOptView.id, ConstraintSet.START,
            this@setConstraintForOptView.id, ConstraintSet.START, optViewMargin)
        connect(newOptView.id, ConstraintSet.TOP, targetMemoRow.id, ConstraintSet.TOP)
        setVerticalBias(newOptView.id, 0f)
        connect(newOptView.id, ConstraintSet.BOTTOM, targetMemoRow.id, ConstraintSet.BOTTOM)
        connect(targetMemoRow.id, ConstraintSet.START,
            newOptView.id, ConstraintSet.END, MemoRowMargin)
        applyTo(this@setConstraintForOptView)
    }
}

internal fun ConstraintLayout.setConstraintForDeleteOptView(targetMemoRow: MemoRow) {
    constraintSet.apply {
        clone(this@setConstraintForDeleteOptView)
        clear(targetMemoRow.id, ConstraintSet.START)
        connect(targetMemoRow.id, ConstraintSet.START,
            this@setConstraintForDeleteOptView.id, ConstraintSet.START)
        applyTo(this@setConstraintForDeleteOptView)
    }
}

internal fun ConstraintLayout.removeOptViewFromLayout(fragment: Fragment,
                                                      targetMemoRow: MemoRow,
                                                      optViewId: TypeForMemoRowInfo) {
    when {
        optViewId is CheckBoxId && optViewId.value is Some ->
            this.removeView(fragment.requireActivity().findViewById(optViewId.value.t))
        optViewId is BulletId && optViewId.value is Some ->
            this.removeView(fragment.requireActivity().findViewById(optViewId.value.t))
    }
    targetMemoRow.setTextColor(Color.BLACK)
}
