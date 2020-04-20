package com.example.samplenotepad

import android.content.Context
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import arrow.core.Some


private lateinit var constraintSet: ConstraintSet


internal fun ConstraintLayout.setConstraintForFirstMemoRow(targetMemoRow: MemoRow) {
    Log.d("場所:setConstraintForFirstMemoRow", "Constraintのセットに入った")
    constraintSet = ConstraintSet()

    this.addView(targetMemoRow)

    constraintSet.apply {
        clone(this@setConstraintForFirstMemoRow)
        connect(targetMemoRow.id, ConstraintSet.TOP,
            this@setConstraintForFirstMemoRow.id, ConstraintSet.TOP, 0)
        connect(targetMemoRow.id, ConstraintSet.START,
            this@setConstraintForFirstMemoRow.id, ConstraintSet.START, 0)
        connect(targetMemoRow.id, ConstraintSet.END,
            this@setConstraintForFirstMemoRow.id, ConstraintSet.END, 0)
        applyTo(this@setConstraintForFirstMemoRow)
    }
}

internal fun ConstraintLayout.setConstraintForNextMemoRowWithNoBelow(targetMemoRow: MemoRow,
                                                                     formerMemoRowId: MemoRowId) {
    Log.d("場所:setConstraintForNextMemoRowWithNoBelow", "Constraintのセットに入った")

    this.addView(targetMemoRow)

    constraintSet.apply {
        clone(this@setConstraintForNextMemoRowWithNoBelow)
        connect(targetMemoRow.id, ConstraintSet.TOP,
            formerMemoRowId.value, ConstraintSet.BOTTOM, 0)
        connect(targetMemoRow.id, ConstraintSet.START,
            this@setConstraintForNextMemoRowWithNoBelow.id, ConstraintSet.START, 0)
        connect(targetMemoRow.id, ConstraintSet.END,
            this@setConstraintForNextMemoRowWithNoBelow.id, ConstraintSet.END, 0)
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
            formerMemoRowId.value, ConstraintSet.BOTTOM, 0)
        connect(targetMemoRow.id, ConstraintSet.START,
            this@setConstraintForNextMemoRowWithBelow.id, ConstraintSet.START, 0)
        connect(targetMemoRow.id, ConstraintSet.END,
            this@setConstraintForNextMemoRowWithBelow.id, ConstraintSet.END, 0)
        connect(nextMemoRowId.value, ConstraintSet.TOP,
            targetMemoRow.id, ConstraintSet.BOTTOM, 0)
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
        connect(nextMemoRowId.value, ConstraintSet.TOP, formerMemoRowId.value, ConstraintSet.BOTTOM, 0)
        applyTo(this@setConstraintForDeleteMemoRow)
    }
}

internal fun ConstraintLayout.removeMemoRowFromLayout(fragment: MemoMainFragment,
                                                      targetMemoRow: MemoRow,
                                                      formerMemoRow: MemoRow) {
    val inputManager =
        fragment.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputManager.restartInput(formerMemoRow)

    this.removeView(targetMemoRow)
}

internal fun ConstraintLayout.setConstraintForBulletsView(targetMemoRow: MemoRow, newBulletsView: View,
                                                          MemoRowMargin: Int, bulletsViewMargin: Int = 0) {
    this.addView(newBulletsView)

    constraintSet.apply {
        clone(this@setConstraintForBulletsView)
        clear(targetMemoRow.id, ConstraintSet.START)
        connect(newBulletsView.id, ConstraintSet.START,
            this@setConstraintForBulletsView.id, ConstraintSet.START, bulletsViewMargin)
        connect(newBulletsView.id, ConstraintSet.TOP, targetMemoRow.id, ConstraintSet.TOP, 0)
        setVerticalBias(newBulletsView.id, 0f)
        connect(newBulletsView.id, ConstraintSet.BOTTOM, targetMemoRow.id, ConstraintSet.BOTTOM, 0)
        connect(targetMemoRow.id, ConstraintSet.START, newBulletsView.id, ConstraintSet.END, MemoRowMargin)
        applyTo(this@setConstraintForBulletsView)
    }
}

internal fun ConstraintLayout.setConstraintForDeleteBulletsView(targetMemoRow: MemoRow) {
    constraintSet.apply {
        clone(this@setConstraintForDeleteBulletsView)
        clear(targetMemoRow.id, ConstraintSet.START)
        connect(targetMemoRow.id, ConstraintSet.START,
            this@setConstraintForDeleteBulletsView.id, ConstraintSet.START, 0)
        applyTo(this@setConstraintForDeleteBulletsView)
    }
}

internal fun ConstraintLayout.removeBulletsViewFromLayout(fragment: MemoMainFragment,
                                                          targetMemoRow: MemoRow,
                                                          bulletsViewId: TypeForMemoRowInfo) {
    when {
        bulletsViewId is CheckBoxId && bulletsViewId.value is Some ->
            this.removeView(fragment.requireActivity().findViewById(bulletsViewId.value.t))
        bulletsViewId is DotId && bulletsViewId.value is Some ->
            this.removeView(fragment.requireActivity().findViewById(bulletsViewId.value.t))
    }
    targetMemoRow.setTextColor(resources.getColor(R.color.colorBlack, fragment.activity?.theme))
}
