package com.example.samplenotepad.views.main

import android.content.Context
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.example.samplenotepad.*
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.viewModels.MemoEditViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_memo_edit.*


private lateinit var constraintSet: ConstraintSet

//internal fun ConstraintLayout.restartSoftwareKeyboardAndGetFocus() {
//    val editFragment = MemoEditFragment.getInstanceOrCreateNew()
//    val childCount = this.childCount
//
//    if (childCount != 0) {
//        val inputManager =
//            editFragment.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//
//        this.getChildAt(childCount - 1).apply {
//            inputManager.restartInput(this)
//            requestFocus()
//        }
//    }
//}

//internal fun View.showSoftwareKeyBoard(context: Context?) {
//    val inputManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//
//    inputManager.showSoftInput(this, InputMethodManager.SHOW_FORCED)
//}

//internal fun View.restartSoftwareKeyBoard(context: Context?) {
//    val inputManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//
//    inputManager.restartInput(this)
//}

internal fun View.hideSoftwareKeyBoard(context: Context){
    if (this.findFocus() != null) {
        val inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        inputManager.hideSoftInputFromWindow(this.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }
}

internal fun MemoRow.setFocus() = this.requestFocus()

internal fun MemoRow.setFocusAndTextAndCursorPosition(
    editViewModel: MemoEditViewModel,
    text: Text,
    selection: Int = 0
) {
    Log.d("場所:setFocusAndTextAndCursorPosition", "setFocusAndTextAndCursorPositionに入った")

    this.apply {
        setFocus()
        setText(text.value)
        setSelection(selection)
    }

    when (selection) {
        0 -> editViewModel.updateIfAtFirstInText(true)
        else -> editViewModel.updateIfAtFirstInText(false)
    }
    Log.d("場所:setTextAndCursorPosition", "ifAtFirstInText=${editViewModel.getIfAtFirstInText()}")
}

internal fun MemoEditFragment.showSnackbarForSavedMassageAtEditMemo() {
    this.saveImgBtn?.let { Snackbar.make(it, R.string.save_snackbar, Snackbar.LENGTH_SHORT).apply {
        view.alpha = 0.5f
        show()
    } }
}


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

    val targetParam = targetMemoRow.layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:setConstraintForFirstMemoRow [after set]", "targetMemoRow: start=${targetParam.startToStart} top=${targetParam.topToTop} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
}

internal fun ConstraintLayout.setConstraintForNextMemoRowWithNoBelow(
    newMemoRow: MemoRow,
    formerMemoRowId: MemoRowId,
    editViewModel: MemoEditViewModel,
    text: Text
) {
    Log.d("場所:setConstraintForNextMemoRowWithNoBelow", "Constraintのセットに入った")
    val targetParam = newMemoRow.layoutParams as ConstraintLayout.LayoutParams
    val formerParam = this.findViewById<MemoRow>(formerMemoRowId.value).layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:setConstraintForNextMemoRowWithNoBelow [before set]", "targetMemoRow: start=${targetParam.startToStart} top=${targetParam.topToTop} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
    Log.d("場所:setConstraintForNextMemoRowWithNoBelow [before set]", "formerMemoRow: start=${formerParam.startToStart} top=${formerParam.topToTop} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )

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

    newMemoRow.setFocusAndTextAndCursorPosition(editViewModel, text)

    Log.d("場所:setConstraintForNextMemoRowWithNoBelow [after set]", "targetMemoRow: start=${targetParam.startToStart} top=${targetParam.topToTop} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
    Log.d("場所:setConstraintForNextMemoRowWithNoBelow [after set]", "formerMemoRow: start=${formerParam.startToStart} top=${formerParam.topToTop} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )
}

internal fun ConstraintLayout.setConstraintForNextMemoRowWithBelow(
    newMemoRow: MemoRow,
    formerMemoRowId: MemoRowId,
    nextMemoRowId: MemoRowId,
    editViewModel: MemoEditViewModel,
    text: Text
) {
    Log.d("場所:setConstraintForNextMemoRowWithBelow", "Constraintのセットに入った")
    val targetParam = newMemoRow.layoutParams as ConstraintLayout.LayoutParams
    val formerParam = this.findViewById<MemoRow>(formerMemoRowId.value).layoutParams as ConstraintLayout.LayoutParams
    val nextParam = this.findViewById<MemoRow>(nextMemoRowId.value).layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:setConstraintForNextMemoRowWithBelow [before set]", "targetMemoRow: start=${targetParam.startToStart} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
    Log.d("場所:setConstraintForNextMemoRowWithBelow [before set]", "formerMemoRow: start=${formerParam.startToStart} top=${formerParam.topToBottom} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )
    Log.d("場所:setConstraintForNextMemoRowWithBelow [before set]", "nextMemoRow: start=${nextParam.startToStart} top=${nextParam.topToBottom} end=${nextParam.endToEnd} bottom=${nextParam.bottomToTop}" )

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

    newMemoRow.setFocusAndTextAndCursorPosition(editViewModel, text)

    Log.d("場所:setConstraintForNextMemoRowWithBelow [after set]", "targetMemoRow: start=${targetParam.startToStart} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
    Log.d("場所:setConstraintForNextMemoRowWithBelow [after set]", "formerMemoRow: start=${formerParam.startToStart} top=${formerParam.topToBottom} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )
    Log.d("場所:setConstraintForNextMemoRowWithBelow [after set]", "nextMemoRow: start=${nextParam.startToStart} top=${nextParam.topToBottom} end=${nextParam.endToEnd} bottom=${nextParam.bottomToTop}" )
}

internal fun ConstraintLayout.setConstraintForDeleteMemoRow(targetMemoRow: MemoRow,
                                                            formerMemoRowId: MemoRowId,
                                                            nextMemoRowId: MemoRowId
) {
    Log.d("場所:setConstraintForDeleteMemoRow", "Constraintのセットに入った")
    val targetParam = targetMemoRow.layoutParams as ConstraintLayout.LayoutParams
    val formerParam = this.findViewById<MemoRow>(formerMemoRowId.value).layoutParams as ConstraintLayout.LayoutParams
    val nextParam = this.findViewById<MemoRow>(nextMemoRowId.value).layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:setConstraintForDeleteMemoRow [before set]", "targetMemoRow: start=${targetParam.startToStart} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
    Log.d("場所:setConstraintForDeleteMemoRow [before set]", "formerMemoRow: start=${formerParam.startToStart} top=${formerParam.topToBottom} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )
    Log.d("場所:setConstraintForDeleteMemoRow [before set]", "nextMemoRow: start=${nextParam.startToStart} top=${nextParam.topToBottom} end=${nextParam.endToEnd} bottom=${nextParam.bottomToTop}" )

    constraintSet.apply {
        clone(this@setConstraintForDeleteMemoRow)
        clear(nextMemoRowId.value, ConstraintSet.TOP)
        clear(targetMemoRow.id, ConstraintSet.TOP)
        connect(nextMemoRowId.value, ConstraintSet.TOP, formerMemoRowId.value, ConstraintSet.BOTTOM, 0)
        applyTo(this@setConstraintForDeleteMemoRow)
    }

    Log.d("場所:setConstraintForDeleteMemoRow [after set]", "targetMemoRow: start=${targetParam.startToStart} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
    Log.d("場所:setConstraintForDeleteMemoRow [after set]", "formerMemoRow: start=${formerParam.startToStart} top=${formerParam.topToBottom} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )
    Log.d("場所:setConstraintForDeleteMemoRow [after set]", "nextMemoRow: start=${nextParam.startToStart} top=${nextParam.topToBottom} end=${nextParam.endToEnd} bottom=${nextParam.bottomToTop}" )
}

internal fun ConstraintLayout.removeMemoRowFromLayout(
    targetMemoRow: MemoRow,
    formerMemoRow: MemoRow,
    editViewModel: MemoEditViewModel
) {
    Log.d("場所:removeMemoRowFromLayout", "リムーブ処理に入った")
    val formerParam = formerMemoRow.layoutParams as ConstraintLayout.LayoutParams
    val textOfFormerMemoRow = formerMemoRow.text.toString()

    Log.d("場所:removeMemoRowFromLayout [before remove view]", "formerMemoRow: start=${formerParam.startToStart} top=${formerParam.topToBottom} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )

    this.removeView(targetMemoRow)

    formerMemoRow.apply {
        setFocusAndTextAndCursorPosition(
            editViewModel,
            Text(textOfFormerMemoRow + targetMemoRow.text.toString()),
            textOfFormerMemoRow.length
        )
    }

    Log.d("場所:removeMemoRowFromLayout [after remove view]", "formerMemoRow: start=${formerParam.startToStart} top=${formerParam.topToBottom} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )
}

internal fun ConstraintLayout.setConstraintForBulletsView(
    targetMemoRow: MemoRow,
    newBulletsView: View,
    MemoRowMargin: Int,
    bulletsViewMargin: Int = 0
) {
    Log.d("場所:setConstraintForBulletsView", "Constraintのセットに入った")
    val targetParam = targetMemoRow.layoutParams as ConstraintLayout.LayoutParams
    val bulletParam = newBulletsView.layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:setConstraintForBulletsView [before set]", "targetMemoRow: start=${targetParam.startToStart} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )

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

    Log.d("場所:setConstraintForBulletsView [after set]", "targetMemoRow: start=${targetParam.startToEnd} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
    Log.d("場所:setConstraintForBulletsView [after set]", "bulletView: start=${bulletParam.startToStart} top=${bulletParam.topToTop} end=${bulletParam.endToStart} bottom=${bulletParam.bottomToBottom}" )
}

internal fun ConstraintLayout.setConstraintForDeleteBulletsView(targetMemoRow: MemoRow) {
    Log.d("場所:setConstraintForDeleteBulletsView", "Constraintのセットに入った")
    val targetParam = targetMemoRow.layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:setConstraintForDeleteBulletsView [before set]", "targetMemoRow: start=${targetParam.startToEnd} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )

    constraintSet.apply {
        clone(this@setConstraintForDeleteBulletsView)
        clear(targetMemoRow.id, ConstraintSet.START)
        connect(targetMemoRow.id, ConstraintSet.START,
            this@setConstraintForDeleteBulletsView.id, ConstraintSet.START, 0)
        applyTo(this@setConstraintForDeleteBulletsView)
    }

    Log.d("場所:setConstraintForDeleteBulletsView [after set]", "targetMemoRow: start=${targetParam.startToStart} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
}

internal fun ConstraintLayout.removeBulletsViewFromLayout(fragment: MemoEditFragment,
                                                          targetMemoRow: MemoRow,
                                                          bulletsViewId: TypeForMemoRowInfo
) {
    Log.d("場所:removeBulletsViewFromLayout", "リムーブ処理に入った")
    val targetParam = targetMemoRow.layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:removeBulletsViewFromLayout [before remove view]", "targetMemoRow: start=${targetParam.startToStart} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )

    when {
        bulletsViewId is CheckBoxId && bulletsViewId.value != null ->
            this.removeView(fragment.requireActivity().findViewById(bulletsViewId.value))
        bulletsViewId is DotId && bulletsViewId.value != null ->
            this.removeView(fragment.requireActivity().findViewById(bulletsViewId.value))
    }

    targetMemoRow.setTextColor(resources.getColor(R.color.colorBlack, fragment.activity?.theme))

    Log.d("場所:removeBulletsViewFromLayout [after remove view]", "targetMemoRow: start=${targetParam.startToStart} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
}
