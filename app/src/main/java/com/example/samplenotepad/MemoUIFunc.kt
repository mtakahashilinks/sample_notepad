package com.example.samplenotepad

import android.content.Context
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import arrow.core.Some


private lateinit var constraintSet: ConstraintSet
private lateinit var mainFragment: MemoMainFragment //Log用


internal fun ConstraintLayout.setConstraintForFirstMemoRow(targetMemoRow: MemoRow,
                                                           fragment: MemoMainFragment) {
    Log.d("場所:setConstraintForFirstMemoRow", "Constraintのセットに入った")
    constraintSet = ConstraintSet()
    mainFragment = fragment //Log用

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
    val targetParam = targetMemoRow.layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:setConstraintForFirstMemoRow",
        "targetMemoRow: start=${targetParam.startToStart} top=${targetParam.topToTop} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
}

internal fun ConstraintLayout.setConstraintForNextMemoRowWithNoBelow(newMemoRow: MemoRow,
                                                                     formerMemoRowId: MemoRowId) {
    Log.d("場所:setConstraintForNextMemoRowWithNoBelow", "Constraintのセットに入った")
    val targetParam = newMemoRow.layoutParams as ConstraintLayout.LayoutParams
    val formerParam = mainFragment.requireActivity().findViewById<MemoRow>(formerMemoRowId.value).layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:setConstraintForNextMemoRowWithNoBelow", "before set")
    Log.d("場所:setConstraintForNextMemoRowWithNoBelow",
        "targetMemoRow: start=${targetParam.startToStart} top=${targetParam.topToTop} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
    Log.d("場所:setConstraintForNextMemoRowWithNoBelow",
        "formerMemoRow: start=${formerParam.startToStart} top=${formerParam.topToTop} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )

    this.addView(newMemoRow)

    constraintSet.apply {
        clone(this@setConstraintForNextMemoRowWithNoBelow)
        connect(newMemoRow.id, ConstraintSet.TOP,
            formerMemoRowId.value, ConstraintSet.BOTTOM, 0)
        connect(newMemoRow.id, ConstraintSet.START,
            this@setConstraintForNextMemoRowWithNoBelow.id, ConstraintSet.START, 0)
        connect(newMemoRow.id, ConstraintSet.END,
            this@setConstraintForNextMemoRowWithNoBelow.id, ConstraintSet.END, 0)
        applyTo(this@setConstraintForNextMemoRowWithNoBelow)
    }

    Log.d("場所:setConstraintForNextMemoRowWithNoBelow", "after set")
    Log.d("場所:setConstraintForNextMemoRowWithNoBelow",
        "targetMemoRow: start=${targetParam.startToStart} top=${targetParam.topToTop} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
    Log.d("場所:setConstraintForNextMemoRowWithNoBelow",
        "formerMemoRow: start=${formerParam.startToStart} top=${formerParam.topToTop} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )
}

internal fun ConstraintLayout.setConstraintForNextMemoRowWithBelow(newMemoRow: MemoRow,
                                                                   formerMemoRowId: MemoRowId,
                                                                   nextMemoRowId: MemoRowId) {
    Log.d("場所:setConstraintForNextMemoRowWithBelow", "Constraintのセットに入った")
    val targetParam = newMemoRow.layoutParams as ConstraintLayout.LayoutParams
    val formerParam = mainFragment.requireActivity().findViewById<MemoRow>(formerMemoRowId.value).layoutParams as ConstraintLayout.LayoutParams
    val nextParam = mainFragment.requireActivity().findViewById<MemoRow>(nextMemoRowId.value).layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:setConstraintForNextMemoRowWithBelow", "before set")
    Log.d("場所:setConstraintForNextMemoRowWithBelow",
        "targetMemoRow: start=${targetParam.startToStart} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
    Log.d("場所:setConstraintForNextMemoRowWithBelow",
        "formerMemoRow: start=${formerParam.startToStart} top=${formerParam.topToBottom} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )
    Log.d("場所:setConstraintForNextMemoRowWithBelow",
        "nextMemoRow: start=${nextParam.startToStart} top=${nextParam.topToBottom} end=${nextParam.endToEnd} bottom=${nextParam.bottomToTop}" )

    this.addView(newMemoRow)

    constraintSet.apply {
        clone(this@setConstraintForNextMemoRowWithBelow)
        clear(nextMemoRowId.value, ConstraintSet.TOP)
        connect(newMemoRow.id, ConstraintSet.TOP,
            formerMemoRowId.value, ConstraintSet.BOTTOM, 0)
        connect(newMemoRow.id, ConstraintSet.START,
            this@setConstraintForNextMemoRowWithBelow.id, ConstraintSet.START, 0)
        connect(newMemoRow.id, ConstraintSet.END,
            this@setConstraintForNextMemoRowWithBelow.id, ConstraintSet.END, 0)
        connect(nextMemoRowId.value, ConstraintSet.TOP,
            newMemoRow.id, ConstraintSet.BOTTOM, 0)
        applyTo(this@setConstraintForNextMemoRowWithBelow)
    }

    Log.d("場所:setConstraintForNextMemoRowWithBelow", "after set")
    Log.d("場所:setConstraintForNextMemoRowWithBelow",
        "targetMemoRow: start=${targetParam.startToStart} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
    Log.d("場所:setConstraintForNextMemoRowWithBelow",
        "formerMemoRow: start=${formerParam.startToStart} top=${formerParam.topToBottom} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )
    Log.d("場所:setConstraintForNextMemoRowWithBelow",
        "nextMemoRow: start=${nextParam.startToStart} top=${nextParam.topToBottom} end=${nextParam.endToEnd} bottom=${nextParam.bottomToTop}" )
}

internal fun ConstraintLayout.setConstraintForDeleteMemoRow(targetMemoRow: MemoRow,
                                                            formerMemoRowId: MemoRowId,
                                                            nextMemoRowId: MemoRowId) {
    Log.d("場所:setConstraintForDeleteMemoRow", "Constraintのセットに入った")
    val targetParam = targetMemoRow.layoutParams as ConstraintLayout.LayoutParams
    val formerParam = mainFragment.requireActivity().findViewById<MemoRow>(formerMemoRowId.value).layoutParams as ConstraintLayout.LayoutParams
    val nextParam = mainFragment.requireActivity().findViewById<MemoRow>(nextMemoRowId.value).layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:setConstraintForDeleteMemoRow", "before set")
    Log.d("場所:setConstraintForDeleteMemoRow",
        "targetMemoRow: start=${targetParam.startToStart} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
    Log.d("場所:setConstraintForDeleteMemoRow",
        "formerMemoRow: start=${formerParam.startToStart} top=${formerParam.topToBottom} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )
    Log.d("場所:setConstraintForDeleteMemoRow",
        "nextMemoRow: start=${nextParam.startToStart} top=${nextParam.topToBottom} end=${nextParam.endToEnd} bottom=${nextParam.bottomToTop}" )

    constraintSet.apply {
        clone(this@setConstraintForDeleteMemoRow)
        clear(nextMemoRowId.value, ConstraintSet.TOP)
        clear(targetMemoRow.id, ConstraintSet.TOP)
        connect(nextMemoRowId.value, ConstraintSet.TOP, formerMemoRowId.value, ConstraintSet.BOTTOM, 0)
        applyTo(this@setConstraintForDeleteMemoRow)
    }

    Log.d("場所:setConstraintForDeleteMemoRow", "after set")
    Log.d("場所:setConstraintForDeleteMemoRow",
        "targetMemoRow: start=${targetParam.startToStart} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
    Log.d("場所:setConstraintForDeleteMemoRow",
        "formerMemoRow: start=${formerParam.startToStart} top=${formerParam.topToBottom} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )
    Log.d("場所:setConstraintForDeleteMemoRow",
        "nextMemoRow: start=${nextParam.startToStart} top=${nextParam.topToBottom} end=${nextParam.endToEnd} bottom=${nextParam.bottomToTop}" )
}

internal fun ConstraintLayout.removeMemoRowFromLayout(fragment: MemoMainFragment,
                                                      targetMemoRow: MemoRow,
                                                      formerMemoRow: MemoRow) {
    Log.d("場所:removeMemoRowFromLayout", "リムーブ処理に入った")
    val formerParam = formerMemoRow.layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:removeMemoRowFromLayout", "before remove view")
    Log.d("場所:removeMemoRowFromLayout",
        "formerMemoRow: start=${formerParam.startToStart} top=${formerParam.topToBottom} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )

    val inputManager =
        fragment.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputManager.restartInput(formerMemoRow)

    this.removeView(targetMemoRow)

    Log.d("場所:removeMemoRowFromLayout", "after remove view")
    Log.d("場所:removeMemoRowFromLayout",
        "formerMemoRow: start=${formerParam.startToStart} top=${formerParam.topToBottom} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )
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

internal fun showMenuPopup(view: View, context: Context?) {
    PopupMenu(context, view).apply {
        setOnMenuItemClickListener {item ->
            when (item.itemId) {
                R.id.addFormat -> {
                    true
                }
            }
            false
        }

        menuInflater.inflate(R.menu.memo_format_menu, this.menu)

        show()
    }
}
