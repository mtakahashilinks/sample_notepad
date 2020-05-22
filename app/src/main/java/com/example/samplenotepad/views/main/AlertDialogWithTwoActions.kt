package com.example.samplenotepad.views.main

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment


class AlertDialogWithTwoActions(
    private val title: Int,
    private val message: Int,
    private val positiveButton: Int,
    private val negativeButton: Int,
    private val positiveAction: (DialogInterface, Int) -> Unit,
    private val negativeAction: (DialogInterface, Int) -> Unit) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity.let {
            AlertDialog.Builder(requireContext()).apply {
                setTitle(title)
                setMessage(message)
                setPositiveButton(positiveButton) { dialog, id -> positiveAction(dialog, id) }
                setNegativeButton(negativeButton) { dialog, id -> negativeAction(dialog, id) }
            }.create()
        }
    }
}