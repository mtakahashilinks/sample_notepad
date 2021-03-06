package com.example.samplenotepad.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.usecases.firstMemoEditText
import com.example.samplenotepad.usecases.getMemoContentsOperationActor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

@SuppressLint("ViewConstructor")
class MemoEditText(
    context: Context,
    val viewModel: ViewModel,
    val operationActor: SendChannel<TypeOfMemoContentsOperation>
) : AppCompatEditText(context) {

    //下のonCreateInputConnection()でReturnする
    inner class MemoInputConnection(target: InputConnection?, mutable: Boolean)
        : InputConnectionWrapper(target, mutable) {

        //Enter処理とBackSpace処理
        override fun sendKeyEvent(event: KeyEvent?): Boolean =
            when {
                this@MemoEditText.id != firstMemoEditText.id
                        && event?.keyCode == KeyEvent.KEYCODE_DEL
                        && event.action != KeyEvent.ACTION_UP
                        && this@MemoEditText.selectionStart == 0 -> {
                    Log.d("場所:MemoEditText#sendKeyEvent", "Delキーイベントに入った")

                    viewModel.viewModelScope.launch {
                        val memoContentsDefer = CompletableDeferred<MemoContents>()
                        getMemoContentsOperationActor().send(GetMemoContents(memoContentsDefer))

                        val memoContents = memoContentsDefer.await()
                        val memoRowInfo =
                                memoContents.first { it.memoEditTextId.value == this@MemoEditText.id }

                        when {
                            memoRowInfo.checkBoxId.value != null ->
                                operationActor.send(DeleteCheckBox(this@MemoEditText))
                            memoRowInfo.dotId.value != null ->
                                operationActor.send(DeleteDot(this@MemoEditText))
                        }

                        operationActor.send(DeleteMemoRow(this@MemoEditText))
                    }

                    false
                }
                event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action != KeyEvent.ACTION_UP -> {
                    Log.d("場所:MemoEditText#sendKeyEvent", "Enterキーイベントに入った")

                    viewModel.viewModelScope.launch {
                        val textBringToNextRow =
                            this@MemoEditText.text.toString().substring(this@MemoEditText.selectionStart)

                        this@MemoEditText.setText(
                            this@MemoEditText.text.toString().replace(textBringToNextRow, ""),
                            BufferType.NORMAL
                        )

                        operationActor.send(CreateNextMemoEditText(Text(textBringToNextRow), CreateNewMemo))
                    }

                    false
                }
                else -> {
                    Log.d("場所:MemoEditText#sendKeyEvent", "elseに入った")
                    super.sendKeyEvent(event)
                }
            }
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection {
        Log.d("場所:MemoEditText", "onCreateInputConnectionが呼ばれた")

        return MemoInputConnection(super.onCreateInputConnection(outAttrs), true)
    }

    //このEditTextのFocusが外れた時にtextをMemoContentsにUpdateする
    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (this.isClickable && !focused)
            viewModel.viewModelScope.launch {
                operationActor.send(UpdateTextOfMemoRowInfo(this@MemoEditText))
            }
    }
}