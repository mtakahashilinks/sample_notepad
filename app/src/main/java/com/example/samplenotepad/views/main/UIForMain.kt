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
import com.example.samplenotepad.views.MemoEditText
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

internal fun MemoEditText.setFocus() = this.requestFocus()

internal fun MemoEditText.setFocusAndTextAndCursorPosition(
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


internal fun ConstraintLayout.setConstraintForFirstMemoEditText(targetMemoEditText: MemoEditText) {
    Log.d("場所:setConstraintForFirstMemoEditText", "Constraintのセットに入った")
    constraintSet = ConstraintSet()

    this.addView(targetMemoEditText)

    constraintSet.apply {
        clone(this@setConstraintForFirstMemoEditText)
        connect(targetMemoEditText.id, ConstraintSet.TOP,
            this@setConstraintForFirstMemoEditText.id, ConstraintSet.TOP, 0)
        connect(targetMemoEditText.id, ConstraintSet.START,
            this@setConstraintForFirstMemoEditText.id, ConstraintSet.START, 0)
        connect(targetMemoEditText.id, ConstraintSet.END,
            this@setConstraintForFirstMemoEditText.id, ConstraintSet.END, 0)
        applyTo(this@setConstraintForFirstMemoEditText)
    }

    val targetParam = targetMemoEditText.layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:setConstraintForFirstMemoEditText [after set]", "targetMemoEditText: start=${targetParam.startToStart} top=${targetParam.topToTop} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
}

internal fun ConstraintLayout.setConstraintForNextMemoEditTextWithNoBelow(
    newMemoEditText: MemoEditText,
    formerMemoEditTextId: MemoEditTextId,
    editViewModel: MemoEditViewModel,
    text: Text
) {
    Log.d("場所:setConstraintForNextMemoEditTextWithNoBelow", "Constraintのセットに入った")
    val targetParam = newMemoEditText.layoutParams as ConstraintLayout.LayoutParams
    val formerParam = this.findViewById<MemoEditText>(formerMemoEditTextId.value).layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:setConstraintForNextMemoEditTextWithNoBelow [before set]", "targetMemoEditText: start=${targetParam.startToStart} top=${targetParam.topToTop} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
    Log.d("場所:setConstraintForNextMemoEditTextWithNoBelow [before set]", "formerMemoEditText: start=${formerParam.startToStart} top=${formerParam.topToTop} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )

    this.addView(newMemoEditText)

    constraintSet.apply {
        clone(this@setConstraintForNextMemoEditTextWithNoBelow)
        connect(newMemoEditText.id, ConstraintSet.TOP,
            formerMemoEditTextId.value, ConstraintSet.BOTTOM, 0)
        connect(newMemoEditText.id, ConstraintSet.START,
            this@setConstraintForNextMemoEditTextWithNoBelow.id, ConstraintSet.START, 0)
        connect(newMemoEditText.id, ConstraintSet.END,
            this@setConstraintForNextMemoEditTextWithNoBelow.id, ConstraintSet.END, 0)
        applyTo(this@setConstraintForNextMemoEditTextWithNoBelow)
    }

    newMemoEditText.setFocusAndTextAndCursorPosition(editViewModel, text)

    Log.d("場所:setConstraintForNextMemoEditTextWithNoBelow [after set]", "targetMemoEditText: start=${targetParam.startToStart} top=${targetParam.topToTop} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
    Log.d("場所:setConstraintForNextMemoEditTextWithNoBelow [after set]", "formerMemoEditText: start=${formerParam.startToStart} top=${formerParam.topToTop} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )
}

internal fun ConstraintLayout.setConstraintForNextMemoEditTextWithBelow(
    newMemoEditText: MemoEditText,
    formerMemoEditTextId: MemoEditTextId,
    nextMemoEditTextId: MemoEditTextId,
    editViewModel: MemoEditViewModel,
    text: Text
) {
    Log.d("場所:setConstraintForNextMemoEditTextWithBelow", "Constraintのセットに入った")
    val targetParam = newMemoEditText.layoutParams as ConstraintLayout.LayoutParams
    val formerParam = this.findViewById<MemoEditText>(formerMemoEditTextId.value).layoutParams as ConstraintLayout.LayoutParams
    val nextParam = this.findViewById<MemoEditText>(nextMemoEditTextId.value).layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:setConstraintForNextMemoEditTextWithBelow [before set]", "targetMemoEditText: start=${targetParam.startToStart} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
    Log.d("場所:setConstraintForNextMemoEditTextWithBelow [before set]", "formerMemoEditText: start=${formerParam.startToStart} top=${formerParam.topToBottom} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )
    Log.d("場所:setConstraintForNextMemoEditTextWithBelow [before set]", "nextMemoEditText: start=${nextParam.startToStart} top=${nextParam.topToBottom} end=${nextParam.endToEnd} bottom=${nextParam.bottomToTop}" )

    this.addView(newMemoEditText)

    constraintSet.apply {
        clone(this@setConstraintForNextMemoEditTextWithBelow)
        clear(nextMemoEditTextId.value, ConstraintSet.TOP)
        connect(newMemoEditText.id, ConstraintSet.TOP,
            formerMemoEditTextId.value, ConstraintSet.BOTTOM, 0)
        connect(newMemoEditText.id, ConstraintSet.START,
            this@setConstraintForNextMemoEditTextWithBelow.id, ConstraintSet.START, 0)
        connect(newMemoEditText.id, ConstraintSet.END,
            this@setConstraintForNextMemoEditTextWithBelow.id, ConstraintSet.END, 0)
        connect(nextMemoEditTextId.value, ConstraintSet.TOP,
            newMemoEditText.id, ConstraintSet.BOTTOM, 0)
        applyTo(this@setConstraintForNextMemoEditTextWithBelow)
    }

    newMemoEditText.setFocusAndTextAndCursorPosition(editViewModel, text)

    Log.d("場所:setConstraintForNextMemoEditTextWithBelow [after set]", "targetMemoEditText: start=${targetParam.startToStart} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
    Log.d("場所:setConstraintForNextMemoEditTextWithBelow [after set]", "formerMemoEditText: start=${formerParam.startToStart} top=${formerParam.topToBottom} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )
    Log.d("場所:setConstraintForNextMemoEditTextWithBelow [after set]", "nextMemoEditText: start=${nextParam.startToStart} top=${nextParam.topToBottom} end=${nextParam.endToEnd} bottom=${nextParam.bottomToTop}" )
}

internal fun ConstraintLayout.setConstraintForDeleteMemoRow(targetMemoEditText: MemoEditText,
                                                            formerMemoEditTextId: MemoEditTextId,
                                                            nextMemoEditTextId: MemoEditTextId
) {
    Log.d("場所:setConstraintForDeleteMemoRow", "Constraintのセットに入った")
    val targetParam = targetMemoEditText.layoutParams as ConstraintLayout.LayoutParams
    val formerParam = this.findViewById<MemoEditText>(formerMemoEditTextId.value).layoutParams as ConstraintLayout.LayoutParams
    val nextParam = this.findViewById<MemoEditText>(nextMemoEditTextId.value).layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:setConstraintForDeleteMemoRow [before set]", "targetMemoEditText: start=${targetParam.startToStart} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
    Log.d("場所:setConstraintForDeleteMemoRow [before set]", "formerMemoEditText: start=${formerParam.startToStart} top=${formerParam.topToBottom} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )
    Log.d("場所:setConstraintForDeleteMemoRow [before set]", "nextMemoEditText: start=${nextParam.startToStart} top=${nextParam.topToBottom} end=${nextParam.endToEnd} bottom=${nextParam.bottomToTop}" )

    constraintSet.apply {
        clone(this@setConstraintForDeleteMemoRow)
        clear(nextMemoEditTextId.value, ConstraintSet.TOP)
        clear(targetMemoEditText.id, ConstraintSet.TOP)
        connect(nextMemoEditTextId.value, ConstraintSet.TOP, formerMemoEditTextId.value, ConstraintSet.BOTTOM, 0)
        applyTo(this@setConstraintForDeleteMemoRow)
    }

    Log.d("場所:setConstraintForDeleteMemoRow [after set]", "targetMemoEditText: start=${targetParam.startToStart} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
    Log.d("場所:setConstraintForDeleteMemoRow [after set]", "formerMemoEditText: start=${formerParam.startToStart} top=${formerParam.topToBottom} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )
    Log.d("場所:setConstraintForDeleteMemoRow [after set]", "nextMemoEditText: start=${nextParam.startToStart} top=${nextParam.topToBottom} end=${nextParam.endToEnd} bottom=${nextParam.bottomToTop}" )
}

internal fun ConstraintLayout.removeMemoRowFromLayout(
    targetMemoEditText: MemoEditText,
    formerMemoEditText: MemoEditText,
    editViewModel: MemoEditViewModel
) {
    Log.d("場所:removeMemoRowFromLayout", "リムーブ処理に入った")
    val formerParam = formerMemoEditText.layoutParams as ConstraintLayout.LayoutParams
    val textOfFormerMemoEditText = formerMemoEditText.text.toString()

    Log.d("場所:removeMemoRowFromLayout [before remove view]", "formerMemoEditText: start=${formerParam.startToStart} top=${formerParam.topToBottom} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )

    this.removeView(targetMemoEditText)

    formerMemoEditText.apply {
        setFocusAndTextAndCursorPosition(
            editViewModel,
            Text(textOfFormerMemoEditText + targetMemoEditText.text.toString()),
            textOfFormerMemoEditText.length
        )
    }

    Log.d("場所:removeMemoRowFromLayout [after remove view]", "formerMemoEditText: start=${formerParam.startToStart} top=${formerParam.topToBottom} end=${formerParam.endToEnd} bottom=${formerParam.bottomToTop}" )
}

internal fun ConstraintLayout.setConstraintForBulletsView(
    targetMemoEditText: MemoEditText,
    newBulletsView: View,
    MemoEditTextMargin: Int,
    bulletsViewMargin: Int = 0
) {
    Log.d("場所:setConstraintForBulletsView", "Constraintのセットに入った")
    val targetParam = targetMemoEditText.layoutParams as ConstraintLayout.LayoutParams
    val bulletParam = newBulletsView.layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:setConstraintForBulletsView [before set]", "targetMemoEditText: start=${targetParam.startToStart} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )

    this.addView(newBulletsView)

    constraintSet.apply {
        clone(this@setConstraintForBulletsView)
        clear(targetMemoEditText.id, ConstraintSet.START)
        connect(newBulletsView.id, ConstraintSet.START,
            this@setConstraintForBulletsView.id, ConstraintSet.START, bulletsViewMargin)
        connect(newBulletsView.id, ConstraintSet.TOP, targetMemoEditText.id, ConstraintSet.TOP, 0)
        setVerticalBias(newBulletsView.id, 0f)
        connect(newBulletsView.id, ConstraintSet.BOTTOM, targetMemoEditText.id, ConstraintSet.BOTTOM, 0)
        connect(targetMemoEditText.id, ConstraintSet.START, newBulletsView.id, ConstraintSet.END, MemoEditTextMargin)
        applyTo(this@setConstraintForBulletsView)
    }

    Log.d("場所:setConstraintForBulletsView [after set]", "targetMemoEditText: start=${targetParam.startToEnd} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
    Log.d("場所:setConstraintForBulletsView [after set]", "bulletView: start=${bulletParam.startToStart} top=${bulletParam.topToTop} end=${bulletParam.endToStart} bottom=${bulletParam.bottomToBottom}" )
}

internal fun ConstraintLayout.setConstraintForDeleteBulletsView(targetMemoEditText: MemoEditText) {
    Log.d("場所:setConstraintForDeleteBulletsView", "Constraintのセットに入った")
    val targetParam = targetMemoEditText.layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:setConstraintForDeleteBulletsView [before set]", "targetMemoEditText: start=${targetParam.startToEnd} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )

    constraintSet.apply {
        clone(this@setConstraintForDeleteBulletsView)
        clear(targetMemoEditText.id, ConstraintSet.START)
        connect(targetMemoEditText.id, ConstraintSet.START,
            this@setConstraintForDeleteBulletsView.id, ConstraintSet.START, 0)
        applyTo(this@setConstraintForDeleteBulletsView)
    }

    Log.d("場所:setConstraintForDeleteBulletsView [after set]", "targetMemoEditText: start=${targetParam.startToStart} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
}

internal fun ConstraintLayout.removeBulletsViewFromLayout(fragment: MemoEditFragment,
                                                          targetMemoEditText: MemoEditText,
                                                          bulletsViewId: TypeForMemoRowInfo
) {
    Log.d("場所:removeBulletsViewFromLayout", "リムーブ処理に入った")
    val targetParam = targetMemoEditText.layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:removeBulletsViewFromLayout [before remove view]", "targetMemoEditText: start=${targetParam.startToStart} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )

    when {
        bulletsViewId is CheckBoxId && bulletsViewId.value != null ->
            this.removeView(fragment.requireActivity().findViewById(bulletsViewId.value))
        bulletsViewId is DotId && bulletsViewId.value != null ->
            this.removeView(fragment.requireActivity().findViewById(bulletsViewId.value))
    }

    targetMemoEditText.setTextColor(resources.getColor(R.color.colorBlack, fragment.activity?.theme))

    Log.d("場所:removeBulletsViewFromLayout [after remove view]", "targetMemoEditText: start=${targetParam.startToStart} top=${targetParam.topToBottom} end=${targetParam.endToEnd} bottom=${targetParam.bottomToTop}" )
}
