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

internal fun MemoRow.setConstraintForFirstMemoRow(container: ConstraintLayout) {
    constraintSet = ConstraintSet()

    container.addView(this)
    constraintSet.apply {
        clone(container)
        connect(this@setConstraintForFirstMemoRow.id, ConstraintSet.TOP, container.id, ConstraintSet.TOP)
        connect(this@setConstraintForFirstMemoRow.id, ConstraintSet.START, container.id, ConstraintSet.START)
        connect(this@setConstraintForFirstMemoRow.id, ConstraintSet.END, container.id, ConstraintSet.END)
        applyTo(container)
    }
}

internal fun MemoRow.setConstraintForNextMemoRowWithNoBelow(container: ConstraintLayout,
                                                             formerMemoRowId: MemoRowId) {
    container.addView(this)
    constraintSet.apply {
        clone(container)
        connect(this@setConstraintForNextMemoRowWithNoBelow.id, ConstraintSet.TOP,
            formerMemoRowId.value, ConstraintSet.BOTTOM)
        connect(this@setConstraintForNextMemoRowWithNoBelow.id, ConstraintSet.START,
            container.id, ConstraintSet.START)
        connect(this@setConstraintForNextMemoRowWithNoBelow.id, ConstraintSet.END,
            container.id, ConstraintSet.END)
        applyTo(container)
    }
}

internal fun MemoRow.setConstraintForNextMemoRowWithBelow(container: ConstraintLayout,
                                                           formerMemoRowId: MemoRowId,
                                                           nextMemoRowId: MemoRowId) {
    container.addView(this)

    constraintSet.apply {
        clone(container)
        clear(nextMemoRowId.value, ConstraintSet.TOP)
        connect(this@setConstraintForNextMemoRowWithBelow.id, ConstraintSet.TOP,
            formerMemoRowId.value, ConstraintSet.BOTTOM)
        connect(this@setConstraintForNextMemoRowWithBelow.id, ConstraintSet.START,
            container.id, ConstraintSet.START)
        connect(this@setConstraintForNextMemoRowWithBelow.id, ConstraintSet.END,
            container.id, ConstraintSet.END)
        connect(nextMemoRowId.value, ConstraintSet.TOP,
            this@setConstraintForNextMemoRowWithBelow.id, ConstraintSet.BOTTOM)
        applyTo(container)
    }
}

internal fun MemoRow.setConstraintForDeleteMemoRow(container: ConstraintLayout,
                                                     formerMemoRowId: MemoRowId,
                                                     nextMemoRowId: MemoRowId) {
    constraintSet.apply {
        clone(container)
        clear(nextMemoRowId.value, ConstraintSet.TOP)
        clear(this@setConstraintForDeleteMemoRow.id, ConstraintSet.TOP)
        connect(nextMemoRowId.value, ConstraintSet.TOP, formerMemoRowId.value, ConstraintSet.BOTTOM)
        applyTo(container)
    }
}

internal fun MemoRow.removeMemoRowFromLayout(fragment: Fragment,
                                             container: ConstraintLayout,
                                             formerMemoRow: MemoRow) {
    val inputManager = fragment.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    inputManager.restartInput(formerMemoRow)
    container.removeView(this)
}

internal fun MemoRow.setConstraintForOptView(container: ConstraintLayout, newOptView: View,
                                              MemoRowMargin: Int, optViewMargin: Int = 0) {
    constraintSet.apply {
        clone(container)
        clear(this@setConstraintForOptView.id, ConstraintSet.START)
        connect(newOptView.id, ConstraintSet.START, container.id, ConstraintSet.START, optViewMargin)
        connect(newOptView.id, ConstraintSet.TOP, this@setConstraintForOptView.id, ConstraintSet.TOP)
        setVerticalBias(newOptView.id, 0f)
        connect(newOptView.id, ConstraintSet.BOTTOM, this@setConstraintForOptView.id, ConstraintSet.BOTTOM)
        connect(this@setConstraintForOptView.id, ConstraintSet.START,
            newOptView.id, ConstraintSet.END, MemoRowMargin)
        applyTo(container)
    }
}

internal fun MemoRow.setConstraintForDeleteOptView(container: ConstraintLayout) {
    constraintSet.apply {
        clone(container)
        clear(this@setConstraintForDeleteOptView.id, ConstraintSet.START)
        connect(this@setConstraintForDeleteOptView.id, ConstraintSet.START,
            container.id, ConstraintSet.START)
        applyTo(container)
    }
}

internal fun MemoRow.removeOptViewFromLayout(fragment: Fragment,
                                             container: ConstraintLayout,
                                             optViewId: MemoRowInfoProp) {
    when {
        optViewId is CheckBoxId && optViewId.value is Some ->
            container.removeView(fragment.requireActivity().findViewById(optViewId.value.t))
        optViewId is BulletId && optViewId.value is Some ->
            container.removeView(fragment.requireActivity().findViewById(optViewId.value.t))
    }
    this.setTextColor(Color.BLACK)
}
