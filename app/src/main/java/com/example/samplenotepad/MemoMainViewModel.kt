package com.example.samplenotepad

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import androidx.lifecycle.ViewModel
import arrow.core.internal.AtomicBooleanW
import android.graphics.Color
import android.provider.CalendarContract
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import arrow.core.None
import arrow.core.Some
import arrow.core.k
import org.xmlpull.v1.XmlPullParserFactory


class MemoMainViewModel : ViewModel() {

    //executeMemoOperationが呼ばれた時、実行中の処理がなければそのまま実行。 他の処理が実行中なら処理をキューに入れる。
    //実行中の処理は処理の完了をexecuteMemoOperationに知らせる。
    // executeMemoOperationは、知らせを受けたらキューに入っている処理を順に開始させる。
    object MemoContentsOperation {
        private lateinit var container: ConstraintLayout
        private lateinit var fragment: Fragment //viewPagerのバグのためのとりあえずのプロパティ
        private val queue = mutableListOf<ExecuteTypeForMemoContents>()
        //trueの時のみmemoContentsの変更処理を許可するトランザクション処理のためのフラグ
        private val flagForQueue = AtomicBooleanW(true)
        private var memoContents = MemoContents(listOf<MemoRowInfo>().k())

        //backSpaceKeyが押された時にMemoRowの削除処理に入るかどうかの判断基準
        private val ifAtFirstInText = AtomicBooleanW(false)

        private fun push(executeId: ExecuteTypeForMemoContents) = queue.add(executeId)
        private fun pop() = queue.drop(1)

        internal fun executeMemoOperation(viewModel: MemoMainViewModel,
                                          executeId: ExecuteTypeForMemoContents) {
            Log.d("場所:MemoContentsOperation#excute", "executeに入った")

            when (flagForQueue.compareAndSet(expect = true, update = false)) {
                true -> {
                    Log.d("場所:MemoContentsOperation#excute", "flagForQueueがtrueの場合")
                    when (executeId) {
                        is CreateFirstMemoRow -> createFirstMemoRow(viewModel, executeId)
                        is CreateNextMemoRow -> createNextMemoRow(viewModel, executeId)
                        is DeleteMemoRow -> deleteMemoRow(viewModel, executeId)
                        is AddCheckBox -> addCheckBox(viewModel, executeId)
                        is DeleteCheckBox -> deleteCheckBox(viewModel, executeId)
                        is AddBullet -> addBullet(viewModel, executeId)
                        is DeleteBullet -> deleteBullet(viewModel, executeId)
                        is ClearAll -> clearAllInMemoContents(viewModel)
                        is Complete -> return
                    }
                }
                false -> {
                    Log.d("場所:MemoContentsOperation#excute", "flagForQueueがfalseの場合")
                    when {
                        executeId is Complete && queue.isNotEmpty() -> {
                            Log.d("場所:MemoContentsOperation#excute",
                                "実行中の処理が終了。且つQueueが空でない場合")
                            val mExecuteId = queue[0]

                            pop()
                            flagForQueue.value = true

                            when (mExecuteId) {
                                is CreateFirstMemoRow -> createFirstMemoRow(viewModel, mExecuteId)
                                is CreateNextMemoRow -> createNextMemoRow(viewModel, mExecuteId)
                                is DeleteMemoRow -> deleteMemoRow(viewModel, mExecuteId)
                                is AddCheckBox -> addCheckBox(viewModel, mExecuteId)
                                is DeleteCheckBox -> deleteCheckBox(viewModel, mExecuteId)
                                is AddBullet -> addBullet(viewModel, mExecuteId)
                                is DeleteBullet -> deleteBullet(viewModel, mExecuteId)
                                is ClearAll -> clearAllInMemoContents(viewModel)
                            }

                        }
                        executeId is Complete && queue.isEmpty() -> {
                            Log.d("場所:MemoContentsOperation#excute",
                                "実行中の処理が終了。且つQueueが空の場合")
                            flagForQueue.value = true
                        }
                        executeId is CreateFirstMemoRow -> push(executeId)
                        executeId is CreateNextMemoRow -> push(executeId)
                        executeId is DeleteMemoRow -> push(executeId)
                        executeId is AddCheckBox -> push(executeId)
                        executeId is DeleteCheckBox -> push(executeId)
                        executeId is AddBullet -> push(executeId)
                        executeId is DeleteBullet -> push(executeId)
                        executeId is ClearAll -> push(executeId)
                        else -> return
                    }
                }
            }
        }


        //viewPagerのバグのためのとりあえずのメッソッド
        internal fun setFocusAndSoftWareKeyboard() {
            container.getChildAt(0).requestFocus()
            val inputManager =
                fragment.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.restartInput(container.focusedChild)
        }

        internal fun getMemoContents(): MemoContents = memoContents

        internal fun getMemoRowIndexInList(targetMemoRowId: MemoRowId): Int =
            memoContents.contentsList.indexOfFirst { it.memoRowId.value == targetMemoRowId.value }

        private fun addToMemoContents(index: Int, memoRowInfo: MemoRowInfo) {
            val copiedList = mutableListOf<MemoRowInfo>().apply {
                addAll(memoContents.contentsList.copy())
                add(index, memoRowInfo)
            }.k()
            memoContents = MemoContents(copiedList)
        }

        private fun modifyMemoRowInfo(memoRow: MemoRow, memoRowInfo: MemoRowInfo) {
            val index = getMemoRowIndexInList(MemoRowId(memoRow.id))
            val copiedList = mutableListOf<MemoRowInfo>().apply {
                addAll(memoContents.contentsList.copy())
                set(index, memoRowInfo)
            }.k()
            memoContents = MemoContents(copiedList)
        }

        private fun deleteFromMemoContents(memoRowId: MemoRowId) {
            val index = getMemoRowIndexInList(memoRowId)
            val copiedList = mutableListOf<MemoRowInfo>().apply {
                addAll(memoContents.contentsList.copy())
                removeAt(index)
            }.k()
            memoContents = MemoContents(copiedList)
        }


        //メモコンテンツの最初の行をセットする
        private fun createFirstMemoRow(viewModel: MemoMainViewModel, executeId: CreateFirstMemoRow) {
            Log.d("場所:createFirstMemoRow", "createFirstMemoRowに入った")
            viewModel.apply {
                container = executeId.container
                fragment = executeId.fragment //viewPagerのバグのためにとりあえず追加

                val text = executeId.text
                val newMemoRow = createNewMemoRow(executeId.fragment, viewModel).apply {
                    container.setConstraintForFirstMemoRow(this)
                    setTextAndCursorPosition(text, text.value.length)
                }
                Log.d("場所:createFirstMemoRow", "newMemoRowId=${newMemoRow.id}")

                addToMemoContents(0, MemoRowInfo(MemoRowId(newMemoRow.id), text))
                Log.d("場所:createFirstMemoRow", "memoContents=${memoContents.contentsList}")

                executeMemoOperation(this, Complete())
            }
        }

        private fun createNextMemoRow(viewModel: MemoMainViewModel, executeId: CreateNextMemoRow) {
            Log.d("場所:createNextMemoRow", "createNextMemoRowに入った")
            Log.d("場所:createNextMemoRow", "text=${executeId.text}")
            viewModel.apply {
                val targetMemoRowId = container.findFocus().id
                val targetMemoRowIndexInList = getMemoRowIndexInList(MemoRowId(targetMemoRowId))
                val maxIndexOfList = memoContents.contentsList.size -1
                val newMemoRow = createNewMemoRow(executeId.fragment, viewModel).apply {
                    setBackSpaceKeyAction(viewModel, executeId)
                }

                Log.d("場所:createNextMemoRow", "targetMemoRowId=$targetMemoRowId")
                Log.d("場所:createNextMemoRow", "newMemoRowId=${newMemoRow.id}")
                Log.d("場所:createNextMemoRow", "targetMemoRowIndexInList=$targetMemoRowIndexInList")
                Log.d("場所:createNextMemoRow", "maxIndexOfList=$maxIndexOfList")

                newMemoRow.apply {
                    when {
                        //FocusViewの下に他のViewが無い場合
                        maxIndexOfList == targetMemoRowIndexInList -> {
                            Log.d("場所:createNextMemoRow", "下に他のViewがない場合")
                            //TextViewとContainerViewの制約をセット
                            container.setConstraintForNextMemoRowWithNoBelow(this, MemoRowId(targetMemoRowId))
                        }

                        //FocusViewの下に他のViewがある場合
                        maxIndexOfList > targetMemoRowIndexInList -> {
                            Log.d("場所:createNextMemoRow", "下に他のViewがある場合")
                            val nextMemoRowId =
                                memoContents.contentsList[targetMemoRowIndexInList + 1].memoRowId
                            container.setConstraintForNextMemoRowWithBelow(
                                this, MemoRowId(targetMemoRowId), nextMemoRowId
                            )
                        }
                    }

                    setTextAndCursorPosition(executeId.text)
                    addToMemoContents(targetMemoRowIndexInList + 1, MemoRowInfo(MemoRowId(newMemoRow.id)))
                    Log.d("場所:createNextMemoRow", "memoContents=${memoContents.contentsList}")

                }

                executeMemoOperation(this, Complete())
            }
        }

        private fun deleteMemoRow(viewModel: MemoMainViewModel, executeId: DeleteMemoRow) {
            Log.d("場所:deleteMemoRow", "deleteMemoRowに入った")
            viewModel.apply {
                val memoRow = executeId.memoRow
                val targetMemoRowIndexInList = getMemoRowIndexInList(MemoRowId(memoRow.id))
                val maxIndexOfList = memoContents.contentsList.size - 1
                val formerMemoRowId =
                    memoContents.contentsList[targetMemoRowIndexInList - 1].memoRowId.value
                val formerMemoRow =
                    executeId.fragment.requireActivity().findViewById<EditText>(formerMemoRowId)
                val textOfFormerMemoRow = formerMemoRow.text.toString()

                Log.d("場所:deleteMemoRow", "targetMemoRowId=${memoRow.id}")
                Log.d("場所:deleteMemoRow", "targetMemoRowIndexInList=$targetMemoRowIndexInList")
                Log.d("場所:deleteMemoRow", "maxIndexOfList=$maxIndexOfList")
                Log.d("場所:deleteMemoRow", "削除前のmemoContents=${memoContents.contentsList}")

                //targetMemoRowの下に他のViewがある場合の制約のセット。無い場合はそのままViewを削除する。
                if (maxIndexOfList > targetMemoRowIndexInList) {
                    Log.d("場所:deleteMemoRow", "下に他のViewがある場合")
                    val nextMemoRowId = memoContents.contentsList[targetMemoRowIndexInList + 1].memoRowId
                    container.setConstraintForDeleteMemoRow(
                        memoRow, MemoRowId(formerMemoRowId), nextMemoRowId
                    )
                }

                formerMemoRow.setTextAndCursorPosition(
                    Text(textOfFormerMemoRow + memoRow.text.toString()), textOfFormerMemoRow.length
                )

                if (textOfFormerMemoRow.isNotEmpty())
                    ifAtFirstInText.compareAndSet(expect = true, update = false)

                container.removeMemoRowFromLayout(executeId.fragment, memoRow, formerMemoRow)
                deleteFromMemoContents(MemoRowId(memoRow.id))
                Log.d("場所:createFirstMemoRow", "memoContents=${memoContents.contentsList}")

                executeMemoOperation(this, Complete())
            }
        }

        private fun MemoRow.setEnterKeyAction(fragment: Fragment, viewModel: MemoMainViewModel) {
            this.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    Log.d("場所:beforeTextChanged", "s=$s start=$start  count=$count after=$after")
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    Log.d("場所:onTextChanged", "s=$s start=$start before=$before count=$count")

                    //フラグをfalseに変更する処理。trueにはsetBackSpaceKeyAction()の中で変更している
                    ifAtFirstInText.compareAndSet(expect = true, update = false)
                    Log.d("場所:onTextChanged#flag変更処理", "ifAtFirstInText=${ifAtFirstInText.value}")
                }

                override fun afterTextChanged(s: Editable?) {
                    Log.d("場所:afterTextChanged", "s=$s")
                    when {
                        s !== null && """\n""".toRegex().containsMatchIn(s.toString()) -> {
                            Log.d("場所:afterTextChanged", "改行処理に入った")
                            this@setEnterKeyAction.apply {
                                val textBringToNextRow = s.toString().substringAfter("\n")
                                setText(
                                    s.toString().replace("\n" + textBringToNextRow, ""),
                                    TextView.BufferType.NORMAL
                                )

                                executeMemoOperation(
                                    viewModel, CreateNextMemoRow(fragment, Text(textBringToNextRow))
                                )
                            }
                        }
                        else -> return
                    }
                }
            } )
        }

        private fun MemoRow.setBackSpaceKeyAction(viewModel: MemoMainViewModel,
                                                  executeId: CreateNextMemoRow) {
            setOnKeyListener { v, code, event ->
                when {
                    code == KeyEvent.KEYCODE_DEL && ifAtFirstInText.value -> {
                        Log.d("場所:setOnKeyListener ", "削除するMemoRowのId=${(v as MemoRow).id}")
                        Log.d("場所:setOnKeyListener ", "削除するMemoRowのText=${v.text}")
                        Log.d("場所:setOnKeyListener ", "selectionEnd=${v.selectionEnd}")
                        Log.d("場所:setOnKeyListener ", "ifAtFirstInText=${ifAtFirstInText.value}")

                        viewModel.apply {
                            val targetMemoRowInfo =
                                memoContents.contentsList[getMemoRowIndexInList(MemoRowId(v.id))]
                            val fragment = executeId.fragment

                            when {
                                targetMemoRowInfo.checkBoxId.value is Some ->
                                    executeMemoOperation(this, DeleteCheckBox(fragment, v))
                                targetMemoRowInfo.bulletId.value is Some ->
                                    executeMemoOperation(this, DeleteBullet(fragment, v))
                            }

                            executeMemoOperation(this, DeleteMemoRow(fragment, v))
                        }
                    }

                    //Delキー処理の後にフラグをtrueに変更することで、削除処理のタイミングを適正にしている
                    //falseにはsetEnterKeyAction()のtextWatcherの中で変更している
                    this.selectionEnd == 0 && !ifAtFirstInText.value -> {
                        ifAtFirstInText.value = true
                        Log.d("場所:setOnKeyListener#Frag変更処理", "atFirstInText=${ifAtFirstInText.value}")
                    }
                }
                false
            }
        }

        private fun createNewMemoRow(fragment:Fragment, viewModel: MemoMainViewModel): MemoRow {
            return EditText(fragment.context, null, 0, R.style.MemoEditTextStyle).apply {
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
                id = View.generateViewId()

                setEnterKeyAction(fragment, viewModel)

                //フォーカスが他に移るタイミングでMemoRowInfoのTextを更新する
                setOnFocusChangeListener { v, hasFocus ->
                    when {
                        v is MemoRow && !hasFocus -> {
                            viewModel.apply {
                                val index = getMemoRowIndexInList(MemoRowId(v.id))
                                Log.d("場所:setOnFocusChangeListener", "targetMemoRowのindex=$index")
                                val copiedMemoRowInfo =
                                    memoContents.contentsList[index].copy(text = Text(v.text.toString()))
                                Log.d("場所:setOnFocusChangeListener", "copiedMemoRowInfo=$copiedMemoRowInfo")
                                modifyMemoRowInfo(v, copiedMemoRowInfo)
                            }
                        }
                    }
                }
            }
        }


        private fun addCheckBox(viewModel: MemoMainViewModel, executeId: AddCheckBox) {
            Log.d("場所:addCheckBox", "checkBox追加処理に入った")
            viewModel.apply {
                val memoRow = executeId.memoRow
                val newCheckBox = createCheckBox(executeId)

                container.setConstraintForOptView(memoRow, newCheckBox, 80)

                val index = getMemoRowIndexInList(MemoRowId(memoRow.id))
                val copiedMemoRowInfo = memoContents.contentsList[index].copy(
                    checkBoxId = CheckBoxId(Some(newCheckBox.id)), checkBoxState = CheckBoxState(false)
                )
                modifyMemoRowInfo(memoRow, copiedMemoRowInfo)

                executeMemoOperation(this, Complete())
            }
        }

        private fun deleteCheckBox(viewModel: MemoMainViewModel, executeId: DeleteCheckBox) {
            Log.d("場所:deleteCheckBox", "checkBox削除処理に入った")
            viewModel.apply {
                val fragment = executeId.fragment
                val memoRow = executeId.memoRow
                val checkBoxId =
                    memoContents.contentsList[getMemoRowIndexInList(MemoRowId(memoRow.id))].checkBoxId

                container.apply {
                    setConstraintForDeleteOptView(memoRow)
                    removeOptViewFromLayout(fragment, memoRow, checkBoxId)
                }

                val index = getMemoRowIndexInList(MemoRowId(memoRow.id))
                val copiedMemoRowInfo = memoContents.contentsList[index].copy(
                    checkBoxId = CheckBoxId(None), checkBoxState = CheckBoxState(false)
                )
                modifyMemoRowInfo(memoRow, copiedMemoRowInfo)

                executeMemoOperation(this, Complete())
            }
        }

        private fun createCheckBox(executeId: AddCheckBox): CheckBox {
            return CheckBox(executeId.fragment.context).apply {
                layoutParams =
                    ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    )
                ViewGroup.MarginLayoutParams(0, 0)
                id = View.generateViewId()
                textSize = 0f
                setPadding(4)
                setCheckedChangeAction(this, executeId)
            }
        }

        private fun setCheckedChangeAction(checkBox: CheckBox, executeId: AddCheckBox) {
            val memoRow = executeId.memoRow

            checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                val targetIndexInList = getMemoRowIndexInList(MemoRowId(memoRow.id))
                when (isChecked) {
                    true -> {
                        val copiedMemoRowInfo = memoContents.contentsList[targetIndexInList]
                            .copy(checkBoxState = CheckBoxState(true))

                        modifyMemoRowInfo(memoRow, copiedMemoRowInfo)
                        memoRow.setTextColor(Color.GRAY)
                    }
                    false -> {
                        val copiedMemoRowInfo = memoContents.contentsList[targetIndexInList]
                            .copy(checkBoxState = CheckBoxState(false))

                        modifyMemoRowInfo(memoRow, copiedMemoRowInfo)
                        memoRow.setTextColor(Color.BLACK)
                    }
                }
                Log.d("場所:OnCheckedChangeListener", """checkBoxのId=${checkBox.id} checkBoxState=${
                    memoContents.contentsList[targetIndexInList].checkBoxState.value}""")
            }
        }

        private fun addBullet(viewModel: MemoMainViewModel, executeId: AddBullet) {
            Log.d("場所:addBullet", "bullet追加処理に入った")
            viewModel.apply {
                val memoRow = executeId.memoRow
                val newBullet = TextView(executeId.fragment.context).apply {
                    layoutParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    )
                    id = View.generateViewId()
                    setPadding(4)
                    setBackgroundResource(R.color.colorTransparent)
                    text = "・"
                }

                container.setConstraintForOptView(memoRow, newBullet, 50, 20)

                val index = getMemoRowIndexInList(MemoRowId(memoRow.id))
                val copiedMemoRowInfo =
                    memoContents.contentsList[index].copy(bulletId = BulletId(Some(newBullet.id)))
                modifyMemoRowInfo(memoRow, copiedMemoRowInfo)

                executeMemoOperation(this, Complete())
            }
        }

        private fun deleteBullet(viewModel: MemoMainViewModel, executeId: DeleteBullet) {
            Log.d("場所:deleteBullet", "bulletの削除処理に入った")
            viewModel.apply {
                val memoRow = executeId.memoRow
                val bulletId =
                    memoContents.contentsList[getMemoRowIndexInList(MemoRowId(memoRow.id))].bulletId

                container.apply {
                    setConstraintForDeleteOptView(memoRow)
                    removeOptViewFromLayout(executeId.fragment, memoRow, bulletId)
                }

                val index = getMemoRowIndexInList(MemoRowId(memoRow.id))
                val copiedMemoRowInfo = memoContents.contentsList[index].copy(bulletId = BulletId(None))
                modifyMemoRowInfo(memoRow, copiedMemoRowInfo)

                executeMemoOperation(this, Complete())
            }
        }

        private fun clearAllInMemoContents(viewModel: MemoMainViewModel) {
            Log.d("場所:clearAllInMemoContents", "ClearAll処理に入った")
            memoContents = MemoContents(listOf<MemoRowInfo>().k())
            ifAtFirstInText.compareAndSet(expect = false, update = true)

            executeMemoOperation(viewModel, Complete())
        }
    }


    override fun onCleared() {
        super.onCleared()
        //ここに終了処理を書く
    }
}
