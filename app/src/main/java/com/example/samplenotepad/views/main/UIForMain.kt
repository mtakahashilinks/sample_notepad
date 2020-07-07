package com.example.samplenotepad.views.main

import android.content.Context
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.example.samplenotepad.*
import com.example.samplenotepad.entities.*
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
    text: Text, selection: Int = 0
) {
    Log.d("場所:setFocusAndTextAndCursorPosition", "setFocusAndTextAndCursorPositionに入った")

    this.apply {
        setFocus()
        setText(text.value)
        setSelection(selection)
    }
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
    Log.d("場所:setConstraintForFirstMemoEditText#追加後#Target(id=${targetMemoEditText.id})", "上=${targetParam.topToTop} 左=${targetParam.startToStart} 右=${targetParam.endToEnd}")
}

internal fun ConstraintLayout.setConstraintForNextMemoEditTextWithNoBelow(
    targetMemoEditText: MemoEditText,
    formerMemoEditTextId: MemoEditTextId,
    text: Text
) {
    Log.d("場所:setConstraintForNextMemoEditTextWithNoBelow", "Constraintのセットに入った")

    this.addView(targetMemoEditText)

    constraintSet.apply {
        clone(this@setConstraintForNextMemoEditTextWithNoBelow)
        connect(targetMemoEditText.id, ConstraintSet.TOP,
            formerMemoEditTextId.value, ConstraintSet.BOTTOM, 0)
        connect(targetMemoEditText.id, ConstraintSet.START,
            this@setConstraintForNextMemoEditTextWithNoBelow.id, ConstraintSet.START, 0)
        connect(targetMemoEditText.id, ConstraintSet.END,
            this@setConstraintForNextMemoEditTextWithNoBelow.id, ConstraintSet.END, 0)
        applyTo(this@setConstraintForNextMemoEditTextWithNoBelow)
    }

    targetMemoEditText.setFocusAndTextAndCursorPosition(text)

    val targetParam = targetMemoEditText.layoutParams as ConstraintLayout.LayoutParams
    val formerParam = this.findViewById<MemoEditText>(formerMemoEditTextId.value).layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:setConstraintForNextMemoEditTextWithNoBelow#追加後#Former(id=${formerMemoEditTextId.value})", "上=${formerParam.topToBottom} 左=${formerParam.startToStart} 右=${formerParam.endToEnd}" )
    Log.d("場所:setConstraintForNextMemoEditTextWithNoBelow#追加後#Target(id=${targetMemoEditText.id})", "上=${targetParam.topToBottom} 左=${targetParam.startToStart} 右=${targetParam.endToEnd}" )
}

internal fun ConstraintLayout.setConstraintForNextMemoEditTextWithBelow(
    targetMemoEditText: MemoEditText,
    formerMemoEditTextId: MemoEditTextId,
    nextMemoEditTextId: MemoEditTextId,
    text: Text
) {
    Log.d("場所:setConstraintForNextMemoEditTextWithBelow", "Constraintのセットに入った")

    this.addView(targetMemoEditText)

    constraintSet.apply {
        clone(this@setConstraintForNextMemoEditTextWithBelow)
        clear(nextMemoEditTextId.value, ConstraintSet.TOP)
        connect(targetMemoEditText.id, ConstraintSet.TOP,
            formerMemoEditTextId.value, ConstraintSet.BOTTOM, 0)
        connect(targetMemoEditText.id, ConstraintSet.START,
            this@setConstraintForNextMemoEditTextWithBelow.id, ConstraintSet.START, 0)
        connect(targetMemoEditText.id, ConstraintSet.END,
            this@setConstraintForNextMemoEditTextWithBelow.id, ConstraintSet.END, 0)
        connect(nextMemoEditTextId.value, ConstraintSet.TOP,
            targetMemoEditText.id, ConstraintSet.BOTTOM, 0)
        applyTo(this@setConstraintForNextMemoEditTextWithBelow)
    }

    targetMemoEditText.setFocusAndTextAndCursorPosition(text)

    val targetParam = targetMemoEditText.layoutParams as ConstraintLayout.LayoutParams
    val formerParam = this.findViewById<MemoEditText>(formerMemoEditTextId.value).layoutParams as ConstraintLayout.LayoutParams
    val nextParam = this.findViewById<MemoEditText>(nextMemoEditTextId.value).layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:setConstraintForNextMemoEditTextWithBelow#追加後#Former(id=${formerMemoEditTextId.value})", "上=${formerParam.topToBottom} 左=${formerParam.startToStart} 右=${formerParam.endToEnd}")
    Log.d("場所:setConstraintForNextMemoEditTextWithBelow#追加後#Target(id=${targetMemoEditText.id})", "上=${targetParam.topToBottom} 左=${targetParam.startToStart} 右=${targetParam.endToEnd}")
    Log.d("場所:setConstraintForNextMemoEditTextWithBelow#追加後#Next(id=${nextMemoEditTextId.value})", "上=${nextParam.topToBottom} 左=${nextParam.startToStart} 右=${nextParam.endToEnd}")
}

internal fun ConstraintLayout.setConstraintForDeleteMemoRow(
    targetMemoEditText: MemoEditText,
    formerMemoEditTextId: MemoEditTextId,
    nextMemoEditTextId: MemoEditTextId
) {
    Log.d("場所:setConstraintForDeleteMemoRow", "Constraintのセットに入った")

    constraintSet.apply {
        clone(this@setConstraintForDeleteMemoRow)
        clear(nextMemoEditTextId.value, ConstraintSet.TOP)
        clear(targetMemoEditText.id, ConstraintSet.TOP)
        connect(nextMemoEditTextId.value, ConstraintSet.TOP, formerMemoEditTextId.value, ConstraintSet.BOTTOM, 0)
        applyTo(this@setConstraintForDeleteMemoRow)
    }

    val formerParam = this.findViewById<MemoEditText>(formerMemoEditTextId.value).layoutParams as ConstraintLayout.LayoutParams
    val nextParam = this.findViewById<MemoEditText>(nextMemoEditTextId.value).layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:setConstraintForDeleteMemoRow#削除後#Former(id=${formerMemoEditTextId.value})", "上=${formerParam.topToBottom} 左=${formerParam.startToStart} 右=${formerParam.endToEnd}")
    Log.d("場所:setConstraintForDeleteMemoRow#削除後#Next(id=${nextMemoEditTextId.value})", "上=${nextParam.topToBottom} 左=${nextParam.startToStart} 右=${nextParam.endToEnd}")
}

internal fun ConstraintLayout.removeMemoRowFromLayout(
    targetMemoEditText: MemoEditText,
    formerMemoEditText: MemoEditText
) {
    Log.d("場所:removeMemoRowFromLayout", "リムーブ処理に入った")
    val textOfFormerMemoEditText = formerMemoEditText.text.toString()

    this.removeView(targetMemoEditText)

    formerMemoEditText.apply {
        setFocusAndTextAndCursorPosition(
            Text(textOfFormerMemoEditText + targetMemoEditText.text.toString()),
            textOfFormerMemoEditText.length
        )
    }

    val formerParam = formerMemoEditText.layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:removeMemoRowFromLayout#リムーブ後#Former(id=${formerMemoEditText.id})", "上=${formerParam.topToBottom} 左=${formerParam.startToStart} 右=${formerParam.endToEnd}")
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
    Log.d("場所:setConstraintForBulletsView#追加前#Target(id=${targetMemoEditText.id})", "上=${targetParam.topToBottom} 左=${targetParam.startToStart} 右=${targetParam.endToEnd}")

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

    Log.d("場所:setConstraintForBulletsView#追加後#Bullet(id=${newBulletsView.id})", "上=${bulletParam.topToTop} 左=${bulletParam.startToStart}")
    Log.d("場所:setConstraintForBulletsView#追加後#Target(id=${targetMemoEditText.id})", "上=${targetParam.topToBottom} 左=${targetParam.startToEnd} 右=${targetParam.endToEnd}")
}

internal fun ConstraintLayout.setConstraintForDeleteBulletsView(targetMemoEditText: MemoEditText) {
    Log.d("場所:setConstraintForDeleteBulletsView", "Constraintのセットに入った")
    val targetParam = targetMemoEditText.layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:setConstraintForDeleteBulletsView#削除前#Target(id=${targetMemoEditText.id})", "上=${targetParam.topToBottom} 左=${targetParam.startToEnd} 右=${targetParam.endToEnd}")

    constraintSet.apply {
        clone(this@setConstraintForDeleteBulletsView)
        clear(targetMemoEditText.id, ConstraintSet.START)
        connect(targetMemoEditText.id, ConstraintSet.START,
            this@setConstraintForDeleteBulletsView.id, ConstraintSet.START, 0)
        applyTo(this@setConstraintForDeleteBulletsView)
    }

    Log.d("場所:setConstraintForDeleteBulletsView#削除後#Target(id=${targetMemoEditText.id})", "上=${targetParam.topToBottom} 左=${targetParam.startToStart} 右=${targetParam.endToEnd}")
}

internal fun ConstraintLayout.removeBulletsViewFromLayout(fragment: MemoEditFragment,
                                                          targetMemoEditText: MemoEditText,
                                                          bulletsViewId: TypeForMemoRowInfo
) {
    Log.d("場所:removeBulletsViewFromLayout", "リムーブ処理に入った")

    when {
        bulletsViewId is CheckBoxId && bulletsViewId.value != null ->
            this.removeView(fragment.requireActivity().findViewById(bulletsViewId.value))
        bulletsViewId is DotId && bulletsViewId.value != null ->
            this.removeView(fragment.requireActivity().findViewById(bulletsViewId.value))
    }

    targetMemoEditText.setTextColor(resources.getColor(R.color.colorBlack, fragment.activity?.theme))

    val targetParam = targetMemoEditText.layoutParams as ConstraintLayout.LayoutParams
    Log.d("場所:removeBulletsViewFromLayout#リムーブ後#Target(id=${targetMemoEditText.id})", "上=${targetParam.topToBottom} 左=${targetParam.startToStart} 右=${targetParam.endToEnd}")
}
