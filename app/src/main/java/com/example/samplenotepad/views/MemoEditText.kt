package com.example.samplenotepad.views

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.appcompat.widget.AppCompatEditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.viewModelScope
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.usecases.getMemoContentsExecuteActor
import com.example.samplenotepad.viewModels.MemoEditViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MemoEditText(
    context: Context?,
    val editViewModel: MemoEditViewModel,
    val container: ConstraintLayout,
    val executeActor: SendChannel<TypeOfMemoContentsOperation>
) : AppCompatEditText(context) {

    inner class MemoInputConnection(
        target: InputConnection?,
        mutable: Boolean
    ) : InputConnectionWrapper(target, mutable) {

        override fun sendKeyEvent(event: KeyEvent?): Boolean = runBlocking {
            if (memoLayoutParams?.topToTop != container.id
             //   && this@MemoEditText.id != firstMemoEditText.id
                && event?.action == KeyEvent.ACTION_UP)
                when {
                    event.keyCode == KeyEvent.KEYCODE_DEL && this@MemoEditText.selectionEnd == 0 -> {
                        Log.d("場所:MemoEditText#sendKeyEvent", "Delキーイベントに入った")

                        val memoContentsDefer = CompletableDeferred<MemoContents>()
                        getMemoContentsExecuteActor().send(GetMemoContents(memoContentsDefer))

                        val memoContents = memoContentsDefer.await()
                        val memoRowInfo = memoContents[
                                memoContents.indexOfFirst { it.memoEditTextId.value == this@MemoEditText.id }
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

            return@runBlocking super.sendKeyEvent(event)
        }
    }


    val memoLayoutParams = this@MemoEditText.layoutParams as ConstraintLayout.LayoutParams?

    override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection {
        return MemoInputConnection(super.onCreateInputConnection(outAttrs), true)
    }

    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
    }
}