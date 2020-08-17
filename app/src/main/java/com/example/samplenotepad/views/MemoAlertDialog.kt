package com.example.samplenotepad.views

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment


class MemoAlertDialog(
    private val message: Int,
    private val positiveButton: Int,
    private val negativeButton: Int,
    private val positiveAction: (DialogInterface, Int) -> Unit,
    private val negativeAction: (DialogInterface, Int) -> Unit) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity.let {
            AlertDialog.Builder(requireContext()).apply {
                setMessage(message)
                setPositiveButton(positiveButton) { dialog, id -> positiveAction(dialog, id) }
                setNegativeButton(negativeButton) { dialog, id -> negativeAction(dialog, id) }
            }.create()
        }
    }
}