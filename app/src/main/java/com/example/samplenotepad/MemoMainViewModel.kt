package com.example.samplenotepad


import android.content.Context
import androidx.lifecycle.ViewModel
import arrow.core.internal.AtomicBooleanW
import android.graphics.Color
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
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.extensions.listk.semigroupK.combineK
import arrow.core.internal.AtomicRefW
import arrow.core.k


class MemoMainViewModel : ViewModel() {

    //viewPagerのバグのためのとりあえずのobject
    object ForFirstFocusInMainFragment {
        private lateinit var mainFragment: MemoMainFragment
        private lateinit var memoContainer: ConstraintLayout

        internal fun setFragmentAndContainer(fragment: MemoMainFragment, container: ConstraintLayout) {
            mainFragment = fragment
            memoContainer = container
        }

        internal fun setFocusAndSoftWareKeyboard() {
            memoContainer.getChildAt(0).requestFocus()
            val inputManager =
                mainFragment.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.restartInput(memoContainer.focusedChild)
        }
    }


    //executeMemoOperationが呼ばれた時、実行中の処理がなければそのまま実行。 他の処理が実行中なら処理をキューに入れる。
    //実行中の処理は処理の完了をexecuteMemoOperationに知らせる。
    // executeMemoOperationは、知らせを受けたらキューに入っている処理を順に開始させる。
    object MemoContentsOperation {
        private lateinit var memoContainer: ConstraintLayout
        private lateinit var mainFragment: MemoMainFragment
        private lateinit var mainViewModel: MemoMainViewModel

        private val queue = mutableListOf<ExecuteTypeForMemoContents>()
        //trueの時のみmemoContentsの変更処理を許可するトランザクション処理のためのフラグ
        private val flagForQueue = AtomicBooleanW(true)

        private val memoContents = AtomicRefW(MemoContents(listOf<MemoRowInfo>().k()))

        //backSpaceKeyが押された時にMemoRowの削除処理に入るかどうかの判断基準
        private val ifAtFirstInText = AtomicBooleanW(false)

        private fun push(executeId: ExecuteTypeForMemoContents) = queue.add(executeId)
        private fun pop() = queue.drop(1)

        private fun executeMemoOperation(executeId: ExecuteTypeForMemoContents) {
            Log.d("場所:MemoContentsOperation#excute", "executeに入った")

            when (flagForQueue.compareAndSet(expect = true, update = false)) {
                true -> {
                    Log.d("場所:MemoContentsOperation#excute", "flagForQueueがtrueの場合")
                    when (executeId) {
                        is CreateFirstMemoRow -> createFirstMemoRow(executeId)
                        is CreateNextMemoRow -> createNextMemoRow(executeId)
                        is DeleteMemoRow -> deleteMemoRow(executeId)
                        is AddCheckBox -> addCheckBox(executeId)
                        is DeleteCheckBox -> deleteCheckBox(executeId)
                        is AddBullet -> addBullet(executeId)
                        is DeleteBullet -> deleteBullet(executeId)
                        is ClearAll -> clearAllInMemoContents()
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
                                is CreateFirstMemoRow -> createFirstMemoRow(mExecuteId)
                                is CreateNextMemoRow -> createNextMemoRow(mExecuteId)
                                is DeleteMemoRow -> deleteMemoRow(mExecuteId)
                                is AddCheckBox -> addCheckBox(mExecuteId)
                                is DeleteCheckBox -> deleteCheckBox(mExecuteId)
                                is AddBullet -> addBullet(mExecuteId)
                                is DeleteBullet -> deleteBullet(mExecuteId)
                                is ClearAll -> clearAllInMemoContents()
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


        internal fun initMemoContentsOperation(fragment: MemoMainFragment,
                                               viewModel: MemoMainViewModel,
                                               container: ConstraintLayout,
                                               mMemoContents: Option<MemoContents>) {
            mainFragment = fragment
            mainViewModel = viewModel
            memoContainer = container

            when (mMemoContents){
                is Some ->  memoContents.getAndSet(mMemoContents.t)
                is None -> {
                    memoContents.getAndSet(MemoContents(listOf<MemoRowInfo>().k()))
                    executeMemoOperation(CreateFirstMemoRow(Text("")))
                }
            }
        }

        internal fun getMemoContents() = memoContents

        internal fun getMemoRowIndexInList(targetMemoRowId: MemoRowId): Int =
            memoContents.value.contentsList.indexOfFirst { 
                it.memoRowId.value == targetMemoRowId.value
            }


        private fun addToMemoContents(index: Int, memoRowInfo: MemoRowInfo) {
            Log.d("場所:addToMemoContents", "addToMemoContentsに入った")

            memoContents.updateAndGet { mMemoContents ->
                when {
                    index == 0 -> {
                        Log.d("場所:addToMemoContents", "indexが0の場合")
                        MemoContents(listOf(memoRowInfo).k())
                    }
                    index < (mMemoContents.contentsList.size - 1) -> {
                        Log.d("場所:addToMemoContents", "indexが0でなくリストサイズより小さい場合")

                        val beforeList = mMemoContents.contentsList.take(index).k()
                        val afterList = mMemoContents.contentsList.drop(index).k()
                        MemoContents(
                            beforeList.combineK(listOf(memoRowInfo).k()).combineK(afterList)
                        )
                    }
                    else -> {
                        Log.d("場所:addToMemoContents", "elseの場合(indexがリストの最後尾)")

                        MemoContents(
                            mMemoContents.contentsList.combineK(listOf(memoRowInfo).k())
                        )
                    }
                }
            }
        }

        private fun modifyMemoRowInfo(memoRowInfo: MemoRowInfo) {
            memoContents.updateAndGet { mMemoContents ->
                MemoContents(
                    mMemoContents.contentsList.flatMap {
                        if (it.memoRowId.value == memoRowInfo.memoRowId.value)
                            listOf(memoRowInfo).k() else listOf(it).k()
                    }
                )
            }
        }

        private fun deleteFromMemoContents(memoRowId: MemoRowId) {
            memoContents.updateAndGet { mMemoContents ->
                MemoContents(
                    mMemoContents.contentsList.filter { it.memoRowId.value != memoRowId.value }.k()
                )
            }
        }


        //メモコンテンツの最初の行をセットする
        private fun createFirstMemoRow(executeId: CreateFirstMemoRow) {
            Log.d("場所:createFirstMemoRow", "createFirstMemoRowに入った")
            val text = executeId.text
            val newMemoRow = createNewMemoRow().apply {
                memoContainer.setConstraintForFirstMemoRow(this)
                setTextAndCursorPosition(text, text.value.length)
            }
            Log.d("場所:createFirstMemoRow", "newMemoRowId=${newMemoRow.id}")

            addToMemoContents(0, MemoRowInfo(MemoRowId(newMemoRow.id), text))
            Log.d("場所:createFirstMemoRow", "memoContents=${memoContents.value.contentsList}"
            )

            executeMemoOperation(Complete())
        }

        private fun createNextMemoRow(executeId: CreateNextMemoRow) {
            Log.d("場所:createNextMemoRow", "createNextMemoRowに入った")
            Log.d("場所:createNextMemoRow", "text=${executeId.text}")
            val targetMemoRowId = memoContainer.findFocus().id
            val targetMemoRowIndexInList = getMemoRowIndexInList(MemoRowId(targetMemoRowId))
            val maxIndexOfList = memoContents.value.contentsList.size -1
            val newMemoRow = createNewMemoRow().apply {
                setBackSpaceKeyAction()
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
                        memoContainer.setConstraintForNextMemoRowWithNoBelow(this, MemoRowId(targetMemoRowId))
                    }

                    //FocusViewの下に他のViewがある場合
                    maxIndexOfList > targetMemoRowIndexInList -> {
                        Log.d("場所:createNextMemoRow", "下に他のViewがある場合")
                        val nextMemoRowId = memoContents.value
                            .contentsList[targetMemoRowIndexInList + 1].memoRowId
                        memoContainer.setConstraintForNextMemoRowWithBelow(
                            this, MemoRowId(targetMemoRowId), nextMemoRowId
                        )
                    }
                }

                setTextAndCursorPosition(executeId.text)
                addToMemoContents(targetMemoRowIndexInList + 1, MemoRowInfo(MemoRowId(newMemoRow.id)))
                Log.d("場所:createNextMemoRow",
                    "memoContents=${memoContents.value.contentsList}")
            }

            executeMemoOperation(Complete())
        }

        private fun deleteMemoRow(executeId: DeleteMemoRow) {
            Log.d("場所:deleteMemoRow", "deleteMemoRowに入った")
            val memoRow = executeId.memoRow
            val targetMemoRowIndexInList = getMemoRowIndexInList(MemoRowId(memoRow.id))
            val maxIndexOfList = memoContents.value.contentsList.size - 1
            val formerMemoRowId =
                memoContents.value.contentsList[targetMemoRowIndexInList - 1].memoRowId.value
            val formerMemoRow =
                mainFragment.requireActivity().findViewById<EditText>(formerMemoRowId)
            val textOfFormerMemoRow = formerMemoRow.text.toString()

            Log.d("場所:deleteMemoRow", "targetMemoRowId=${memoRow.id}")
            Log.d("場所:deleteMemoRow", "targetMemoRowIndexInList=$targetMemoRowIndexInList")
            Log.d("場所:deleteMemoRow", "maxIndexOfList=$maxIndexOfList")
            Log.d("場所:deleteMemoRow", "削除前のmemoContents=${memoContents.value.contentsList}")

            //targetMemoRowの下に他のViewがある場合の制約のセット。無い場合はそのままViewを削除する。
            if (maxIndexOfList > targetMemoRowIndexInList) {
                Log.d("場所:deleteMemoRow", "下に他のViewがある場合")
                val nextMemoRowId =
                    memoContents.value.contentsList[targetMemoRowIndexInList + 1].memoRowId
                memoContainer.setConstraintForDeleteMemoRow(
                    memoRow, MemoRowId(formerMemoRowId), nextMemoRowId
                )
            }

            formerMemoRow.setTextAndCursorPosition(
                Text(textOfFormerMemoRow + memoRow.text.toString()), textOfFormerMemoRow.length
            )

            if (textOfFormerMemoRow.isNotEmpty())
                ifAtFirstInText.compareAndSet(expect = true, update = false)

            memoContainer.removeMemoRowFromLayout(mainFragment, memoRow, formerMemoRow)
            deleteFromMemoContents(MemoRowId(memoRow.id))
            Log.d("場所:createFirstMemoRow", "削除後のmemoContents=${memoContents.value.contentsList}")

            executeMemoOperation(Complete())
        }

        private fun MemoRow.setEnterKeyAction() {
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

                                executeMemoOperation(CreateNextMemoRow(Text(textBringToNextRow)))
                            }
                        }
                        else -> return
                    }
                }
            } )
        }

        private fun MemoRow.setBackSpaceKeyAction() {
            setOnKeyListener { v, code, event ->
                when {
                    code == KeyEvent.KEYCODE_DEL && ifAtFirstInText.value -> {
                        Log.d("場所:setOnKeyListener ", "削除するMemoRowのId=${(v as MemoRow).id}")
                        Log.d("場所:setOnKeyListener ", "削除するMemoRowのText=${v.text}")
                        Log.d("場所:setOnKeyListener ", "selectionEnd=${v.selectionEnd}")
                        Log.d("場所:setOnKeyListener ", "ifAtFirstInText=${ifAtFirstInText.value}")

                        val targetMemoRowInfo =
                            memoContents.value.contentsList[getMemoRowIndexInList(MemoRowId(v.id))]

                        when {
                            targetMemoRowInfo.checkBoxId.value is Some ->
                                executeMemoOperation(DeleteCheckBox(v))
                            targetMemoRowInfo.bulletId.value is Some ->
                                executeMemoOperation(DeleteBullet(v))
                        }

                        executeMemoOperation(DeleteMemoRow(v))
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

        private fun createNewMemoRow(): MemoRow {
            return EditText(mainFragment.context, null, 0, R.style.MemoEditTextStyle).apply {
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
                id = View.generateViewId()

                setEnterKeyAction()

                //フォーカスが他に移るタイミングでMemoRowInfoのTextを更新する
                setOnFocusChangeListener { v, hasFocus ->
                    when {
                        v is MemoRow && !hasFocus -> {
                            val index = getMemoRowIndexInList(MemoRowId(v.id))
                            Log.d("場所:setOnFocusChangeListener", "targetMemoRowのindex=$index")
                            val modifiedMemoRowInfo = memoContents.value.contentsList[index].copy(
                                text = Text(v.text.toString())
                            )
                            Log.d("場所:setOnFocusChangeListener", "copiedMemoRowInfo=$modifiedMemoRowInfo")
                            modifyMemoRowInfo(modifiedMemoRowInfo)
                        }
                    }
                }
            }
        }


        //ボタンがクリックされた時のcheckBox処理の入り口
        internal fun MemoRow.operationCheckBox() {
            val targetMemoRowInfo =
                getMemoContents().value.contentsList[getMemoRowIndexInList(MemoRowId(this.id))]
            val checkBoxId = targetMemoRowInfo.checkBoxId.value

            when {
                targetMemoRowInfo.bulletId.value is Some<Int> -> {
                    executeMemoOperation(DeleteBullet(this))
                    executeMemoOperation(AddCheckBox(this))
                }

                checkBoxId is None -> executeMemoOperation(AddCheckBox(this))
                checkBoxId is Some<Int> -> executeMemoOperation(DeleteCheckBox(this))
            }
        }


        private fun addCheckBox(executeId: AddCheckBox) {
            Log.d("場所:addCheckBox", "checkBox追加処理に入った")
            val memoRow = executeId.memoRow
            val newCheckBox = createCheckBox(executeId)

            memoContainer.setConstraintForOptView(memoRow, newCheckBox, 80)

            val index = getMemoRowIndexInList(MemoRowId(memoRow.id))
            val modifiedMemoRowInfo = memoContents.value.contentsList[index].copy(
                checkBoxId = CheckBoxId(Some(newCheckBox.id)), checkBoxState = CheckBoxState(false)
            )
            modifyMemoRowInfo(modifiedMemoRowInfo)

            executeMemoOperation(Complete())
        }

        private fun deleteCheckBox(executeId: DeleteCheckBox) {
            Log.d("場所:deleteCheckBox", "checkBox削除処理に入った")
            val fragment = mainFragment
            val memoRow = executeId.memoRow
            val checkBoxId =
                memoContents.value.contentsList[getMemoRowIndexInList(MemoRowId(memoRow.id))].checkBoxId

            memoContainer.apply {
                setConstraintForDeleteOptView(memoRow)
                removeOptViewFromLayout(fragment, memoRow, checkBoxId)
            }

            val index = getMemoRowIndexInList(MemoRowId(memoRow.id))
            val modifiedMemoRowInfo = memoContents.value.contentsList[index].copy(
                checkBoxId = CheckBoxId(None), checkBoxState = CheckBoxState(false)
            )
            modifyMemoRowInfo(modifiedMemoRowInfo)

            executeMemoOperation(Complete())
        }

        private fun createCheckBox(executeId: AddCheckBox): CheckBox {
            return CheckBox(mainFragment.context).apply {
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
                        val copiedMemoRowInfo = memoContents.value.contentsList[targetIndexInList]
                            .copy(checkBoxState = CheckBoxState(true))

                        modifyMemoRowInfo(copiedMemoRowInfo)
                        memoRow.setTextColor(Color.GRAY)
                    }
                    false -> {
                        val copiedMemoRowInfo = memoContents.value.contentsList[targetIndexInList]
                            .copy(checkBoxState = CheckBoxState(false))

                        modifyMemoRowInfo(copiedMemoRowInfo)
                        memoRow.setTextColor(Color.BLACK)
                    }
                }
                Log.d("場所:OnCheckedChangeListener", """checkBoxのId=${checkBox.id} checkBoxState=${
                memoContents.value.contentsList[targetIndexInList].checkBoxState.value}""")
            }
        }


        //ボタンがクリックされた時のbullet処理の入り口
        internal fun MemoRow.operationBullet() {
            val targetMemoRowInfo =
                getMemoContents().value.contentsList[getMemoRowIndexInList(MemoRowId(this.id))]
            val bulletId = targetMemoRowInfo.bulletId.value

            when {
                targetMemoRowInfo.checkBoxId.value is Some<Int> -> {
                    executeMemoOperation(DeleteCheckBox(this))
                    executeMemoOperation(AddBullet(this))
                }
                bulletId is None -> executeMemoOperation(AddBullet(this))
                bulletId is Some<Int> -> executeMemoOperation(DeleteBullet(this))
            }
        }

        private fun addBullet(executeId: AddBullet) {
            Log.d("場所:addBullet", "bullet追加処理に入った")
            val memoRow = executeId.memoRow
            val newBullet = TextView(mainFragment.context).apply {
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
                id = View.generateViewId()
                setPadding(4)
                setBackgroundResource(R.color.colorTransparent)
                text = "・"
            }

            memoContainer.setConstraintForOptView(memoRow, newBullet, 50, 20)

            val index = getMemoRowIndexInList(MemoRowId(memoRow.id))
            val copiedMemoRowInfo =
                memoContents.value.contentsList[index].copy(bulletId = BulletId(Some(newBullet.id)))
            modifyMemoRowInfo(copiedMemoRowInfo)

            executeMemoOperation(Complete())
        }

        private fun deleteBullet(executeId: DeleteBullet) {
            Log.d("場所:deleteBullet", "bulletの削除処理に入った")
                val memoRow = executeId.memoRow
                val bulletId =
                    memoContents.value.contentsList[getMemoRowIndexInList(MemoRowId(memoRow.id))].bulletId

                memoContainer.apply {
                    setConstraintForDeleteOptView(memoRow)
                    removeOptViewFromLayout(mainFragment, memoRow, bulletId)
                }

                val index = getMemoRowIndexInList(MemoRowId(memoRow.id))
                val copiedMemoRowInfo = memoContents.value.contentsList[index].copy(bulletId = BulletId(None))
                modifyMemoRowInfo(copiedMemoRowInfo)

                executeMemoOperation(Complete())
        }


        internal fun clearAll() {
            executeMemoOperation(ClearAll())
            executeMemoOperation(CreateFirstMemoRow(Text("")))
        }

        private fun clearAllInMemoContents() {
            Log.d("場所:clearAllInMemoContents", "ClearAll処理に入った")

            memoContainer.removeAllViews()
            memoContents.getAndSet(MemoContents(listOf<MemoRowInfo>().k()))
            Log.d("場所:clearAllInMemoContents", "memoContents=${getMemoContents().value}")
            ifAtFirstInText.compareAndSet(expect = false, update = true)

            executeMemoOperation(Complete())
        }
    }


    override fun onCleared() {
        super.onCleared()
        //ここに終了処理を書く
    }
}
