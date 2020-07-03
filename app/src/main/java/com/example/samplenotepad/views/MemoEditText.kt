package com.example.samplenotepad.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.appcompat.widget.AppCompatEditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.viewModelScope
import com.example.samplenotepad.entities.DeleteCheckBox
import com.example.samplenotepad.entities.DeleteDot
import com.example.samplenotepad.entities.DeleteMemoRow
import com.example.samplenotepad.entities.TypeForExecuteMemoContents
import com.example.samplenotepad.usecases.firstMemoRow
import com.example.samplenotepad.viewModels.MemoEditViewModel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

class MemoEditText(
    val editViewModel: MemoEditViewModel,
    val container: ConstraintLayout,
    val executeActor: SendChannel<TypeForExecuteMemoContents>,
    context: Context,
    attrs: AttributeSet,
    defStyle: Int
) : AppCompatEditText(context, attrs, defStyle) {

    inner class MemoInputConnection(
        target: InputConnection?,
        mutable: Boolean
    ) : InputConnectionWrapper(target, mutable) {

        override fun sendKeyEvent(event: KeyEvent?): Boolean {
            if (memoLayoutParams.topToTop != container.id
                && this@MemoEditText.id != firstMemoRow.id
                && event?.action == KeyEvent.ACTION_DOWN)
                when {
                    event.keyCode == KeyEvent.KEYCODE_DEL && this@MemoEditText.selectionEnd == 0 -> {
                        Log.d("場所:MemoEditText#sendKeyEvent", "Delキーイベントに入った")

                        val memoContents = editViewModel.getMemoContents()
                        val memoRowInfo = memoContents[
                                memoContents.indexOfFirst { it.memoRowId.value == this@MemoEditText.id }
                        ]

                        editViewModel.viewModelScope.launch {
                            when {
                                memoRowInfo.checkBoxId.value != null ->
                                    executeActor.send(DeleteCheckBox(this@MemoEditText))
                                memoRowInfo.dotId.value != null ->
                                    executeActor.send(DeleteDot(this@MemoEditText))
                            }

                            executeActor.send(DeleteMemoRow(this@MemoEditText))
                        }
                    }
                    this@MemoEditText.selectionEnd == 0 -> editViewModel.updateIfAtFirstInText(true)
                }

            return super.sendKeyEvent(event)
        }
    }


    val memoLayoutParams = this@MemoEditText.layoutParams as ConstraintLayout.LayoutParams

    override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection {
        return MemoInputConnection(super.onCreateInputConnection(outAttrs), true)
    }

    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
    }
}