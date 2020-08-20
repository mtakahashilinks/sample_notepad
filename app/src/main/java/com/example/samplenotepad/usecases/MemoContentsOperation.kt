package com.example.samplenotepad.usecases

import android.text.InputType
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.samplenotepad.*
import com.example.samplenotepad.data.saveMemoInfoIO
import com.example.samplenotepad.entities.*
import com.example.samplenotepad.entities.GetMemoContents
import com.example.samplenotepad.viewModels.DisplayViewModel
import com.example.samplenotepad.viewModels.MainViewModel
import com.example.samplenotepad.views.MemoEditText
import com.example.samplenotepad.views.SampleMemoApplication
import com.example.samplenotepad.views.display.DisplayFragment
import com.example.samplenotepad.views.main.*
import com.example.samplenotepad.views.main.setConstraintForFirstMemoEditText
import com.example.samplenotepad.views.main.setConstraintForNextMemoEditTextWithNoBelow
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor


internal lateinit var firstMemoEditText: MemoEditText private set
private lateinit var editFragment: MemoEditFragment
private lateinit var displayFragment: DisplayFragment
private lateinit var mainViewModel: MainViewModel
private lateinit var displayViewModel: DisplayViewModel
private lateinit var memoContainer: ConstraintLayout
private lateinit var operationActor: SendChannel<TypeOfMemoContentsOperation>
private lateinit var formerMemoEditTextForExistMemo: MemoEditText //databaseから読みだした既存のメモの表示の際にのみ使う

private val showMassageForSavedLiveData = MutableLiveData<TypeOfFragment>(NoneOfThem)

internal fun getShowMassageForSavedLiveData() = showMassageForSavedLiveData

internal fun initValueOfShowMassageForSavedLiveData() {
    showMassageForSavedLiveData.postValue(NoneOfThem)
}

internal fun getMemoContentsOperationActor() = operationActor

@ObsoleteCoroutinesApi
internal fun createMemoContentsOperationActor(viewModel: ViewModel) {
    operationActor = viewModel.viewModelScope.memoContentsOperationActor()
}

@ObsoleteCoroutinesApi
internal fun initMemoContentsOperation(
    fragment: Fragment,
    viewModel: ViewModel,
    container: ConstraintLayout,
    buildType: TypeOfBuildMemoOperation
) = runBlocking {
    when (buildType){
        is CreateNewMemo -> {
            editFragment = fragment as MemoEditFragment
            mainViewModel = viewModel as MainViewModel
            memoContainer = container

            mainViewModel.apply {
                getMemoContentsOperationActor().send(SetMemoContents(listOf<MemoRowInfo>()))

                operationActor.send(CreateFirstMemoEditText(Text(""), CreateNewMemo))

                updateSavePointOfMemoContents()
                clearIsChangedValueInOptionFragment()
            }
        }
        is EditExistMemo -> {
            editFragment = fragment as MemoEditFragment
            mainViewModel = viewModel as MainViewModel
            memoContainer = container

            val memoContentsDefer = CompletableDeferred<MemoContents>()
            operationActor.send(GetMemoContents(memoContentsDefer))

            createMemoRowsForExistMemo(buildType, memoContentsDefer.await())
        }
        is DisplayExistMemo -> {
            displayFragment = fragment as DisplayFragment
            displayViewModel = viewModel as DisplayViewModel
            memoContainer = container

            val memoContentsDefer = CompletableDeferred<MemoContents>()
            operationActor.send(GetMemoContents(memoContentsDefer))

            createMemoRowsForExistMemo(buildType, memoContentsDefer.await())
        }
    }
}

internal fun closeMemoContentsOperationActor() = operationActor.close()

//ボタンがクリックされた時のcheckBox処理の入り口
internal fun MemoEditText.checkBoxOperation() = runBlocking {
    val memoContentsDefer = CompletableDeferred<MemoContents>()
    operationActor.send(GetMemoContents(memoContentsDefer))

    val memoContents = memoContentsDefer.await()
    val memoRowInfo = memoContents.first { it.memoEditTextId.value == this@checkBoxOperation.id }
    val checkBoxId = memoRowInfo.checkBoxId.value

    when {
        memoRowInfo.dotId.value != null -> {
            operationActor.send(DeleteDot(this@checkBoxOperation))
            operationActor.send(AddCheckBox(this@checkBoxOperation, CreateNewMemo))
        }
        checkBoxId != null -> operationActor.send(DeleteCheckBox(this@checkBoxOperation))
        else -> operationActor.send(AddCheckBox(this@checkBoxOperation, CreateNewMemo))
    }
}

//ボタンがクリックされた時のdot処理の入り口
internal fun MemoEditText.dotOperation() = runBlocking {
    val memoContentsDefer = CompletableDeferred<MemoContents>()

    operationActor.send(GetMemoContents(memoContentsDefer))

    val memoContents = memoContentsDefer.await()
    val memoRowInfo = memoContents.first { it.memoEditTextId.value == this@dotOperation.id }
    val dotId = memoRowInfo.dotId.value

    when {
        memoRowInfo.checkBoxId.value != null -> {
            operationActor.send(DeleteCheckBox(this@dotOperation))
            operationActor.send(AddDot(this@dotOperation, CreateNewMemo))
        }
        dotId != null -> operationActor.send(DeleteDot(this@dotOperation))
        else -> operationActor.send(AddDot(this@dotOperation, CreateNewMemo))
    }
}

internal fun updateText() = runBlocking {
    val focusView = memoContainer.findFocus()

    if (focusView != null && focusView is MemoEditText)
        mainViewModel.viewModelScope.launch {
            operationActor.send(UpdateTextOfMemoRowInfo(focusView))
        }
}

internal fun clearAll() = runBlocking {
    Log.d("場所:clearAll", "ClearAll処理に入った")

    memoContainer.removeAllViews()

    mainViewModel.viewModelScope.launch {
        operationActor.send(CreateFirstMemoEditText(Text(""), CreateNewMemo))
    }.join()

    mainViewModel.updateSavePointOfMemoContents()
}

internal fun saveMemo(buildType: TypeOfBuildMemoOperation) = runBlocking {
    updateText() //まずmemoContentsのTextを更新する

    operationActor.send(SaveMemoInfo(buildType))

    when (buildType) {
        is DisplayExistMemo -> {
            displayViewModel.updateSavePointOfMemoContents()
            showMassageForSavedLiveData.postValue(com.example.samplenotepad.entities.DisplayFragment)
        }
        else -> {
            mainViewModel.apply {
                updateSavePointOfMemoContents()
                clearIsChangedValueInOptionFragment()
            }
            showMassageForSavedLiveData.postValue(EditFragment)
        }
    }
}

private fun saveOperation(operateType: SaveMemoInfo, memoContents: MemoContents) {
    Log.d("saveOperation", "save処理に入った")
    when (operateType.buildType) {
        is DisplayExistMemo -> displayViewModel.getMemoInfo().saveMemoInfoIO(displayViewModel, memoContents)
        else -> mainViewModel.getMemoInfo().saveMemoInfoIO(mainViewModel, memoContents)
    }
}

private fun createMemoRowsForExistMemo(
    buildType: TypeOfBuildMemoOperation,
    memoContents: MemoContents
) = runBlocking {
    suspend fun List<MemoRowInfo>.createFirstRow(): List<MemoRowInfo> {
        val targetMemoRowInfo = this@createFirstRow[0]

        operationActor.send(CreateFirstMemoEditText(targetMemoRowInfo.memoText, buildType, targetMemoRowInfo))

        return this.drop(1)
    }

    suspend fun List<MemoRowInfo>.createNextRow() {
        this.onEach { memoRowInfo ->
            operationActor.send(CreateNextMemoEditText(memoRowInfo.memoText, buildType, memoRowInfo))
        }
    }

    Log.d("場所:createMemoRowsForExistMemo", "memoContents=${memoContents.toList()}")

    //主要な処理
    when (buildType) {
        is DisplayExistMemo -> {
            memoContents.toList().createFirstRow().createNextRow()

            displayViewModel.updateSavePointOfMemoContents()
        }
        else -> {
            memoContents.toList().createFirstRow().createNextRow()

            mainViewModel.apply {
                updateSavePointOfMemoContents()
                clearIsChangedValueInOptionFragment()
            }
        }
    }
}

private fun MemoEditText.addCheckBoxAndDotForExistMemo(
    memoRowInfo: MemoRowInfo,
    buildType: TypeOfBuildMemoOperation,
    memoContents:MemoContents
) {
    if (memoRowInfo.checkBoxId.value != null)
        addCheckBox(
            AddCheckBox(this, buildType, memoRowInfo.checkBoxId.value, memoRowInfo.checkBoxState.value),
            memoContents
        )

    if (memoRowInfo.dotId.value != null)
        addDot(AddDot(this, buildType, memoRowInfo.dotId.value), memoContents)
}


//近い将来、代替えのAPIに切り替わるらしい
@ObsoleteCoroutinesApi
private fun CoroutineScope.memoContentsOperationActor() =
    actor<TypeOfMemoContentsOperation> {
        var memoContents = listOf<MemoRowInfo>()

        for (msg in channel) {
            Log.d("場所:memoContentsOperationActor1", "memoContentsOperationActorに入った operateType=$msg")
            Log.d("場所:memoContentsOperationActor2", "変更前:size=${memoContents.size} memoContents=$memoContents")

            when (msg) {
                is UpdateTextOfMemoRowInfo -> memoContents = updateTextOfMemoRowInfo(msg, memoContents)
                is CreateFirstMemoEditText -> memoContents = createFirstMemoEditText(msg, memoContents)
                is CreateNextMemoEditText -> memoContents = createNextMemoEditText(msg, memoContents)
                is DeleteMemoRow -> memoContents = deleteMemoRow(msg, memoContents)
                is AddCheckBox -> memoContents = addCheckBox(msg, memoContents)
                is DeleteCheckBox -> memoContents = deleteCheckBox(msg, memoContents)
                is ChangeCheckBoxState ->
                    memoContents = updateCheckBoxStateOfMemoContents(msg, memoContents)
                is AddDot -> memoContents = addDot(msg, memoContents)
                is DeleteDot -> memoContents = deleteDot(msg, memoContents)
                is SaveMemoInfo -> saveOperation(msg, memoContents)
                is SetMemoContents -> memoContents = msg.memoContents
                is GetMemoContents -> msg.response.complete(memoContents)
            }

            Log.d("場所:memoContentsOperationActor3", "変更後:size=${memoContents.size} memoContents=$memoContents")
            Log.d("場所:memoContentsOperationActor4", "memoContentsOperationActorが終わった operateType=$msg")
        }
    }


private fun updateTextOfMemoRowInfo(
    operationType: UpdateTextOfMemoRowInfo,
    memoContents: MemoContents
): MemoContents = memoContents.flatMap { memoRowInfo ->
    if (memoRowInfo.memoEditTextId.value == operationType.memoEditText.id)
        listOf(memoRowInfo.copy(memoText = Text(operationType.memoEditText.text.toString())))
    else listOf(memoRowInfo)
}


private fun MemoEditText.setCommonLayoutParams() {
    layoutParams = ConstraintLayout.LayoutParams(
        ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT
    )
    background = ResourcesCompat.getDrawable(
        resources, android.R.color.transparent, SampleMemoApplication.instance.theme
    )
    setPadding(4)
    isFocusable = true
    isFocusableInTouchMode = true
    inputType = InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE
    isSingleLine = false
}

private fun createNewMemoEditText(
    memoEditTextId: Int?, text : Text, buildType: TypeOfBuildMemoOperation
): MemoEditText = when (buildType) {
        is CreateNewMemo -> {
            MemoEditText(editFragment.requireContext(), mainViewModel, operationActor).apply {
                id = View.generateViewId()
                setCommonLayoutParams()
            }
        }
        is EditExistMemo -> {
            MemoEditText(editFragment.requireContext(), mainViewModel, operationActor).apply {
                setCommonLayoutParams()
                id = memoEditTextId ?: throw(NullPointerException("memoEditTextId mast be not null"))
                setText(text.value)
            }
        }
        is DisplayExistMemo -> {
            MemoEditText(displayFragment.requireContext(), mainViewModel, operationActor).apply {
                setCommonLayoutParams()
                id = memoEditTextId ?: throw(NullPointerException("memoEditTextId mast be not null"))
                setText(text.value)
                isFocusable = false
                isFocusableInTouchMode = false
            }
        }
    }

//MemoContentsの最初の行をセットする
private fun createFirstMemoEditText(
    operateType: CreateFirstMemoEditText,
    memoContents: MemoContents
): MemoContents {
    Log.d("場所:createFirstMemoEditText", "createFirstMemoEditTextに入った")

    val newMemoEditText = createNewMemoEditText(
        operateType.memoRowInfo?.memoEditTextId?.value, operateType.text, operateType.buildType
    )

    firstMemoEditText = newMemoEditText

    return when (operateType.buildType) {
        is CreateNewMemo -> {
            mainViewModel.viewModelScope
                .launch(Dispatchers.Main) { memoContainer.setConstraintForFirstMemoEditText(newMemoEditText) }

            Log.d("場所:createFirstMemoEditText", "newMemoEditTextId=${newMemoEditText.id}")

            newMemoEditText.setFocusAndTextAndCursorPosition(operateType.text)

            listOf(MemoRowInfo(MemoEditTextId(newMemoEditText.id), operateType.text))
        }
        else -> {
            memoContainer.setConstraintForFirstMemoEditText(newMemoEditText)

            if (operateType.memoRowInfo != null) {
                newMemoEditText.addCheckBoxAndDotForExistMemo(
                    operateType.memoRowInfo, operateType.buildType, memoContents
                )
            }

            formerMemoEditTextForExistMemo = newMemoEditText

            memoContents
        }
    }
}

private fun createNextMemoEditText(
    operateType: CreateNextMemoEditText,
    memoContents: MemoContents
): MemoContents {
    Log.d("場所:createNextMemoEditText", "createNextMemoEditTextに入った")

    val newMemoEditText = createNewMemoEditText(
        operateType.memoRowInfo?.memoEditTextId?.value, operateType.text, operateType.buildType
    )

    return when (operateType.buildType) {
        is CreateNewMemo -> {
            val targetMemoEditTextId = memoContainer.findFocus().id
            val indexOfTargetMemoRowInfo =
                memoContents.indexOfFirst { it.memoEditTextId.value == targetMemoEditTextId }
            val maxIndexOfList = memoContents.size - 1

            when (indexOfTargetMemoRowInfo == maxIndexOfList) {
                 true-> {
                    Log.d("場所:createNextMemoEditText", "下に他のViewがない場合のLayoutの制約設定とMemoContentsのUpdate")
                    memoContainer.setConstraintForNextMemoEditTextWithNoBelow(
                        newMemoEditText,
                        MemoEditTextId(targetMemoEditTextId),
                        operateType.text
                    )

                    memoContents.plus(listOf(MemoRowInfo(MemoEditTextId(newMemoEditText.id))))
                }
                false -> {
                    Log.d("場所:createNextMemoEditText", "下に他のViewがある場合のLayoutの制約とMemoContentsのUpdate")
                    val nextMemoEditTextId = memoContents[indexOfTargetMemoRowInfo + 1].memoEditTextId
                    val prefixList = memoContents.take(indexOfTargetMemoRowInfo + 1)
                    val suffixList = memoContents.drop(indexOfTargetMemoRowInfo + 1)

                    memoContainer.setConstraintForNextMemoEditTextWithBelow(
                        newMemoEditText,
                        MemoEditTextId(targetMemoEditTextId),
                        nextMemoEditTextId,
                        operateType.text
                    )

                    prefixList
                        .plus(listOf(MemoRowInfo(MemoEditTextId(newMemoEditText.id))))
                        .plus(suffixList)
                }
            }
        }
        else -> {
            //既存のメモの表示(DisplayExistMemo)か編集(EditExistMemo)の場合
            memoContainer.setConstraintForNextMemoEditTextWithNoBelow(
                newMemoEditText,
                MemoEditTextId(formerMemoEditTextForExistMemo.id),
                operateType.text
            )

            if (operateType.memoRowInfo?.checkBoxId != null || operateType.memoRowInfo?.dotId != null)
                newMemoEditText.addCheckBoxAndDotForExistMemo(
                    operateType.memoRowInfo, operateType.buildType, memoContents
                )

            formerMemoEditTextForExistMemo = newMemoEditText

            memoContents
        }
    }
}



private fun deleteMemoRow(
    operateType: DeleteMemoRow,
    memoContents: MemoContents
): MemoContents {
    Log.d("場所:deleteMemoRow", "deleteMemoRowに入った")

    val targetMemoEditText = operateType.memoEditText
    val indexOfTargetMemoRowInfo =
        memoContents.indexOfFirst { it.memoEditTextId.value == targetMemoEditText.id }
    val maxIndexOfList = memoContents.size - 1
    val formerMemoEditTextId = memoContents[indexOfTargetMemoRowInfo - 1].memoEditTextId.value
    val formerMemoEditText = editFragment.requireActivity().findViewById<MemoEditText>(formerMemoEditTextId)

    //FocusChangedListenerで処理をさせない為。プロパティの種類は何でも良い
    targetMemoEditText.isClickable = false

    //targetMemoEditTextの下に他のViewがある場合の制約のセット。無い場合はそのままViewを削除する。
    if (indexOfTargetMemoRowInfo < maxIndexOfList) {
        Log.d("場所:deleteMemoRow", "下に他のViewがある場合")
        val nextMemoEditTextId = memoContents[indexOfTargetMemoRowInfo + 1].memoEditTextId

        memoContainer.setConstraintForDeleteMemoRow(
            targetMemoEditText, MemoEditTextId(formerMemoEditTextId), nextMemoEditTextId
        )
    }

    memoContainer.removeMemoRowFromLayout(targetMemoEditText, formerMemoEditText)

    return memoContents.filter { it.memoEditTextId.value != targetMemoEditText.id }
}

private fun updateCheckBoxStateOfMemoContents(
    operateType: ChangeCheckBoxState,
    memoContents: MemoContents
): MemoContents = memoContents.map { memoRowInfo ->
    if (memoRowInfo.memoEditTextId.value == operateType.memoEditText.id)
        memoRowInfo.copy(checkBoxState = CheckBoxState(!memoRowInfo.checkBoxState.value))
    else memoRowInfo
}


private fun MemoEditText.changeTextColorByCheckBoxState(
    checkBoxState: Boolean,
    buildType: TypeOfBuildMemoOperation
) {
    when {
        buildType is DisplayExistMemo && checkBoxState->
            this.setTextColor(resources.getColor(R.color.colorGray, displayFragment.activity?.theme))
        buildType is DisplayExistMemo && !checkBoxState->
            this.setTextColor(resources.getColor(R.color.colorBlack, displayFragment.activity?.theme))
        buildType !is DisplayExistMemo && checkBoxState->
            this.setTextColor(resources.getColor(R.color.colorGray, editFragment.activity?.theme))
        buildType !is DisplayExistMemo && !checkBoxState->
            this.setTextColor(resources.getColor(R.color.colorBlack, editFragment.activity?.theme))
    }
}

private fun CheckBox.setCheckedChangeAction(operateType: AddCheckBox, viewModel: ViewModel) {
    this@setCheckedChangeAction.setOnCheckedChangeListener { buttonView, isChecked ->
        val memoEditText = operateType.memoEditText
        Log.d("場所:setOnCheckedChangeListener", "targetMemoEditTextId=${memoEditText.id} targetCheckBoxId=${operateType.checkBoxId}")

        when (isChecked){
            true -> {
                Log.d("場所:setOnCheckedChangeListener", "isChecked=true")
                memoEditText.changeTextColorByCheckBoxState(true, operateType.buildType)
                viewModel.viewModelScope.launch {
                    operationActor.send(ChangeCheckBoxState(memoEditText, operateType.buildType))
                }
            }
            false -> {
                Log.d("場所:setOnCheckedChangeListener", "isChecked=false")
                memoEditText.changeTextColorByCheckBoxState(false, operateType.buildType)
                viewModel.viewModelScope.launch {
                    operationActor.send(ChangeCheckBoxState(memoEditText, operateType.buildType))
                }
            }
        }
    }
}

private fun createNewCheckBoxView(operateType: AddCheckBox): CheckBox {
    val context = when (operateType.buildType) {
        DisplayExistMemo -> displayFragment.context
        else -> editFragment.context
    }

    return CheckBox(context).apply {
        layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        ViewGroup.MarginLayoutParams(0, 0)
        textSize = 0f
        setPadding(4)
        id = when (operateType.buildType) {
            is CreateNewMemo -> View.generateViewId()
            is EditExistMemo , is DisplayExistMemo ->
                operateType.checkBoxId ?: throw(NullPointerException("CheckBoxId mast not be null"))
        }

        //既存メモの編集の場合、setCheckedChangeActionの前にCheckBoxViewを変更しておく
        if (operateType.checkBoxState) {
            isChecked = true
            operateType.memoEditText.changeTextColorByCheckBoxState(true, operateType.buildType)
        }

        when (operateType.buildType) {
            is DisplayExistMemo -> setCheckedChangeAction(operateType, displayViewModel)
            else -> setCheckedChangeAction(operateType, mainViewModel)
        }
    }
}

private fun addCheckBox(
    operateType: AddCheckBox,
    memoContents: MemoContents
): MemoContents {
    Log.d("場所:addCheckBox", "checkBox追加処理に入った")

    val newCheckBox = createNewCheckBoxView(operateType)
    val memoEditText = operateType.memoEditText

    return when (operateType.buildType) {
        is CreateNewMemo -> {
            memoContainer.setConstraintForBulletsView(memoEditText, newCheckBox, 80)

            memoContents.flatMap { memoRowInfo ->
                if (memoRowInfo.memoEditTextId.value == memoEditText.id)
                    listOf(memoRowInfo.copy(
                        checkBoxId = CheckBoxId(newCheckBox.id), checkBoxState = CheckBoxState(false)
                    ))
                else listOf(memoRowInfo)
            }
        }
        else -> {
            memoContainer.setConstraintForBulletsView(memoEditText, newCheckBox, 80)

            memoContents
        }
    }
}

private fun deleteCheckBox(
    operateType: DeleteCheckBox,
    memoContents: MemoContents
): MemoContents {
    Log.d("場所:deleteCheckBox", "checkBox削除処理に入った")
    val memoEditText = operateType.memoEditText
    val targetMemoRowInfo = memoContents.first { it.memoEditTextId.value == memoEditText.id }

    memoContainer.apply {
        setConstraintForDeleteBulletsView(memoEditText)
        removeBulletsViewFromLayout(editFragment, memoEditText, targetMemoRowInfo.checkBoxId)
    }

    return memoContents.flatMap { memoRowInfo ->
        if (memoRowInfo.memoEditTextId.value == operateType.memoEditText.id)
            listOf(targetMemoRowInfo.copy(
                checkBoxId = CheckBoxId(null), checkBoxState = CheckBoxState(false)
            ))
        else listOf(memoRowInfo)
    }
}

private fun createDotTextView(operateType: AddDot): TextView {
    val context = when (operateType.buildType) {
        DisplayExistMemo -> displayFragment.context
        else -> editFragment.context
    }

    return TextView(context).apply {
        layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        setPadding(4)
        setBackgroundResource(R.color.colorTransparent)
        text = "・"
        id = when (operateType.buildType) {
            is CreateNewMemo -> {
                View.generateViewId()
            }
            is EditExistMemo, is DisplayExistMemo -> {
                operateType.dotId ?: throw(NullPointerException("DotId mast not be null"))
            }
        }
    }
}

private fun addDot(operateType: AddDot, memoContents: MemoContents): MemoContents {
    Log.d("場所:addDot", "dot追加処理に入った")

    val newDot = createDotTextView(operateType)
    val memoEditText = operateType.memoEditText

    return when (operateType.buildType) {
        is CreateNewMemo -> {
            memoContainer.setConstraintForBulletsView(memoEditText, newDot, 80, 40)

            memoContents.flatMap { memoRowInfo ->
                if (memoRowInfo.memoEditTextId.value == operateType.memoEditText.id)
                    listOf(memoRowInfo.copy(dotId = DotId(newDot.id)))
                else listOf(memoRowInfo)
            }
        }
        else -> {
            memoContainer.setConstraintForBulletsView(operateType.memoEditText, newDot, 80, 40)

            memoContents
        }
    }
}

private fun deleteDot(operateType: DeleteDot, memoContents: MemoContents): MemoContents {
    Log.d("場所:deleteDot", "dotの削除処理に入った")
    val memoEditText = operateType.memoEditText
    val targetMemoRowInfo = memoContents.first { it.memoEditTextId.value == memoEditText.id }

    memoContainer.apply {
        setConstraintForDeleteBulletsView(memoEditText)
        removeBulletsViewFromLayout(editFragment, memoEditText, targetMemoRowInfo.dotId)
    }

    return memoContents.flatMap { memoRowInfo ->
        if (memoRowInfo.memoEditTextId.value == operateType.memoEditText.id)
            listOf(targetMemoRowInfo.copy(dotId = DotId(null)))
        else listOf(memoRowInfo)
    }
}
